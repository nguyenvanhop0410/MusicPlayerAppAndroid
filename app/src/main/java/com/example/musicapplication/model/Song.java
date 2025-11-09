package com.example.musicapplication.model;

public class Song {
    public long id;
    public String title;
    public String artist;
    public String uri; // string form of content Uri
    public long albumId; // Add albumId to fetch album art

    public Song(long id, String title, String artist, String uri) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.uri = uri;
        this.albumId = -1; // default
    }

    public Song(long id, String title, String artist, String uri, long albumId) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.uri = uri;
        this.albumId = albumId;
    }

    @Override
    public String toString() {
        return title + (artist != null && !artist.isEmpty() ? " - " + artist : "");
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public long getAlbumId() {
        return albumId;
    }
}
