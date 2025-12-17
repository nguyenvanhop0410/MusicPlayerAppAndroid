package com.example.musicapplication.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.musicapplication.domain.model.Song;
import com.example.musicapplication.model.Album;
import com.example.musicapplication.model.Playlist;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class SongRepository {
    private static final String TAG = "SongRepository";
    private static final String SONGS_COLLECTION = "songs";
    private static final String ALBUMS_COLLECTION = "albums";
    private static final String USERS_COLLECTION = "users";

    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private Context context;

    public SongRepository(Context context) {
        this.context = context.getApplicationContext();
        this.firestore = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
    }

    public interface OnResultListener<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }

    // --- UTILS ---

    public static String removeAccent(String s) {
        if (s == null) return "";
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").toLowerCase().replace("đ", "d");
    }

    private List<String> generateKeywords(String title, String artist) {
        Set<String> keywords = new HashSet<>();
        String fullText = (title + " " + artist).trim();
        String normalizedText = removeAccent(fullText);
        String[] words = normalizedText.split(" ");
        for (String w : words) if (!w.isEmpty()) keywords.add(w);
        return new ArrayList<>(keywords);
    }

    // --- SEARCH ---

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

    // --- LIKED SONGS & SONG DATA ---
    public void checkIsLiked(String songId, OnResultListener<Boolean> listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            listener.onSuccess(false);
            return;
        }

        firestore.collection(USERS_COLLECTION).document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> favoriteSongs = (List<String>) documentSnapshot.get("favoriteSongs");
                        if (favoriteSongs != null && favoriteSongs.contains(songId)) {
                            listener.onSuccess(true);
                        } else {
                            listener.onSuccess(false);
                        }
                    } else {
                        listener.onSuccess(false);
                    }
                })
                .addOnFailureListener(listener::onError);
    }
    public ListenerRegistration listenToLikedSongs(OnResultListener<List<Song>> listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return null;

        return firestore.collection(USERS_COLLECTION).document(user.getUid())
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        listener.onError(e);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        List<String> favoriteIds = (List<String>) documentSnapshot.get("favoriteSongs");
                        if (favoriteIds == null || favoriteIds.isEmpty()) {
                            listener.onSuccess(new ArrayList<>());
                            return;
                        }
                        getSongsByIds(favoriteIds, listener);
                    } else {
                        listener.onSuccess(new ArrayList<>());
                    }
                });
    }

    public void getSongsByIds(List<String> songIds, OnResultListener<List<Song>> listener) {
        if (songIds == null || songIds.isEmpty()) {
            listener.onSuccess(new ArrayList<>());
            return;
        }
        // Limit query to 10 items for demo/simplicity
        List<String> chunk = songIds.subList(0, Math.min(songIds.size(), 10));

        firestore.collection(SONGS_COLLECTION)
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Song> songs = new ArrayList<>();
                    for (var doc : snapshots) {
                        Song s = documentToSong(doc.getId(), doc.getData());
                        if (s != null) songs.add(s);
                    }
                    listener.onSuccess(songs);
                })
                .addOnFailureListener(listener::onError);
    }

    // --- GENERAL SONG OPERATIONS ---

    public void getAlbums(OnResultListener<List<Album>> listener) {
        firestore.collection(ALBUMS_COLLECTION).get()
                .addOnSuccessListener(snapshots -> {
                    List<Album> albums = new ArrayList<>();
                    for (var doc : snapshots) {
                        try {
                            String title = doc.getString("title");
                            if (title == null) title = doc.getString("name");
                            String coverUrl = doc.getString("coverUrl");
                            if (coverUrl == null) coverUrl = doc.getString("imageUrl");
                            if (title != null) albums.add(new Album(doc.getId(), title, doc.getString("artist"), coverUrl));
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing album", e);
                        }
                    }
                    listener.onSuccess(albums);
                })
                .addOnFailureListener(listener::onError);
    }

    public void getTrendingSongs(int limit, OnResultListener<List<Song>> listener) {
        firestore.collection(SONGS_COLLECTION)
                .orderBy("playCount", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(s -> parseSongs(s, listener))
                .addOnFailureListener(listener::onError);
    }

    public void getRecentlyAddedSongs(int limit, OnResultListener<List<Song>> listener) {
        firestore.collection(SONGS_COLLECTION)
                .orderBy("uploadDate", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(s -> parseSongs(s, listener))
                .addOnFailureListener(listener::onError);
    }

    public void getAllSongs(OnResultListener<List<Song>> listener) {
        firestore.collection(SONGS_COLLECTION)
                .orderBy("uploadDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(s -> parseSongs(s, listener))
                .addOnFailureListener(listener::onError);
    }

    public void getSongsByAlbum(String albumName, OnResultListener<List<Song>> listener) {
        firestore.collection(SONGS_COLLECTION)
                .whereEqualTo("album", albumName)
                .get()
                .addOnSuccessListener(s -> parseSongs(s, listener))
                .addOnFailureListener(listener::onError);
    }

    public void getSongsByTag(String tag, OnResultListener<List<Song>> listener) {
        firestore.collection(SONGS_COLLECTION)
                .whereArrayContains("tags", tag)
                .get()
                .addOnSuccessListener(s -> parseSongs(s, listener))
                .addOnFailureListener(listener::onError);
    }

    public void getSongById(String songId, OnResultListener<Song> listener) {
        firestore.collection(SONGS_COLLECTION).document(songId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) listener.onSuccess(documentToSong(doc.getId(), doc.getData()));
                    else listener.onError(new Exception("Not found"));
                })
                .addOnFailureListener(listener::onError);
    }

    public void uploadSong(Song song, byte[] audioFile, byte[] imageFile, OnResultListener<String> listener) {
        String songId = song.id != null ? song.id : UUID.randomUUID().toString();
        song.id = songId;
        StorageReference audioRef = storage.getReference().child("songs").child(songId + ".mp3");
        audioRef.putBytes(audioFile).addOnSuccessListener(task ->
                audioRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    song.audioUrl = uri.toString();
                    if (imageFile != null && imageFile.length > 0) {
                        StorageReference imgRef = storage.getReference().child("images").child(songId + ".jpg");
                        imgRef.putBytes(imageFile).addOnSuccessListener(t ->
                                imgRef.getDownloadUrl().addOnSuccessListener(iUri -> {
                                    song.imageUrl = iUri.toString();
                                    saveSongToDatabase(song, listener);
                                })
                        );
                    } else saveSongToDatabase(song, listener);
                })
        ).addOnFailureListener(listener::onError);
    }

    private void saveSongToDatabase(Song song, OnResultListener<String> listener) {
        Map<String, Object> songData = songToMap(song);
        firestore.collection(SONGS_COLLECTION).document(song.id).set(songData)
                .addOnSuccessListener(aVoid -> listener.onSuccess(song.id))
                .addOnFailureListener(listener::onError);
    }

    public void incrementPlayCount(String songId) {
        firestore.collection(SONGS_COLLECTION).document(songId)
                .update("playCount", FieldValue.increment(1));
    }

    public void updateFavorite(String songId, boolean isLiked, OnResultListener<Boolean> listener) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return;
        FieldValue op = isLiked ? FieldValue.arrayUnion(songId) : FieldValue.arrayRemove(songId);
        firestore.collection(USERS_COLLECTION).document(u.getUid()).update("favoriteSongs", op)
                .addOnSuccessListener(v -> {
                    firestore.collection(SONGS_COLLECTION).document(songId).update("likeCount", FieldValue.increment(isLiked ? 1 : -1));
                    if (listener != null) listener.onSuccess(isLiked);
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e);
                });
    }

    // --- HELPERS ---

    private void parseSongs(com.google.firebase.firestore.QuerySnapshot s, OnResultListener<List<Song>> l) {
        List<Song> list = new ArrayList<>();
        for (var d : s) {
            Song obj = documentToSong(d.getId(), d.getData());
            if (obj != null) list.add(obj);
        }
        l.onSuccess(list);
    }

    private Song documentToSong(String docId, Map<String, Object> data) {
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
            return null;
        }
    }

    private Map<String, Object> songToMap(Song song) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", song.title);
        map.put("artist", song.artist);
        map.put("album", song.album != null ? song.album : "");
        map.put("audioUrl", song.audioUrl);
        map.put("imageUrl", song.imageUrl != null ? song.imageUrl : "");
        map.put("duration", song.duration);
        map.put("uploadDate", song.uploadDate > 0 ? song.uploadDate : System.currentTimeMillis());
        map.put("playCount", song.playCount);
        map.put("likeCount", song.likeCount);
        map.put("tags", song.tags != null ? song.tags : new ArrayList<>());
        List<String> keywords = generateKeywords(song.title, song.artist);
        map.put("searchKeywords", keywords);
        return map;
    }
}