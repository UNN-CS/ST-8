package com.mycompany.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppTest {

    @Test
    void loadData_parsesTracks() throws Exception {
        java.nio.file.Path f = java.nio.file.Path.of("data/data.txt");
        App.CdCaseData d = App.CdCaseData.load(f.toAbsolutePath());
        assertEquals("The Beatles", d.artist);
        assertEquals("Abbey Road", d.title);
        assertTrue(d.tracks.size() >= 10);
        assertEquals("Come Together", d.tracks.get(0));
    }
}
