package ru.unn.cs.st8;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads {@link CoverData} from a UTF-8 text file:
 *
 * <ul>
 *   <li>First non-empty line: artist</li>
 *   <li>Second non-empty line: album title</li>
 *   <li>Remaining non-empty lines: track names (≤ {@link CoverData#MAX_TRACKS_FIELDS})</li>
 * </ul>
 */
public final class CoverDataLoader {

  private CoverDataLoader() {}

  public static CoverData load(Path file) throws IOException {
    List<String> lines =
        Files.readAllLines(file, StandardCharsets.UTF_8).stream()
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .toList();
    if (lines.size() < 3) {
      throw new IllegalArgumentException(
          "Expected at least artist, title, and one track in " + file + " (got " + lines.size() + " lines)");
    }
    String artist = lines.get(0);
    String title = lines.get(1);
    List<String> tracks = new ArrayList<>(lines.subList(2, lines.size()));
    return new CoverData(artist, title, tracks);
  }
}
