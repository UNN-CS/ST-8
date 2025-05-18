package com.example.app.Entity;

import java.util.ArrayList;
import java.util.List;

public class MusicAlbum {
    private String artistName;
    private String title;
    private List<String> nameSongs = new ArrayList<>();

    public MusicAlbum() {
    }

    public void addSong(String song) {
        nameSongs.add(song);
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getNameSongs() {
        return nameSongs;
    }

    public void setNameSongs(List<String> nameSongs) {
        this.nameSongs = nameSongs;
    }

    @Override
    public String toString() {
        return "MusicAlbum{" +
                "artistName='" + artistName + '\'' +
                ", title='" + title + '\'' +
                ", nameSongs=" + nameSongs +
                '}';
    }
}
