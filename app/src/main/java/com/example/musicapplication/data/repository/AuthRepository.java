package com.example.musicapplication.data.repository;

import android.content.Context;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class AuthRepository {
    private static final String TAG = "AuthRepository";
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private Context context;
    
    public AuthRepository(Context context) {
        this.context = context.getApplicationContext();
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }
    
    // Wrapper class để tương thích với code cũ
    public static class FirebaseUserWrapper {
        private String uid;
        private String email;
        
        public FirebaseUserWrapper(FirebaseUser user) {
            this.uid = user != null ? user.getUid() : null;
            this.email = user != null ? user.getEmail() : null;
        }
        
        public String getUid() {
            return uid;
        }
        
        public String getEmail() {
            return email;
        }
    }
    
    public interface OnAuthResultListener {
        void onSuccess(FirebaseUserWrapper user);
        void onError(Exception error);
    }
    
    public interface OnUserDataListener {
        void onSuccess(com.example.musicapplication.domain.model.User user);
        void onError(Exception error);
    }
    
    // Đăng ký tài khoản mới
    public void register(String email, String password, String displayName,
                        OnAuthResultListener listener) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        // Save user data to Firestore
                        saveUserToFirestore(user, displayName, listener);
                    } else {
                        listener.onError(new Exception("User creation failed"));
                    }
                } else {
                    Log.e(TAG, "Registration failed", task.getException());
                    listener.onError(task.getException() != null ? 
                        task.getException() : new Exception("Registration failed"));
                }
            });
    }
    
    private void saveUserToFirestore(FirebaseUser user, String displayName, OnAuthResultListener listener) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getUid());
        userData.put("email", user.getEmail());
        userData.put("displayName", displayName);
        userData.put("createdAt", System.currentTimeMillis());
        userData.put("favoriteSongs", new java.util.ArrayList<>());
        userData.put("playlists", new java.util.ArrayList<>());
        userData.put("followers", 0);
        userData.put("following", 0);
        userData.put("isPremium", false);
        
        firestore.collection("users").document(user.getUid())
            .set(userData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "User document created successfully");
                listener.onSuccess(new FirebaseUserWrapper(user));
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error creating user document", e);
                // Still return success for auth, document can be created later
                listener.onSuccess(new FirebaseUserWrapper(user));
            });
    }
    
    // Đăng nhập
    public void login(String email, String password, OnAuthResultListener listener) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    Log.d(TAG, "Login successful: " + (user != null ? user.getUid() : "null"));
                    listener.onSuccess(new FirebaseUserWrapper(user));
                } else {
                    Log.e(TAG, "Login failed", task.getException());
                    listener.onError(task.getException() != null ? 
                        task.getException() : new Exception("Login failed"));
                }
            });
    }
    
    // Đăng xuất
    public void logout() {
        firebaseAuth.signOut();
        Log.d(TAG, "User logged out");
    }
    
    // Lấy user hiện tại
    public FirebaseUserWrapper getCurrentUser() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null ? new FirebaseUserWrapper(user) : null;
    }
    
    // Kiểm tra đã đăng nhập chưa
    public boolean isLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }
    
    // Lấy thông tin user từ database
    public void getUserData(String userId, OnUserDataListener listener) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    com.example.musicapplication.domain.model.User user = 
                        new com.example.musicapplication.domain.model.User();
                    user.id = documentSnapshot.getString("id");
                    user.email = documentSnapshot.getString("email");
                    user.displayName = documentSnapshot.getString("displayName");
                    listener.onSuccess(user);
                } else {
                    listener.onError(new Exception("User not found"));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting user data", e);
                listener.onError(e);
            });
    }
}
