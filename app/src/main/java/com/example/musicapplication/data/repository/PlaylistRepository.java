package com.example.musicapplication.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.musicapplication.model.Playlist;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlaylistRepository {
    private static final String TAG = "PlaylistRepository";
    private static final String PLAYLISTS_COLLECTION = "playlists";

    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private Context context;

    public PlaylistRepository(Context context) {
        this.context = context.getApplicationContext();
        this.firestore = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
    }

    public interface OnResultListener<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }

    // 1. Tạo Playlist mới
    public void createPlaylist(String name, OnResultListener<String> listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            listener.onError(new Exception("User not logged in"));
            return;
        }

        String id = UUID.randomUUID().toString();
        String ownerName = user.getDisplayName() != null ? user.getDisplayName() : "User";
        Playlist playlist = new Playlist(id, name, user.getUid(), ownerName);

        firestore.collection(PLAYLISTS_COLLECTION).document(id)
                .set(playlistToMap(playlist))
                .addOnSuccessListener(aVoid -> listener.onSuccess(id))
                .addOnFailureListener(listener::onError);
    }

    // 2. Lấy Playlist Real-time (ĐÃ SỬA: Sắp xếp ở Client để tránh lỗi Index Firestore)
    public ListenerRegistration getRealtimeUserPlaylists(OnResultListener<List<Playlist>> listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return null;

        return firestore.collection(PLAYLISTS_COLLECTION)
                .whereEqualTo("ownerId", user.getUid())
                // .orderBy("createdAt", Query.Direction.DESCENDING) <--- ĐÃ BỎ DÒNG NÀY ĐỂ TRÁNH LỖI
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Lỗi lấy playlist: ", e); // Log lỗi để kiểm tra
                        listener.onError(e);
                        return;
                    }
                    if (snapshots != null) {
                        List<Playlist> playlists = new ArrayList<>();
                        for (var doc : snapshots) {
                            Playlist p = documentToPlaylist(doc.getId(), doc.getData());
                            if (p != null) playlists.add(p);
                        }

                        // Sắp xếp danh sách tại đây (Mới nhất lên đầu)
                        Collections.sort(playlists, (p1, p2) -> Long.compare(p2.getCreatedAt(), p1.getCreatedAt()));
                        Log.d(TAG, "Fetched playlists: " + playlists.size());
                        listener.onSuccess(playlists);
                    }
                });
    }

    // ... (Giữ nguyên các hàm khác: getPlaylistById, updatePlaylistInfo, v.v...)

    // 3. Lấy chi tiết 1 Playlist
    public void getPlaylistById(String playlistId, OnResultListener<Playlist> listener) {
        firestore.collection(PLAYLISTS_COLLECTION).document(playlistId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        listener.onSuccess(documentToPlaylist(doc.getId(), doc.getData()));
                    } else {
                        listener.onError(new Exception("Not found"));
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    // 4. Update thông tin Playlist
    public void updatePlaylistInfo(String playlistId, String newName, String newDesc, OnResultListener<Void> listener) {
        Map<String, Object> updates = new HashMap<>();
        if (newName != null) updates.put("name", newName);
        if (newDesc != null) updates.put("description", newDesc);
        updates.put("updatedAt", System.currentTimeMillis());

        firestore.collection(PLAYLISTS_COLLECTION).document(playlistId)
                .update(updates)
                .addOnSuccessListener(aVoid -> listener.onSuccess(null))
                .addOnFailureListener(listener::onError);
    }

    // 5. Update Ảnh bìa Playlist
    public void updatePlaylistImage(String playlistId, byte[] imageData, OnResultListener<String> listener) {
        StorageReference ref = storage.getReference().child("playlist_covers").child(playlistId + ".jpg");
        ref.putBytes(imageData).addOnSuccessListener(task ->
                ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    String url = uri.toString();
                    firestore.collection(PLAYLISTS_COLLECTION).document(playlistId)
                            .update("coverImageUrl", url, "updatedAt", System.currentTimeMillis())
                            .addOnSuccessListener(v -> listener.onSuccess(url))
                            .addOnFailureListener(listener::onError);
                })
        ).addOnFailureListener(listener::onError);
    }

    // 6. Xóa Playlist
    public void deletePlaylist(String playlistId, OnResultListener<Void> listener) {
        firestore.collection(PLAYLISTS_COLLECTION).document(playlistId).delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess(null))
                .addOnFailureListener(listener::onError);
    }

    // 7. Thêm nhạc vào Playlist
    public void addSongToPlaylist(String playlistId, String songId, OnResultListener<Void> listener) {
        firestore.collection(PLAYLISTS_COLLECTION).document(playlistId)
                .update("songIds", FieldValue.arrayUnion(songId), "updatedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> listener.onSuccess(null))
                .addOnFailureListener(listener::onError);
    }

    // 8. Xóa nhạc khỏi Playlist
    public void removeSongFromPlaylist(String playlistId, String songId, OnResultListener<Void> listener) {
        firestore.collection(PLAYLISTS_COLLECTION).document(playlistId)
                .update("songIds", FieldValue.arrayRemove(songId), "updatedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> listener.onSuccess(null))
                .addOnFailureListener(listener::onError);
    }

    // --- Helpers ---
    private Map<String, Object> playlistToMap(Playlist p) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", p.getName());
        map.put("description", p.getDescription());
        map.put("ownerId", p.getOwnerId());
        map.put("ownerName", p.getOwnerName());
        map.put("coverImageUrl", p.getCoverImageUrl());
        map.put("songIds", p.getSongIds());
        map.put("isPublic", p.isPublic());
        map.put("createdAt", p.getCreatedAt());
        map.put("updatedAt", p.getUpdatedAt());
        return map;
    }

    private Playlist documentToPlaylist(String id, Map<String, Object> data) {
        try {
            Playlist p = new Playlist();
            p.setId(id);
            p.setName((String) data.get("name"));
            p.setDescription((String) data.get("description"));
            p.setOwnerId((String) data.get("ownerId"));
            p.setOwnerName((String) data.get("ownerName"));
            p.setCoverImageUrl((String) data.get("coverImageUrl"));

            Object songs = data.get("songIds");
            p.setSongIds(songs != null ? (List<String>) songs : new ArrayList<>());

            Object pub = data.get("isPublic");
            p.setPublic(pub != null ? (Boolean) pub : true);

            Object cr = data.get("createdAt");
            p.setCreatedAt(cr != null ? ((Number) cr).longValue() : 0);
            Object up = data.get("updatedAt");
            p.setUpdatedAt(up != null ? ((Number) up).longValue() : 0);
            return p;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing playlist", e);
            return null;
        }
    }
}