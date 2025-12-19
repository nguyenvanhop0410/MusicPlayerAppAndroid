package com.example.musicapplication.ui.activity.album;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicapplication.R;
import com.example.musicapplication.ui.activity.player.PlayerActivity;
import com.example.musicapplication.ui.adapter.SongListAdapter;
import com.example.musicapplication.data.repository.AlbumRepository;
import com.example.musicapplication.data.repository.SongRepository;
import com.example.musicapplication.model.Song;
import com.example.musicapplication.player.MusicPlayer;
import com.example.musicapplication.player.PlaylistManager;
import com.example.musicapplication.utils.ImageLoader;
import com.example.musicapplication.utils.ToastUtils;
import com.example.musicapplication.utils.Logger;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class AlbumDetailActivity extends AppCompatActivity {

    private static final String TAG = "AlbumDetailActivity"; // Tag để lọc Log trong Logcat

    private RecyclerView recyclerView;
    private SongListAdapter songListAdapter;
    private List<Song> albumSongs = new ArrayList<>();
    private SongRepository songRepository;
    private AlbumRepository albumRepository;

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
            Logger.d("Receive Intent - Name: " + currentAlbumName);
            Logger.d("Receive Intent - ImageURL: " + intentAlbumImage);
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
        albumRepository = new AlbumRepository(this);
    }

    private void setupData(String intentAlbumImage) {
        if (currentAlbumName != null) {
            tvAlbumName.setText(currentAlbumName);
        }

        // 1. Ưu tiên hiển thị ảnh từ Intent (nếu có)
        if (intentAlbumImage != null && !intentAlbumImage.isEmpty()) {
            loadAlbumCover(intentAlbumImage);
            isImageSetFromIntent = true;
        } else {
            Logger.e("Intent Image URL is null or empty");
            imgCover.setImageResource(R.drawable.ic_music); // Ảnh placeholder mặc định
        }

        // 2. Tải danh sách bài hát từ Firebase
        if (currentAlbumName != null) {
            albumRepository.getSongsByAlbum(currentAlbumName, new AlbumRepository.OnResultListener<List<Song>>() {
                @Override
                public void onSuccess(List<Song> result) {
                    albumSongs.clear();
                    albumSongs.addAll(result);

                    // --- LOGIC FALLBACK ---
                    // Nếu Intent không có ảnh (hoặc ảnh lỗi), tự động lấy ảnh của bài hát đầu tiên làm ảnh bìa
                    if (!isImageSetFromIntent && !albumSongs.isEmpty()) {
                        String firstSongImage = albumSongs.get(0).getImageUrl();
                        Logger.d("Fallback to Song Image: " + firstSongImage);

                        if (firstSongImage != null && !firstSongImage.isEmpty()) {
                            runOnUiThread(() -> loadAlbumCover(firstSongImage));
                        }
                    }

                    runOnUiThread(() -> songListAdapter.notifyDataSetChanged());
                }

                @Override
                public void onError(Exception error) {
                    Logger.e(TAG, error);
                    runOnUiThread(() ->
                            ToastUtils.showError(AlbumDetailActivity.this, "Lỗi tải album")
                    );
                }
            });
        }

        // Nút Play All
        fabPlay.setOnClickListener(v -> {
            if (!albumSongs.isEmpty()) {
                onSongClick(albumSongs.get(0), 0);
            } else {
                ToastUtils.showWarning(this, "Album trống");
            }
        });
    }

    // Hàm load ảnh tách riêng để tái sử dụng và bắt lỗi Glide
    private void loadAlbumCover(String url) {
        ImageLoader.load(this, url, imgCover);
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