package com.example.musicapplication.domain.model;

import java.util.List;

public class Playlist {
    public String id;
    public String name;
    public String description;
    public String coverImageUrl;
    public String ownerId;
    public String ownerName;
    public List<String> songIds;
    public boolean isPublic;
    public long createdAt;
    public long updatedAt;
    
    // Default constructor for Firebase
    public Playlist() {
        this.songIds = new java.util.ArrayList<>();
        this.isPublic = true;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Playlist(String id, String name, String ownerId) {
        this();
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getCoverImageUrl() {
        return coverImageUrl;
    }
    
    public String getOwnerId() {
        return ownerId;
    }
    
    public List<String> getSongIds() {
        return songIds != null ? songIds : new java.util.ArrayList<>();
    }
    
    public int getSongCount() {
        return songIds != null ? songIds.size() : 0;
    }
    
    public boolean isPublic() {
        return isPublic;
    }
}

