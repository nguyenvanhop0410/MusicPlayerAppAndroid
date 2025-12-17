package com.example.musicapplication.main;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.musicapplication.R;
import com.example.musicapplication.adapter.SongAdapter;
import com.example.musicapplication.adapter.SongListAdapter;
import com.example.musicapplication.data.repository.SongRepository;
import com.example.musicapplication.domain.model.Song;
import com.example.musicapplication.player.MusicPlayer;
import com.example.musicapplication.player.PlaylistManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class AlbumDetailActivity extends AppCompatActivity {

    private static final String TAG = "AlbumDetailActivity"; // Tag để lọc Log trong Logcat

    private RecyclerView recyclerView;
    private SongListAdapter songListAdapter;
    private List<Song> albumSongs = new ArrayList<>();
    private SongRepository songRepository;

    private ImageView imgCover;
    private TextView tvAlbumName;
    private FloatingActionButton fabPlay;

    // Biến cờ: Đánh dấu xem ảnh đã được set từ màn hình trước (Intent) hay chưa
    private boolean isImageSetFromIntent = false;
    private String currentAlbumName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_detail);

        String intentAlbumImage = null;
        if (getIntent() != null) {
            currentAlbumName = getIntent().getStringExtra("albumName");
            intentAlbumImage = getIntent().getStringExtra("albumImage");

            // Log kiểm tra dữ liệu nhận được
            Log.d(TAG, "Receive Intent - Name: " + currentAlbumName);
            Log.d(TAG, "Receive Intent - ImageURL: " + intentAlbumImage);
        }

        initViews();
        setupData(intentAlbumImage);
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        imgCover = findViewById(R.id.img_album_cover);
        tvAlbumName = findViewById(R.id.tv_album_name);
        fabPlay = findViewById(R.id.fab_play_album);

        recyclerView = findViewById(R.id.recycler_album_songs);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Khởi tạo Adapter với lambda expression khớp với interface (song, pos, list)
        songListAdapter = new SongListAdapter(this, albumSongs, (song, position, list) -> {
            onSongClick(song, position);
        });
        recyclerView.setAdapter(songListAdapter);

        songRepository = new SongRepository(this);
    }

    private void setupData(String intentAlbumImage) {
        if (currentAlbumName != null) {
            tvAlbumName.setText(currentAlbumName);
        }

        // 1. Ưu tiên hiển thị ảnh từ Intent (nếu có)
        if (intentAlbumImage != null && !intentAlbumImage.isEmpty()) {
            loadAlbumCover(intentAlbumImage, "Intent Source");
            isImageSetFromIntent = true;
        } else {
            Log.w(TAG, "Intent Image URL is null or empty");
            imgCover.setImageResource(R.drawable.ic_music); // Ảnh placeholder mặc định
        }

        // 2. Tải danh sách bài hát từ Firebase
        if (currentAlbumName != null) {
            songRepository.getSongsByAlbum(currentAlbumName, new SongRepository.OnResultListener<List<Song>>() {
                @Override
                public void onSuccess(List<Song> result) {
                    albumSongs.clear();
                    albumSongs.addAll(result);

                    // --- LOGIC FALLBACK ---
                    // Nếu Intent không có ảnh (hoặc ảnh lỗi), tự động lấy ảnh của bài hát đầu tiên làm ảnh bìa
                    if (!isImageSetFromIntent && !albumSongs.isEmpty()) {
                        String firstSongImage = albumSongs.get(0).getImageUrl();
                        Log.d(TAG, "Fallback to Song Image: " + firstSongImage);

                        if (firstSongImage != null && !firstSongImage.isEmpty()) {
                            runOnUiThread(() -> loadAlbumCover(firstSongImage, "Song Fallback Source"));
                        }
                    }

                    runOnUiThread(() -> songListAdapter.notifyDataSetChanged());
                }

                @Override
                public void onError(Exception error) {
                    Log.e(TAG, "Error loading album songs", error);
                    runOnUiThread(() ->
                            Toast.makeText(AlbumDetailActivity.this, "Lỗi tải album", Toast.LENGTH_SHORT).show()
                    );
                }
            });
        }

        // Nút Play All
        fabPlay.setOnClickListener(v -> {
            if (!albumSongs.isEmpty()) {
                onSongClick(albumSongs.get(0), 0);
            } else {
                Toast.makeText(this, "Album trống", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Hàm load ảnh tách riêng để tái sử dụng và bắt lỗi Glide
    private void loadAlbumCover(String url, String source) {
        // Kiểm tra Activity còn hoạt động không để tránh crash
        if (!isDestroyed() && !isFinishing()) {
            Log.d(TAG, "Loading image from (" + source + "): " + url);

            Glide.with(this)
                    .load(url)
                    .placeholder(R.drawable.ic_music)
                    .error(R.drawable.ic_music)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            // Log lỗi chi tiết nếu Glide không tải được ảnh
                            Log.e(TAG, "GLIDE ERROR loading: " + url);
                            if (e != null) {
                                e.logRootCauses(TAG);
                            }
                            return false; // Trả về false để Glide xử lý hiển thị ảnh error
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d(TAG, "GLIDE SUCCESS loading image from " + source);
                            return false;
                        }
                    })
                    .into(imgCover);
        }
    }

    private void onSongClick(Song song, int position) {
        // Cập nhật PlaylistManager và MusicPlayer
        PlaylistManager.getInstance().setPlaylist(albumSongs, position);
        MusicPlayer.getInstance(this).setPlaylist(albumSongs, position);

        // Chuyển sang màn hình Player
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("songId", song.getId());
        intent.putExtra("title", song.getTitle());
        intent.putExtra("artist", song.getArtist());
        intent.putExtra("imageUrl", song.getImageUrl());
        intent.putExtra("audioUrl", song.getAudioUrl());
        intent.putExtra("songIndex", position);
        intent.putExtra("isOnline", true);

        startActivity(intent);
    }
}