package com.example.musicapplication.data.repository;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileRepository {
    private static final String TAG = "ProfileRepository";
    private static final String USERS_COLLECTION = "users";
    private static final String PROFILE_IMAGES_PATH = "profile_images";

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private Context context;

    public ProfileRepository(Context context) {
        this.context = context.getApplicationContext();
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
    }

    public interface OnProfileUpdateListener {
        void onSuccess();
        void onError(Exception e);
    }

    public interface OnImageUploadListener {
        void onSuccess(String imageUrl);
        void onError(Exception e);
    }

    // 1. Upload ảnh lên Firebase Storage
    public void uploadProfileImage(Uri imageUri, OnImageUploadListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onError(new Exception("User not logged in"));
            return;
        }

        try {
            // Nén ảnh trước khi upload để tiết kiệm dung lượng
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos); // Chất lượng 70%
            byte[] data = baos.toByteArray();

            // Đường dẫn: profile_images/{userId}.jpg
            StorageReference ref = storage.getReference()
                    .child(PROFILE_IMAGES_PATH)
                    .child(user.getUid() + ".jpg");

            ref.putBytes(data)
                    .addOnSuccessListener(taskSnapshot -> {
                        ref.getDownloadUrl().addOnSuccessListener(uri -> {
                            listener.onSuccess(uri.toString());
                        });
                    })
                    .addOnFailureListener(listener::onError);

        } catch (IOException e) {
            listener.onError(e);
        }
    }

    // 2. Cập nhật thông tin User (Auth & Firestore)
    public void updateProfile(String newName, String newPhotoUrl, OnProfileUpdateListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onError(new Exception("User not logged in"));
            return;
        }

        // Bước A: Cập nhật Firebase Auth (DisplayName & PhotoUrl)
        UserProfileChangeRequest.Builder profileUpdates = new UserProfileChangeRequest.Builder();
        if (newName != null && !newName.isEmpty()) {
            profileUpdates.setDisplayName(newName);
        }
        if (newPhotoUrl != null) {
            profileUpdates.setPhotoUri(Uri.parse(newPhotoUrl));
        }

        user.updateProfile(profileUpdates.build())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Bước B: Cập nhật Firestore (để đồng bộ dữ liệu)
                        updateFirestoreUser(user.getUid(), newName, newPhotoUrl, listener);
                    } else {
                        listener.onError(task.getException());
                    }
                });
    }

    // Helper: Cập nhật Firestore
    private void updateFirestoreUser(String userId, String name, String photoUrl, OnProfileUpdateListener listener) {
        Map<String, Object> updates = new HashMap<>();
        if (name != null && !name.isEmpty()) updates.put("displayName", name);
        if (photoUrl != null) updates.put("photoUrl", photoUrl);

        firestore.collection(USERS_COLLECTION).document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> {
                    // Nếu document chưa tồn tại (user cũ), dùng set với merge
                    // firestore.collection(USERS_COLLECTION).document(userId).set(updates, SetOptions.merge());
                    listener.onError(e);
                });
    }
}