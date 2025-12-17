package com.example.musicapplication.model;

import com.google.firebase.Timestamp;

public class History {
    private String id;
    private String userId;
    private String songId;
    private Timestamp playedAt;

    public History() { }

    public History(String userId, String songId) {
        this.userId = userId;
        this.songId = songId;
        this.playedAt = Timestamp.now();
    }

    // Getter & Setter đầy đủ
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getSongId() { return songId; }
    public void setSongId(String songId) { this.songId = songId; }

    public Timestamp getPlayedAt() { return playedAt; }
    public void setPlayedAt(Timestamp playedAt) { this.playedAt = playedAt; }
}
