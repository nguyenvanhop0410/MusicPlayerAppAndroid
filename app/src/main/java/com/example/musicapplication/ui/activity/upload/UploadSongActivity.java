package com.example.musicapplication.ui.activity.upload;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.musicapplication.R;
import com.example.musicapplication.data.repository.AuthRepository;
import com.example.musicapplication.data.repository.SongUploadRepository;
import com.example.musicapplication.model.Song;
import com.example.musicapplication.utils.ImageLoader;
import com.example.musicapplication.utils.ToastUtils;
import com.example.musicapplication.utils.Logger;
import com.example.musicapplication.utils.ValidationUtils;

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

    private SongUploadRepository songUploadRepository;
    private AuthRepository authRepository;
    private long currentDuration = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_song);

        songUploadRepository = new SongUploadRepository(this);
        authRepository = new AuthRepository(this);

        // Check authentication
        if (!authRepository.isLoggedIn()) {
            ToastUtils.showWarning(this, "Vui lòng đăng nhập để upload nhạc");
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
                handleAudioSelection(data.getData());
            } else if (requestCode == PICK_IMAGE) {
                handleImageSelection(data.getData());
            }
        }
    }

    private void handleAudioSelection(Uri uri) {
        audioUri = uri;
        if (audioUri != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(audioUri);
                audioBytes = readInputStream(inputStream);
                extractAudioMetadata(audioUri);
                btnSelectAudio.setText("✓ Đã chọn file nhạc");
                ToastUtils.showSuccess(this, "Đã chọn file nhạc");
            } catch (Exception e) {
                Logger.e(TAG, e);
                ToastUtils.showError(this, "Lỗi đọc file nhạc");
            }
        }
    }

    private void handleImageSelection(Uri uri) {
        imageUri = uri;
        if (imageUri != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                imageBytes = readInputStream(inputStream);
                ImageLoader.load(this, imageUri.toString(), imgCover);
                btnSelectImage.setText("✓ Đã chọn ảnh");
                ToastUtils.showSuccess(this, "Đã chọn ảnh bìa");
            } catch (Exception e) {
                Logger.e(TAG, e);
                ToastUtils.showError(this, "Lỗi đọc file ảnh");
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

            if (ValidationUtils.isNotEmpty(title)) etTitle.setText(title);
            if (ValidationUtils.isNotEmpty(artist)) etArtist.setText(artist);
            if (ValidationUtils.isNotEmpty(album)) etAlbum.setText(album);

            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            currentDuration = durationStr != null ? Long.parseLong(durationStr) : 0;

            byte[] art = retriever.getEmbeddedPicture();
            if (art != null) {
                Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(art, 0, art.length);
                imgCover.setImageBitmap(bitmap);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                imageBytes = stream.toByteArray();
            }

            retriever.release();
        } catch (Exception e) {
            Logger.e(TAG, e);
            currentDuration = 0;
        }
    }

    private void uploadSong() {
        String title = etTitle.getText().toString().trim();
        String artist = etArtist.getText().toString().trim();
        String album = etAlbum.getText().toString().trim();
        String tagsText = etTags.getText().toString().trim();

        // Validation
        if (!ValidationUtils.isValidSongTitle(title)) {
            etTitle.setError(ValidationUtils.getSongTitleError(title));
            etTitle.requestFocus();
            return;
        }

        if (!ValidationUtils.isNotEmpty(artist)) {
            etArtist.setError("Vui lòng nhập tên nghệ sĩ");
            etArtist.requestFocus();
            return;
        }

        if (audioBytes == null || audioBytes.length == 0) {
            ToastUtils.showWarning(this, "Vui lòng chọn file nhạc");
            return;
        }

        AuthRepository.FirebaseUserWrapper user = authRepository.getCurrentUser();
        if (user == null) {
            ToastUtils.showWarning(this, "Vui lòng đăng nhập");
            return;
        }

        Song song = new Song();
        song.title = title;
        song.artist = artist;
        song.album = ValidationUtils.isNotEmpty(album) ? album : "";
        song.duration = currentDuration;
        song.uploaderId = user.getUid();
        song.uploaderName = user.getEmail() != null ? user.getEmail().split("@")[0] : "User";
        song.uploadDate = System.currentTimeMillis();
        song.playCount = 0;
        song.likeCount = 0;

        if (!tagsText.isEmpty()) {
            String[] tagArray = tagsText.split(",");
            song.tags = new java.util.ArrayList<>();
            for (String tag : tagArray) {
                String trimmedTag = tag.trim();
                if (ValidationUtils.isNotEmpty(trimmedTag)) {
                    song.tags.add(trimmedTag);
                }
            }
        } else {
            song.tags = new java.util.ArrayList<>();
        }

        progressBar.setVisibility(View.VISIBLE);
        btnUpload.setEnabled(false);

        songUploadRepository.uploadSong(song, audioBytes, imageBytes, new SongUploadRepository.OnResultListener<String>() {
            @Override
            public void onSuccess(String songId) {
                Logger.d("Song uploaded successfully: " + songId);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnUpload.setEnabled(true);
                    ToastUtils.showSuccess(UploadSongActivity.this, "Upload thành công!");
                    finish();
                });
            }

            @Override
            public void onError(Exception error) {
                Logger.e(TAG, error);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnUpload.setEnabled(true);
                    ToastUtils.showError(UploadSongActivity.this, "Lỗi upload: " + error.getMessage());
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ToastUtils.showSuccess(this, "Đã cấp quyền");
            } else {
                ToastUtils.showWarning(this, "Cần quyền để chọn file");
            }
        }
    }
}