package com.example.musicapplication.model;

public class Artist {
    private String id;
    private String name;
    private String imageUrl; // URL ảnh đại diện

    public Artist() {
        // Constructor rỗng cho Firestore (nếu cần sau này)
    }

    public Artist(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}