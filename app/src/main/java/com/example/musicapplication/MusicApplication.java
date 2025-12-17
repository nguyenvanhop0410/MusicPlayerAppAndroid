package com.example.musicapplication;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class MusicApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
    }
}

