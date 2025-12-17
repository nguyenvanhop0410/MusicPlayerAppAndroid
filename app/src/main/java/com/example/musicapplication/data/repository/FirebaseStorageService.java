package com.example.musicapplication.data.repository;

import android.content.Context;
import android.util.Log;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class FirebaseStorageService implements StorageService {
    private static final String TAG = "FirebaseStorage";
    private FirebaseStorage storage;
    private Context context;
    
    public FirebaseStorageService(Context context) {
        this.context = context.getApplicationContext();
        this.storage = FirebaseStorage.getInstance();
    }
    
    @Override
    public void uploadAudio(String songId, byte[] audioData, OnUploadListener listener) {
        StorageReference audioRef = storage.getReference().child("songs").child(songId + ".mp3");
        UploadTask uploadTask = audioRef.putBytes(audioData);
        
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            audioRef.getDownloadUrl().addOnSuccessListener(uri -> {
                Log.d(TAG, "Audio uploaded successfully: " + uri.toString());
                listener.onSuccess(uri.toString());
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error getting audio URL", e);
                listener.onError(e);
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error uploading audio", e);
            listener.onError(e);
        });
    }
    
    @Override
    public void uploadImage(String songId, byte[] imageData, OnUploadListener listener) {
        StorageReference imageRef = storage.getReference().child("images").child(songId + ".jpg");
        UploadTask uploadTask = imageRef.putBytes(imageData);
        
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                Log.d(TAG, "Image uploaded successfully: " + uri.toString());
                listener.onSuccess(uri.toString());
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error getting image URL", e);
                listener.onError(e);
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error uploading image", e);
            listener.onError(e);
        });
    }
    
    @Override
    public void deleteFile(String path, OnDeleteListener listener) {
        StorageReference fileRef = storage.getReference().child(path);
        fileRef.delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "File deleted successfully: " + path);
                listener.onSuccess();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error deleting file", e);
                listener.onError(e);
            });
    }
    
    @Override
    public String getFileUrl(String fileId) {
        // Firebase Storage URLs are generated on upload, return null here
        // Use getDownloadUrl() from StorageReference instead
        return null;
    }
}

