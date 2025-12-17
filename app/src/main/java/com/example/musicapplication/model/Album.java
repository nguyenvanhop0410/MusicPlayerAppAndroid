package com.example.musicapplication.model;

import java.io.Serializable;

public class Album implements Serializable {
    private String id;
    private String title;       // Tên Album
    private String artist;      // Tên Ca sĩ
    private String coverUrl;    // Link ảnh online (Firebase)
    private int artResId;       // ID ảnh nội bộ (R.drawable.xxx) - dùng cho demo offline

    // Logic lọc bài hát
    private String filterType;  // "album" hoặc "artist"
    private String filterValue; // Giá trị để lọc

    public Album() { }

    // Constructor cho Online (Firebase)
    public Album(String id, String title, String artist, String coverUrl) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.coverUrl = coverUrl;
        this.artResId = 0; // Không dùng
        this.filterType = "album";
        this.filterValue = title; // Mặc định lọc theo tên album
    }

    // Constructor cho Offline (Local Resource) - Code của bạn
    public Album(String title, String artist, int artResId, String filterType, String filterValue) {
        this.title = title;
        this.artist = artist;
        this.artResId = artResId;
        this.filterType = filterType;
        this.filterValue = filterValue;
        this.coverUrl = ""; // Không có link online
    }

    // --- GETTER & SETTER ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public int getArtResId() { return artResId; }
    public void setArtResId(int artResId) { this.artResId = artResId; }

    public String getFilterType() { return filterType; }
    public void setFilterType(String filterType) { this.filterType = filterType; }

    public String getFilterValue() { return filterValue; }
    public void setFilterValue(String filterValue) { this.filterValue = filterValue; }
}