package com.example.musicapplication.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.musicapplication.model.Song;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Repository chuyên xử lý tìm kiếm bài hát
 */
public class SearchRepository {
    private static final String TAG = "SearchRepository";
    private static final String SONGS_COLLECTION = "songs";

    private FirebaseFirestore firestore;
    private Context context;

    public interface OnResultListener<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }

    public SearchRepository(Context context) {
        this.context = context.getApplicationContext();
        this.firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Tìm kiếm bài hát theo từ khóa
     */
    public void searchSongs(String query, OnResultListener<List<Song>> listener) {
        if (query == null || query.isEmpty()) {
            listener.onSuccess(new ArrayList<>());
            return;
        }
        String normalizedQuery = removeAccent(query.trim());
        String[] tokens = normalizedQuery.split("\\s+");
        List<String> searchTerms = Arrays.asList(tokens);

        if (searchTerms.isEmpty()) {
            listener.onSuccess(new ArrayList<>());
            return;
        }

        firestore.collection(SONGS_COLLECTION)
                .whereArrayContainsAny("searchKeywords", searchTerms.subList(0, Math.min(searchTerms.size(), 10)))
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Song> filteredSongs = new ArrayList<>();
                    for (var doc : queryDocumentSnapshots.getDocuments()) {
                        Song song = documentToSong(doc.getId(), doc.getData());
                        if (song != null) {
                            String fullSongText = removeAccent(song.getTitle() + " " + song.getArtist());
                            boolean isMatch = true;
                            for (String token : tokens) {
                                if (!fullSongText.contains(token)) {
                                    isMatch = false;
                                    break;
                                }
                            }
                            if (isMatch) filteredSongs.add(song);
                        }
                    }
                    listener.onSuccess(filteredSongs);
                })
                .addOnFailureListener(listener::onError);
    }

    /**
     * Tìm kiếm bài hát theo tag/genre
     */
    public void getSongsByTag(String tag, OnResultListener<List<Song>> listener) {
        firestore.collection(SONGS_COLLECTION)
                .whereArrayContains("tags", tag)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Song> songs = new ArrayList<>();
                    for (var doc : snapshots) {
                        Song song = documentToSong(doc.getId(), doc.getData());
                        if (song != null) songs.add(song);
                    }
                    listener.onSuccess(songs);
                })
                .addOnFailureListener(listener::onError);
    }

    /**
     * Loại bỏ dấu tiếng Việt để tìm kiếm dễ dàng hơn
     */
    public static String removeAccent(String s) {
        if (s == null) return "";
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").toLowerCase().replace("đ", "d");
    }

    /**
     * Tạo keywords cho việc tìm kiếm
     */
    public static List<String> generateKeywords(String title, String artist) {
        Set<String> keywords = new HashSet<>();
        
        // Handle null values
        String safeTitle = title != null ? title : "";
        String safeArtist = artist != null ? artist : "";
        
        String fullText = (safeTitle + " " + safeArtist).trim();
        
        if (fullText.isEmpty()) {
            return new ArrayList<>();
        }
        
        String normalizedText = removeAccent(fullText);
        String[] words = normalizedText.split("\\s+");
        for (String w : words) {
            if (w != null && !w.isEmpty()) {
                keywords.add(w);
            }
        }
        return new ArrayList<>(keywords);
    }

    /**
     * Helper method: Convert Firestore document to Song object
     */
    private Song documentToSong(String docId, java.util.Map<String, Object> data) {
        try {
            Song song = new Song();
            song.id = docId;
            song.title = (String) data.get("title");
            song.artist = (String) data.get("artist");
            song.album = (String) data.get("album");
            song.audioUrl = (String) data.get("audioUrl");
            song.imageUrl = (String) data.get("imageUrl");
            Object d = data.get("duration");
            song.duration = d != null ? ((Number) d).longValue() : 0;
            Object pc = data.get("playCount");
            song.playCount = pc != null ? ((Number) pc).intValue() : 0;
            Object tags = data.get("tags");
            song.tags = tags != null ? (List<String>) tags : new ArrayList<>();
            return song;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing song", e);
            return null;
        }
    }
}

