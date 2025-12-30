package com.example.musicapplication.utils;

import android.util.Log;

import androidx.media3.common.BuildConfig;

// utils/Logger.java
public class Logger {
    private static final String TAG = "MusicApp";
    
    public static void d(String message) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message);
        }
    }
    
    public static void e(String message, Throwable throwable) {
        Log.e(TAG, message, throwable);
        // TODO: Gửi lên Firebase Crashlytics nếu cần
    }
    
    public static void e(String message) {
        Log.e(TAG, message);
    }
    
    public static void i(String message) {
        Log.i(TAG, message);
    }
    public static void w(String message) {
        Log.w(TAG, message);
    }
    /**
     * Log repository errors
     */
    public static void logRepositoryError(String repositoryName, String methodName, Exception e) {
        String message = String.format("[%s.%s] Error: %s", 
            repositoryName, methodName, e.getMessage());
        e(message, e);
    }
}
