package com.mycompany.app;

import java.util.List;
import java.util.Objects;

record CoverData(String artist, String title, List<String> tracks) {
    CoverData {
        Objects.requireNonNull(artist, "artist");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(tracks, "tracks");
    }
}
