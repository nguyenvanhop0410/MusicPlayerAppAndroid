package com.example.musicapplication.ui.activity;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicapplication.R;
import com.example.musicapplication.data.repository.SongRepository;
import com.example.musicapplication.model.Song;
import com.example.musicapplication.player.MusicPlayer;
import com.example.musicapplication.player.PlaylistManager;
import com.example.musicapplication.ui.activity.player.PlayerActivity;
import com.example.musicapplication.ui.adapter.SongListAdapter;
import com.example.musicapplication.utils.ImageLoader;
import com.example.musicapplication.utils.ToastUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity hiển thị chi tiết nghệ sĩ và danh sách bài hát của họ
 */
public class ArtistDetailActivity extends AppCompatActivity {
    public static final String EXTRA_ARTIST_ID = "artist_id";
    public static final String EXTRA_ARTIST_NAME = "artist_name";
    public static final String EXTRA_ARTIST_IMAGE = "artist_image";
    public static final String EXTRA_ARTIST_FOLLOWERS = "artist_followers";

    private ImageView imageArtist;
    private TextView tvArtistName;
    private TextView tvFollowers;
    private RecyclerView recyclerSongs;
    private ProgressBar progressBar;
    private TextView tvEmptyState;

    private SongRepository songRepository;
    private SongListAdapter songAdapter;
    private List<Song> songList;
    private MusicPlayer musicPlayer;

    private String artistName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_detail);

        initViews();
        setupToolbar();
        getArtistInfo();
        setupRecyclerView();
        loadSongs();
    }

    private void initViews() {
        imageArtist = findViewById(R.id.image_artist);
        tvArtistName = findViewById(R.id.tv_artist_name);
        tvFollowers = findViewById(R.id.tv_followers);
        recyclerSongs = findViewById(R.id.recycler_songs);
        progressBar = findViewById(R.id.progress_bar);
        tvEmptyState = findViewById(R.id.tv_empty_state);

        songRepository = new SongRepository(this);
        musicPlayer = MusicPlayer.getInstance(this);
        songList = new ArrayList<>();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
    }

    private void getArtistInfo() {
        Intent intent = getIntent();
        artistName = intent.getStringExtra(EXTRA_ARTIST_NAME);
        String artistImage = intent.getStringExtra(EXTRA_ARTIST_IMAGE);
        long followers = intent.getLongExtra(EXTRA_ARTIST_FOLLOWERS, 0);

        tvArtistName.setText(artistName != null ? artistName : "Unknown Artist");
        tvFollowers.setText(formatFollowers(followers));

        if (artistImage != null && !artistImage.isEmpty()) {
            ImageLoader.load(this, artistImage, imageArtist);
        }
    }

    private void setupRecyclerView() {
        recyclerSongs.setLayoutManager(new LinearLayoutManager(this));
        songAdapter = new SongListAdapter(this, songList, (song, position, playlist) -> {
            // Set playlist vào PlaylistManager để PlayerActivity có thể next/previous
            PlaylistManager.getInstance().setPlaylist(playlist);
            
            // Mở PlayerActivity với thông tin bài hát
            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putExtra("audioUrl", song.isOnline() ? song.getAudioUrl() : song.uri);
            intent.putExtra("songId", song.getId());
            intent.putExtra("title", song.getTitle());
            intent.putExtra("artist", song.getArtist());
            intent.putExtra("album", song.getAlbum());
            intent.putExtra("imageUrl", song.getImageUrl());
            intent.putExtra("isOnline", song.isOnline());
            intent.putExtra("songIndex", position);
            startActivity(intent);
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });
        recyclerSongs.setAdapter(songAdapter);
    }

    private void loadSongs() {
        if (artistName == null || artistName.isEmpty()) {
            showEmptyState("Không tìm thấy nghệ sĩ");
            return;
        }

        showLoading(true);
        songRepository.getSongsByArtist(artistName, new SongRepository.OnResultListener<List<Song>>() {
            @Override
            public void onSuccess(List<Song> result) {
                showLoading(false);
                if (result != null && !result.isEmpty()) {
                    songList.clear();
                    songList.addAll(result);
                    songAdapter.notifyDataSetChanged();
                    showEmptyState(null);
                } else {
                    showEmptyState("Chưa có bài hát nào của nghệ sĩ này");
                }
            }

            @Override
            public void onError(Exception error) {
                showLoading(false);
                String errorMsg = error.getMessage();
                // Hiển thị thông báo phù hợp cho lỗi mạng
                if (errorMsg != null && errorMsg.contains("mạng")) {
                    showEmptyState(errorMsg);
                    ToastUtils.showError(ArtistDetailActivity.this, errorMsg);
                } else {
                    showEmptyState("Lỗi khi tải bài hát: " + errorMsg);
                    ToastUtils.showError(ArtistDetailActivity.this, "Không thể tải bài hát");
                }
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerSongs.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showEmptyState(String message) {
        if (message != null && !message.isEmpty()) {
            tvEmptyState.setText(message);
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerSongs.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerSongs.setVisibility(View.VISIBLE);
        }
    }

    private String formatFollowers(long followers) {
        if (followers >= 1_000_000) {
            return String.format("%.1fM người theo dõi", followers / 1_000_000.0);
        } else if (followers >= 1_000) {
            return String.format("%.1fK người theo dõi", followers / 1_000.0);
        } else {
            return followers + " người theo dõi";
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
