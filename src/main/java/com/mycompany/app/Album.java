package com.mycompany.app;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Scanner;

public class Album {
    ArrayList<String> artists = new ArrayList<>();
    String title = "";
    ArrayList<String> tracks = new ArrayList<>();

    public void ReadAlbum(Path path) {
        try (Scanner in = new Scanner(path)) {
            String line;
            while (in.hasNextLine()) {
                if (in.nextLine().equals("Artists:"))
                    break;
            }
            while (in.hasNextLine()) {
                line = in.nextLine();
                if (line.equals("Title:"))
                    break;
                this.artists.add(line);
            }
            if (in.hasNextLine()) {
                this.title = in.nextLine();
            }
            if (in.hasNextLine()) { in.nextLine(); }
            while (in.hasNextLine()) {
                this.tracks.add(in.nextLine());
            }
        } catch (IOException ex) {
            System.out.println(ex.toString());
        }
    }

    public Album(Path path) {
        this.ReadAlbum(path);
    }

    public ArrayList<String> getArtists() {
        return artists;
    }

    public String getArtistsString() {
        StringBuilder output = new StringBuilder();
        for (String artist: artists) {
            if (output.isEmpty())
                output.append(artist);
            else output.append("; ").append(artist);
        }
        return output.toString();
    }

    public String getTitle() {
        return title;
    }

    public ArrayList<String> getTracks() {
        return tracks;
    }
}
