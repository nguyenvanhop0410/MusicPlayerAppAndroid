package com.example.musicapplication.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.musicapplication.domain.model.Song;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryRepository {
    private static final String TAG = "HistoryRepository";
    private static final String USERS_COLLECTION = "users";
    private static final String SONGS_COLLECTION = "songs";
    private static final String HISTORY_SUB_COLLECTION = "history";

    private FirebaseFirestore firestore;
    private Context context;

    public HistoryRepository(Context context) {
        this.context = context.getApplicationContext();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public interface OnResultListener<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }

    // 1. Thêm vào lịch sử (Tối ưu: Ghi đè nếu đã tồn tại)
    public void addToHistory(String songId, OnResultListener<Void> listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || songId == null) return;

        Map<String, Object> historyData = new HashMap<>();
        historyData.put("songId", songId);
        historyData.put("timestamp", System.currentTimeMillis());
        firestore.collection(USERS_COLLECTION).document(user.getUid())
                .collection(HISTORY_SUB_COLLECTION).document(songId)
                .set(historyData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Updated history for song: " + songId);
                    if (listener != null) listener.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding history", e);
                    if (listener != null) listener.onError(e);
                });
    }

    // 2. Lấy lịch sử Real-time
    public ListenerRegistration getRealtimeHistory(OnResultListener<List<Song>> listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return null;

        // Lấy về toàn bộ lịch sử (hoặc giới hạn 10 bài gần nhất)
        return firestore.collection(USERS_COLLECTION).document(user.getUid())
                .collection(HISTORY_SUB_COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING) // Sắp xếp giảm dần theo thời gian
                .limit(10)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        listener.onError(e);
                        return;
                    }

                    if (snapshots != null) {
                        List<String> songIds = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            String sid = doc.getString("songId");
                            if (sid != null) songIds.add(sid);
                        }
                        // Lấy chi tiết bài hát
                        fetchSongsDetails(songIds, listener);
                    }
                });
    }

    // Helper: Lấy chi tiết Song từ List ID và sắp xếp đúng thứ tự
    private void fetchSongsDetails(List<String> songIds, OnResultListener<List<Song>> listener) {
        if (songIds == null || songIds.isEmpty()) {
            listener.onSuccess(new ArrayList<>());
            return;
        }

        // Firestore giới hạn whereIn tối đa 3 phần tử
        // Chia nhỏ list nếu cần thiết (ở đây demo lấy 10 bài đầu tiên của list 50 bài)
        List<String> chunk = songIds.subList(0, Math.min(songIds.size(), 3));

        firestore.collection(SONGS_COLLECTION)
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                .get()
                .addOnSuccessListener(snapshots -> {
                    Map<String, Song> songMap = new HashMap<>();
                    for (DocumentSnapshot doc : snapshots) {
                        Song s = documentToSong(doc.getId(), doc.getData());
                        if (s != null) songMap.put(s.getId(), s);
                    }

                    // Sắp xếp lại theo đúng thứ tự thời gian của songIds
                    List<Song> orderedSongs = new ArrayList<>();
                    for (String id : chunk) {
                        if (songMap.containsKey(id)) {
                            orderedSongs.add(songMap.get(id));
                        }
                    }

                    listener.onSuccess(orderedSongs);
                })
                .addOnFailureListener(listener::onError);
    }

    // Xóa 1 bài khỏi lịch sử
    public void removeFromHistory(String songId, OnResultListener<Void> listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        firestore.collection(USERS_COLLECTION).document(user.getUid())
                .collection(HISTORY_SUB_COLLECTION).document(songId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e);
                });
    }

    // Xóa toàn bộ lịch sử
    public void clearHistory(OnResultListener<Void> listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        firestore.collection(USERS_COLLECTION).document(user.getUid())
                .collection(HISTORY_SUB_COLLECTION)
                .get()
                .addOnSuccessListener(snapshots -> {
                    WriteBatch batch = firestore.batch();
                    for (DocumentSnapshot doc : snapshots) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                if (listener != null) listener.onSuccess(null);
                            })
                            .addOnFailureListener(e -> {
                                if (listener != null) listener.onError(e);
                            });
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e);
                });
    }

    private Song documentToSong(String docId, Map<String, Object> data) {
        try {
            Song s = new Song();
            s.id = docId;
            s.title = (String) data.get("title");
            s.artist = (String) data.get("artist");
            s.album = (String) data.get("album");
            s.audioUrl = (String) data.get("audioUrl");
            s.imageUrl = (String) data.get("imageUrl");
            Object d = data.get("duration");
            s.duration = d != null ? ((Number) d).longValue() : 0;
            Object pc = data.get("playCount");
            s.playCount = pc != null ? ((Number) pc).intValue() : 0;
            return s;
        } catch (Exception e) { return null; }
    }
}