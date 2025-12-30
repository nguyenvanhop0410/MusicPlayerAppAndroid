package com.example.musicapplication.data.repository;

import android.content.Context;
import com.example.musicapplication.model.Song;
import com.example.musicapplication.utils.Logger;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Repository chuyên xử lý upload bài hát và lưu trữ file
 */
public class SongUploadRepository {
    private static final String SONGS_COLLECTION = "songs";

    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private Context context;

    public interface OnResultListener<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }

    public SongUploadRepository(Context context) {
        this.context = context.getApplicationContext();
        this.firestore = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
    }

    /**
     * Upload bài hát lên Firebase Storage và lưu metadata vào Firestore
     */
    public void uploadSong(Song song, byte[] audioFile, byte[] imageFile, OnResultListener<String> listener) {
        String songId = song.id != null ? song.id : UUID.randomUUID().toString();
        song.id = songId;

        // Upload audio file
        StorageReference audioRef = storage.getReference().child("songs").child(songId + ".mp3");
        audioRef.putBytes(audioFile).addOnSuccessListener(task ->
                audioRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    song.audioUrl = uri.toString();

                    // Upload image file if exists
                    if (imageFile != null && imageFile.length > 0) {
                        StorageReference imgRef = storage.getReference().child("images").child(songId + ".jpg");
                        imgRef.putBytes(imageFile).addOnSuccessListener(t ->
                                imgRef.getDownloadUrl().addOnSuccessListener(iUri -> {
                                    song.imageUrl = iUri.toString();
                                    saveSongToDatabase(song, listener);
                                }).addOnFailureListener(listener::onError)
                        ).addOnFailureListener(listener::onError);
                    } else {
                        saveSongToDatabase(song, listener);
                    }
                }).addOnFailureListener(listener::onError)
        ).addOnFailureListener(listener::onError);
    }

    /**
     * Lưu thông tin bài hát vào Firestore
     */
    private void saveSongToDatabase(Song song, OnResultListener<String> listener) {
        Map<String, Object> songData = songToMap(song);
        firestore.collection(SONGS_COLLECTION).document(song.id).set(songData)
                .addOnSuccessListener(aVoid -> {
                    Logger.d("SongUploadRepository: Song uploaded successfully: " + song.id);
                    
                    // Auto-create or update artist document
                    createOrUpdateArtist(song.artist);
                    
                    listener.onSuccess(song.id);
                })
                .addOnFailureListener(e -> {
                    Logger.logRepositoryError("SongUploadRepository", "saveSongToDatabase", e);
                    listener.onError(e);
                });
    }

    /**
     * Tự động tạo hoặc cập nhật artist document khi upload bài hát
     */
    private void createOrUpdateArtist(String artistName) {
        if (artistName == null || artistName.trim().isEmpty() || artistName.equals("Unknown Artist")) {
            Logger.d("Skipping artist creation for null/empty/unknown artist");
            return;
        }
        
        try {
            // Sử dụng artistName chuẩn hóa làm document ID để tránh duplicate
            String artistId = artistName.trim().toLowerCase()
                    .replaceAll("\\s+", "-")
                    .replaceAll("[^a-z0-9-]", "")
                    .replaceAll("-+", "-")
                    .replaceAll("^-|-$", "");
            
            // Validate artistId
            if (artistId.isEmpty() || artistId.length() < 2) {
                Logger.w("Invalid artist ID generated from: " + artistName);
                return;
            }
            
            firestore.collection("artists").document(artistId).get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) {
                            // Tạo artist document mới
                            Map<String, Object> artistData = new HashMap<>();
                            artistData.put("name", artistName.trim());
                            artistData.put("followers", 0);
                            artistData.put("imageUrl", "");
                            artistData.put("bio", "");
                            
                            firestore.collection("artists").document(artistId).set(artistData)
                                    .addOnSuccessListener(aVoid -> Logger.d("Auto-created artist: " + artistName + " (ID: " + artistId + ")"))
                                    .addOnFailureListener(e -> Logger.e("Failed to create artist: " + artistName, e));
                        } else {
                            Logger.d("Artist already exists: " + artistName + " (ID: " + artistId + ")");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Logger.e("Error checking artist document: " + artistName, e);
                        // Don't fail the whole upload if artist check fails
                    });
        } catch (Exception e) {
            Logger.e("Exception in createOrUpdateArtist for: " + artistName, e);
            // Don't crash, just log the error
        }
    }

    /**
     * Convert Song object to Map for Firestore
     */
    private Map<String, Object> songToMap(Song song) {
        Map<String, Object> map = new HashMap<>();
        
        // Handle null values with defaults
        map.put("title", song.title != null ? song.title : "Unknown");
        map.put("artist", song.artist != null ? song.artist : "Unknown Artist");
        map.put("album", song.album != null && !song.album.isEmpty() ? song.album : "");
        map.put("audioUrl", song.audioUrl != null ? song.audioUrl : "");
        map.put("imageUrl", song.imageUrl != null ? song.imageUrl : "");
        map.put("duration", song.duration);
        map.put("uploadDate", song.uploadDate > 0 ? song.uploadDate : System.currentTimeMillis());
        map.put("playCount", song.playCount);
        map.put("likeCount", song.likeCount);
        map.put("tags", song.tags != null ? song.tags : new ArrayList<>());

        // Generate search keywords (now null-safe)
        try {
            List<String> keywords = SearchRepository.generateKeywords(song.title, song.artist);
            map.put("searchKeywords", keywords != null ? keywords : new ArrayList<>());
        } catch (Exception e) {
            Logger.e("Error generating keywords: " + e.getMessage());
            map.put("searchKeywords", new ArrayList<>());
        }

        return map;
    }

    /**
     * Xóa bài hát (cả file và metadata)
     */
    public void deleteSong(String songId, OnResultListener<Boolean> listener) {
        // Delete from Firestore
        firestore.collection(SONGS_COLLECTION).document(songId).delete()
                .addOnSuccessListener(aVoid -> {
                    // Delete audio file from Storage
                    StorageReference audioRef = storage.getReference().child("songs").child(songId + ".mp3");
                    audioRef.delete().addOnSuccessListener(v1 -> {
                        // Delete image file from Storage
                        StorageReference imgRef = storage.getReference().child("images").child(songId + ".jpg");
                        imgRef.delete()
                                .addOnSuccessListener(v2 -> listener.onSuccess(true))
                                .addOnFailureListener(e -> listener.onSuccess(true)); // Image might not exist
                    }).addOnFailureListener(e -> listener.onSuccess(true)); // Audio might not exist
                })
                .addOnFailureListener(listener::onError);
    }

    /**
     * Cập nhật thông tin bài hát
     */
    public void updateSong(Song song, OnResultListener<Boolean> listener) {
        Map<String, Object> songData = songToMap(song);
        firestore.collection(SONGS_COLLECTION).document(song.id).update(songData)
                .addOnSuccessListener(aVoid -> listener.onSuccess(true))
                .addOnFailureListener(listener::onError);
    }
}


