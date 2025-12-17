package com.example.musicapplication.domain.model;

import java.io.Serializable;
import java.util.List;

public class Song implements Serializable {
    public String id;              // Firebase document ID
    public String title;
    public String artist;
    public String album;
    public String imageUrl;        // URL từ Firebase Storage
    public String audioUrl;        // URL từ Firebase Storage
    public long duration;         // milliseconds
    public long uploadDate;
    public String uploaderId;      // User ID của người upload
    public String uploaderName;
    public int playCount;
    public int likeCount;
    public List<String> tags;

    // For local playback (legacy support)
    public String uri;             // Local URI (for backward compatibility)
    public long albumId;          // Local album ID (for backward compatibility)

    // Default constructor for Firebase
    public Song() {
    }

    // Constructor for online songs
    public Song(String id, String title, String artist, String album,
                String imageUrl, String audioUrl, long duration) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.imageUrl = imageUrl;
        this.audioUrl = audioUrl;
        this.duration = duration;
        this.playCount = 0;
        this.likeCount = 0;
        this.uploadDate = System.currentTimeMillis();
    }

    // Legacy constructor for local songs
    public Song(long id, String title, String artist, String uri) {
        this.id = String.valueOf(id);
        this.title = title;
        this.artist = artist;
        this.uri = uri;
        this.albumId = -1;
    }

    // Legacy constructor with albumId
    public Song(long id, String title, String artist, String uri, long albumId) {
        this.id = String.valueOf(id);
        this.title = title;
        this.artist = artist;
        this.uri = uri;
        this.albumId = albumId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist != null ? artist : "Unknown Artist";
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album != null ? album : "Unknown Album";
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public long getAlbumId() {
        return albumId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    // --- BỔ SUNG CÁC GETTER/SETTER CÒN THIẾU ---

    public int getPlayCount() {
        return playCount;
    }

    public void setPlayCount(int playCount) {
        this.playCount = playCount;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    // ------------------------------------------

    public String getPlaybackUrl() {
        // Return audioUrl if available (online), otherwise return uri (local)
        return audioUrl != null && !audioUrl.isEmpty() ? audioUrl : uri;
    }

    public boolean isOnline() {
        return audioUrl != null && !audioUrl.isEmpty();
    }

    public boolean isLocal() {
        return uri != null && !uri.isEmpty() && (audioUrl == null || audioUrl.isEmpty());
    }

    @Override
    public String toString() {
        return title + (artist != null && !artist.isEmpty() ? " - " + artist : "");
    }
}