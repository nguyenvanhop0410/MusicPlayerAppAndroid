package com.example.musicapplication.upload;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.musicapplication.R;
import com.example.musicapplication.data.repository.AuthRepository;
import com.example.musicapplication.data.repository.SongRepository;
import com.example.musicapplication.domain.model.Song;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class UploadSongActivity extends AppCompatActivity {
    private static final String TAG = "UploadSongActivity";
    private static final int PICK_AUDIO = 100;
    private static final int PICK_IMAGE = 101;
    private static final int REQ_PERM = 102;
    
    private EditText etTitle, etArtist, etAlbum, etTags;
    private ImageView imgCover;
    private Button btnSelectAudio, btnSelectImage, btnUpload;
    private ProgressBar progressBar;
    
    private Uri audioUri;
    private Uri imageUri;
    private byte[] audioBytes;
    private byte[] imageBytes;
    
    private SongRepository songRepository;
    private AuthRepository authRepository;
    private long currentDuration = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_song);
        
        songRepository = new SongRepository(this);
        authRepository = new AuthRepository(this);
        
        // Check authentication
        if (!authRepository.isLoggedIn()) {
            Toast.makeText(this, "Vui lòng đăng nhập để upload nhạc", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initViews();
        setupClickListeners();
    }
    
    private void initViews() {
        etTitle = findViewById(R.id.et_title);
        etArtist = findViewById(R.id.et_artist);
        etAlbum = findViewById(R.id.et_album);
        etTags = findViewById(R.id.et_tags);
        imgCover = findViewById(R.id.img_cover);
        btnSelectAudio = findViewById(R.id.btn_select_audio);
        btnSelectImage = findViewById(R.id.btn_select_image);
        btnUpload = findViewById(R.id.btn_upload);
        progressBar = findViewById(R.id.progress_bar);
        
        // Set default image
        imgCover.setImageResource(R.drawable.ic_music);
    }
    
    private void setupClickListeners() {
        btnSelectAudio.setOnClickListener(v -> {
            if (hasStoragePermission()) {
                selectAudio();
            } else {
                requestStoragePermission();
            }
        });
        
        btnSelectImage.setOnClickListener(v -> {
            if (hasStoragePermission()) {
                selectImage();
            } else {
                requestStoragePermission();
            }
        });
        
        btnUpload.setOnClickListener(v -> uploadSong());
    }
    
    private boolean hasStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    private void requestStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_AUDIO}, REQ_PERM);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_PERM);
        }
    }
    
    private void selectAudio() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(Intent.createChooser(intent, "Chọn file nhạc"), PICK_AUDIO);
    }
    
    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh bìa"), PICK_IMAGE);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_AUDIO) {
                audioUri = data.getData();
                if (audioUri != null) {
                    try {
                        // Read audio file
                        InputStream inputStream = getContentResolver().openInputStream(audioUri);
                        audioBytes = readInputStream(inputStream);
                        
                        // Extract metadata
                        extractAudioMetadata(audioUri);
                        
                        btnSelectAudio.setText("✓ Đã chọn file nhạc");
                        Toast.makeText(this, "Đã chọn file nhạc", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading audio file", e);
                        Toast.makeText(this, "Lỗi đọc file nhạc: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (requestCode == PICK_IMAGE) {
                imageUri = data.getData();
                if (imageUri != null) {
                    try {
                        // Read image file
                        InputStream inputStream = getContentResolver().openInputStream(imageUri);
                        imageBytes = readInputStream(inputStream);
                        
                        // Display image
                        Glide.with(this)
                                .load(imageUri)
                                .placeholder(R.drawable.ic_music)
                                .error(R.drawable.ic_music)
                                .centerCrop()
                                .into(imgCover);
                        
                        btnSelectImage.setText("✓ Đã chọn ảnh");
                        Toast.makeText(this, "Đã chọn ảnh bìa", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading image file", e);
                        Toast.makeText(this, "Lỗi đọc file ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
    
    private byte[] readInputStream(InputStream inputStream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }
    
    private void extractAudioMetadata(Uri uri) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(this, uri);
            
            String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            
            if (title != null && !title.isEmpty()) {
                etTitle.setText(title);
            }
            if (artist != null && !artist.isEmpty()) {
                etArtist.setText(artist);
            }
            if (album != null && !album.isEmpty()) {
                etAlbum.setText(album);
            }
            
            // Store duration for later use in upload
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationStr != null && !durationStr.isEmpty()) {
                try {
                    long duration = Long.parseLong(durationStr);
                    Log.d(TAG, "Extracted duration: " + duration + "ms");
                    // Store duration in a temporary variable or set directly to song later
                    currentDuration = duration;
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing duration: " + durationStr, e);
                    currentDuration = 0;
                }
            } else {
                currentDuration = 0;
            }
            
            // Extract album art
            byte[] art = retriever.getEmbeddedPicture();
            if (art != null) {
                Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(art, 0, art.length);
                if (bitmap != null) {
                    Glide.with(this)
                            .load(bitmap)
                            .centerCrop()
                            .into(imgCover);
                    
                    // Convert bitmap to bytes for upload
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                    imageBytes = stream.toByteArray();
                }
            }
            
            retriever.release();
        } catch (Exception e) {
            Log.e(TAG, "Error extracting metadata", e);
            currentDuration = 0;
        }
    }
    
    private void uploadSong() {
        String title = etTitle.getText().toString().trim();
        String artist = etArtist.getText().toString().trim();
        String album = etAlbum.getText().toString().trim();
        String tagsText = etTags.getText().toString().trim();
        
        // Validation
        if (title.isEmpty()) {
            etTitle.setError("Vui lòng nhập tên bài hát");
            etTitle.requestFocus();
            return;
        }
        
        if (artist.isEmpty()) {
            etArtist.setError("Vui lòng nhập tên nghệ sĩ");
            etArtist.requestFocus();
            return;
        }
        
        if (audioBytes == null || audioBytes.length == 0) {
            Toast.makeText(this, "Vui lòng chọn file nhạc", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get user info
        AuthRepository.FirebaseUserWrapper user = authRepository.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create song object
        Song song = new Song();
        song.title = title;
        song.artist = artist;
        song.album = album != null && !album.isEmpty() ? album : "";
        song.duration = currentDuration; // Use extracted duration
        song.uploaderId = user.getUid();
        song.uploaderName = user.getEmail() != null ? user.getEmail().split("@")[0] : "User";
        song.uploadDate = System.currentTimeMillis();
        song.playCount = 0;
        song.likeCount = 0;
        
        // Parse tags from comma-separated string
        if (!tagsText.isEmpty()) {
            String[] tagArray = tagsText.split(",");
            song.tags = new java.util.ArrayList<>();
            for (String tag : tagArray) {
                String trimmedTag = tag.trim();
                if (!trimmedTag.isEmpty()) {
                    song.tags.add(trimmedTag);
                }
            }
        } else {
            song.tags = new java.util.ArrayList<>();
        }
        
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        btnUpload.setEnabled(false);
        
        // Upload song
        songRepository.uploadSong(song, audioBytes, imageBytes, new SongRepository.OnResultListener<String>() {
            @Override
            public void onSuccess(String songId) {
                Log.d(TAG, "Song uploaded successfully: " + songId);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnUpload.setEnabled(true);
                    Toast.makeText(UploadSongActivity.this, "Upload thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error uploading song", error);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnUpload.setEnabled(true);
                    Toast.makeText(UploadSongActivity.this, "Lỗi upload: " + error.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Đã cấp quyền", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Cần quyền để chọn file", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

