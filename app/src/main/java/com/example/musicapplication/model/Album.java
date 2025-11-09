package com.example.musicapplication.model;

import androidx.annotation.NonNull;

public class Album {
    public long id;
    public String title;
    public String artist;
    // Changed: use int resource id instead of URI string for drawable resources
    public int artResId;
    public String filterType; // "albumId" or "artist"
    public String filterValue; // Album ID hoặc tên nghệ sĩ để lọc

    public Album(long id, String title, String artist) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.artResId = artResId;
        this.filterType = "albumId"; // Mặc định lọc theo albumId
        this.filterValue = String.valueOf(id);
    }

    // Constructor mới: cho phép lọc theo nghệ sĩ
    public Album(String title, String artistName, int artResId, String filterType, String filterValue) {
        this.id = 0; // không cần id khi lọc theo nghệ sĩ
        this.title = title;
        this.artist = artistName;
        this.artResId = artResId;
        this.filterType = filterType;
        this.filterValue = filterValue;
    }

    @NonNull
    @Override
    public String toString() {
        return title + (artist != null && !artist.isEmpty() ? " - " + artist : "");
    }
}
