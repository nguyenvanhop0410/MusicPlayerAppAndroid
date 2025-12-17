package com.example.musicapplication.domain.model;

public class SliderItem {
    private String imageUrl;

    // Bạn có thể thêm id bài hát để khi bấm vào banner thì phát nhạc luôn
    private String songId;

    public SliderItem(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}