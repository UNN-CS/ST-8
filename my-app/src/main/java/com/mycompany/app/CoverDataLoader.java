package com.mycompany.app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

final class CoverDataLoader {

    private static final int MAX_TRACKS = 18;

    private CoverDataLoader() {
    }

    static CoverData fromFile(Path dataFile) throws IOException {
        if (!Files.exists(dataFile)) {
            throw new IllegalArgumentException("Data file not found: " + dataFile);
        }
        return parseDataLines(Files.readAllLines(dataFile, StandardCharsets.UTF_8));
    }

    static CoverData parseDataLines(List<String> rawLines) {
        List<String> lines = rawLines.stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());

        if (lines.size() < 3) {
            throw new IllegalArgumentException("data.txt must contain artist, title and at least one track.");
        }

        if (looksLikeKeyValueFormat(lines)) {
            return parseKeyValueLines(lines);
        }

        String artist = lines.get(0);
        String title = lines.get(1);
        List<String> tracks = new ArrayList<>(lines.subList(2, lines.size()));
        validateTracks(tracks);
        return new CoverData(artist, title, tracks);
    }

    private static boolean looksLikeKeyValueFormat(List<String> lines) {
        return lines.stream().anyMatch(line -> line.contains(":"));
    }

    private static CoverData parseKeyValueLines(List<String> lines) {
        String artist = null;
        String title = null;
        List<String> tracks = new ArrayList<>();

        for (String line : lines) {
            int separatorIndex = line.indexOf(':');
            if (separatorIndex < 0) {
                throw new IllegalArgumentException("Invalid data line: " + line);
            }

            String key = line.substring(0, separatorIndex).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(separatorIndex + 1).trim();

            switch (key) {
                case "artist":
                    artist = value;
                    break;
                case "title":
                    title = value;
                    break;
                case "track":
                    tracks.add(value);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported key in data.txt: " + key);
            }
        }

        if (artist == null || title == null || tracks.isEmpty()) {
            throw new IllegalArgumentException("Key-value data.txt must contain Artist, Title and at least one Track.");
        }

        validateTracks(tracks);
        return new CoverData(artist, title, tracks);
    }

    private static void validateTracks(List<String> tracks) {
        if (tracks.isEmpty()) {
            throw new IllegalArgumentException("At least one track is required.");
        }
        if (tracks.size() > MAX_TRACKS) {
            throw new IllegalArgumentException("At most " + MAX_TRACKS + " tracks are supported.");
        }
    }
}
