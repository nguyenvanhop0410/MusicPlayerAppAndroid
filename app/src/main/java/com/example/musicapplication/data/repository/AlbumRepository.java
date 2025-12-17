package com.example.musicapplication.data.repository;

import com.example.musicapplication.model.Album;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class AlbumRepository {
    private FirebaseFirestore db;

    public interface OnAlbumLoadedListener {
        void onSuccess(List<Album> albums);
        void onError(Exception e);
    }

    public AlbumRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // Lấy tất cả Album để hiển thị lên Home (Grid View)
    public void getAllAlbums(OnAlbumLoadedListener listener) {
        db.collection("albums")
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Album> albumList = new ArrayList<>();
                    for (var doc : snapshots.getDocuments()) {
                        // Map dữ liệu từ Firestore sang Model Album
                        String title = doc.getString("title");
                        String artist = doc.getString("artist");
                        String coverUrl = doc.getString("coverUrl");
                        String id = doc.getId();

                        // Sử dụng Constructor Online của bạn
                        // Album(String id, String title, String artist, String coverUrl)
                        Album album = new Album(id, title, artist, coverUrl);

                        albumList.add(album);
                    }
                    listener.onSuccess(albumList);
                })
                .addOnFailureListener(listener::onError);
    }
}