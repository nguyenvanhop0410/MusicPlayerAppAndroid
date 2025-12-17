package com.example.musicapplication.data.repository;

import android.content.Context;
import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.musicapplication.domain.model.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private FirebaseFirestore firestore;
    private Context context;
    private static final String USERS_COLLECTION = "users";
    
    public UserRepository(Context context) {
        this.context = context.getApplicationContext();
        this.firestore = FirebaseFirestore.getInstance();
    }
    
    public interface OnResultListener<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }
    
    // Lấy thông tin user
    public void getUser(String userId, OnResultListener<User> listener) {
        firestore.collection(USERS_COLLECTION).document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    User user = documentToUser(documentSnapshot.getId(), documentSnapshot.getData());
                    listener.onSuccess(user);
                } else {
                    listener.onError(new Exception("User not found"));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting user", e);
                listener.onError(e);
            });
    }
    
    // Cập nhật thông tin user
    public void updateUser(User user, OnResultListener<Void> listener) {
        Map<String, Object> updates = new HashMap<>();
        if (user.email != null) updates.put("email", user.email);
        if (user.displayName != null) updates.put("displayName", user.displayName);
        if (user.photoUrl != null) updates.put("photoUrl", user.photoUrl);
        if (user.bio != null) updates.put("bio", user.bio);
        
        firestore.collection(USERS_COLLECTION).document(user.id)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "User updated successfully");
                listener.onSuccess(null);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating user", e);
                listener.onError(e);
            });
    }
    
    // Thêm bài hát vào favorites
    public void addToFavorites(String userId, String songId, OnResultListener<Void> listener) {
        firestore.collection(USERS_COLLECTION).document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    List<String> favorites = (List<String>) documentSnapshot.get("favoriteSongs");
                    if (favorites == null) {
                        favorites = new ArrayList<>();
                    }
                    if (!favorites.contains(songId)) {
                        favorites.add(songId);
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("favoriteSongs", favorites);
                        
                        firestore.collection(USERS_COLLECTION).document(userId)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> listener.onSuccess(null))
                            .addOnFailureListener(e -> listener.onError(e));
                    } else {
                        listener.onSuccess(null);
                    }
                } else {
                    listener.onError(new Exception("User not found"));
                }
            })
            .addOnFailureListener(e -> listener.onError(e));
    }
    
    // Xóa bài hát khỏi favorites
    public void removeFromFavorites(String userId, String songId, OnResultListener<Void> listener) {
        firestore.collection(USERS_COLLECTION).document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    List<String> favorites = (List<String>) documentSnapshot.get("favoriteSongs");
                    if (favorites != null) {
                        favorites.remove(songId);
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("favoriteSongs", favorites);
                        
                        firestore.collection(USERS_COLLECTION).document(userId)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> listener.onSuccess(null))
                            .addOnFailureListener(e -> listener.onError(e));
                    } else {
                        listener.onSuccess(null);
                    }
                } else {
                    listener.onError(new Exception("User not found"));
                }
            })
            .addOnFailureListener(e -> listener.onError(e));
    }
    
    // Lấy danh sách favorites
    public void getFavorites(String userId, OnResultListener<List<String>> listener) {
        firestore.collection(USERS_COLLECTION).document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    List<String> favorites = (List<String>) documentSnapshot.get("favoriteSongs");
                    listener.onSuccess(favorites != null ? favorites : new ArrayList<>());
                } else {
                    listener.onError(new Exception("User not found"));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting favorites", e);
                listener.onError(e);
            });
    }
    
    private User documentToUser(String docId, Map<String, Object> data) {
        try {
            User user = new User();
            user.id = docId;
            user.email = (String) data.get("email");
            user.displayName = (String) data.get("displayName");
            user.photoUrl = (String) data.get("photoUrl");
            user.bio = (String) data.get("bio");
            
            Object favoriteSongsObj = data.get("favoriteSongs");
            user.favoriteSongs = favoriteSongsObj != null ? (List<String>) favoriteSongsObj : new ArrayList<>();
            
            Object playlistsObj = data.get("playlists");
            user.playlists = playlistsObj != null ? (List<String>) playlistsObj : new ArrayList<>();
            
            Object followersObj = data.get("followers");
            user.followers = followersObj != null ? ((Number) followersObj).intValue() : 0;
            
            Object followingObj = data.get("following");
            user.following = followingObj != null ? ((Number) followingObj).intValue() : 0;
            
            Object isPremiumObj = data.get("isPremium");
            user.isPremium = isPremiumObj != null ? (Boolean) isPremiumObj : false;
            
            Object createdAtObj = data.get("createdAt");
            user.createdAt = createdAtObj != null ? ((Number) createdAtObj).longValue() : 0;
            
            return user;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to user", e);
            return null;
        }
    }
}
