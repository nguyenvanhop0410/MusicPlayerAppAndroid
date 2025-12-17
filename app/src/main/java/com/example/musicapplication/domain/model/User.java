package com.example.musicapplication.domain.model;

import java.util.List;

public class User {
    public String id;
    public String email;
    public String displayName;
    public String photoUrl;
    public String bio;
    public List<String> favoriteSongs;
    public List<String> playlists;
    public int followers;
    public int following;
    public boolean isPremium;
    public long createdAt;
    
    // Default constructor for Firebase
    public User() {
        this.favoriteSongs = new java.util.ArrayList<>();
        this.playlists = new java.util.ArrayList<>();
        this.followers = 0;
        this.following = 0;
        this.isPremium = false;
        this.createdAt = System.currentTimeMillis();
    }
    
    public User(String id, String email, String displayName) {
        this();
        this.id = id;
        this.email = email;
        this.displayName = displayName;
    }
    
    public String getId() {
        return id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getDisplayName() {
        return displayName != null ? displayName : "User";
    }
    
    public String getPhotoUrl() {
        return photoUrl;
    }
    
    public List<String> getFavoriteSongs() {
        return favoriteSongs != null ? favoriteSongs : new java.util.ArrayList<>();
    }
    
    public List<String> getPlaylists() {
        return playlists != null ? playlists : new java.util.ArrayList<>();
    }
}

