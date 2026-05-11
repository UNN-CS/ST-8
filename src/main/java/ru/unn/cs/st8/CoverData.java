package ru.unn.cs.st8;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Album metadata read from {@code data/data.txt}: artist, title, up to {@link #MAX_TRACKS_FIELDS} tracks. */
public record CoverData(String artist, String title, List<String> tracks) {

  /** Basic form at papercdcase.com exposes only {@code track1}…{@code track16}. */
  public static final int MAX_TRACKS_FIELDS = 16;

  public CoverData {
    tracks = Collections.unmodifiableList(new ArrayList<>(tracks));
    if (artist.isBlank()) {
      throw new IllegalArgumentException("Artist must not be blank");
    }
    if (title.isBlank()) {
      throw new IllegalArgumentException("Title must not be blank");
    }
    if (tracks.isEmpty()) {
      throw new IllegalArgumentException("At least one track is required");
    }
    if (tracks.size() > MAX_TRACKS_FIELDS) {
      throw new IllegalArgumentException(
          "Too many tracks: "
              + tracks.size()
              + " (basic form accepts at most "
              + MAX_TRACKS_FIELDS
              + ")");
    }
  }
}
