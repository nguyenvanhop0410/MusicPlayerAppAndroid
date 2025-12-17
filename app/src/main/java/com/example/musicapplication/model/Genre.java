package com.example.musicapplication.model;

public class Genre {
    private String id;
    private String name;
    private int imageResId; // Hoặc dùng String imageUrl nếu load từ mạng
    private int backgroundColor; // Màu nền của thẻ

    public Genre(String id, String name, int imageResId, int backgroundColor) {
        this.id = id;
        this.name = name;
        this.imageResId = imageResId;
        this.backgroundColor = backgroundColor;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getImageResId() {
        return imageResId;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }
}