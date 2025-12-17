package com.example.musicapplication.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicapplication.R;
import com.example.musicapplication.adapter.SongListAdapter; // Dùng lại adapter list dọc
import com.example.musicapplication.data.repository.SongRepository;
import com.example.musicapplication.domain.model.Song;
import com.example.musicapplication.player.MusicPlayer;
import com.example.musicapplication.player.PlaylistManager;

import java.util.ArrayList;
import java.util.List;

public class GenreDetailActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private Toolbar toolbar;

    private SongListAdapter songAdapter;
    private List<Song> songList = new ArrayList<>();
    private SongRepository songRepository;

    private String genreName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_genre_detail);

        // Nhận tên thể loại từ Intent
        genreName = getIntent().getStringExtra("genreName");
        if (genreName == null) genreName = "Genre";

        songRepository = new SongRepository(this);
        initViews();
        loadSongsByGenre();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        TextView tvTitle = toolbar.findViewById(R.id.tv_title);
        tvTitle.setText(genreName);
        ImageButton btnBack = toolbar.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progress_bar);
        tvEmpty = findViewById(R.id.tv_empty);

        recyclerView = findViewById(R.id.recycler_genre_songs);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Tái sử dụng SongListAdapter để hiển thị list bài hát có lượt nghe
        songAdapter = new SongListAdapter(this, songList, (song, position, list) -> {
            // Xử lý click play bài hát
            PlaylistManager.getInstance().setPlaylist(list, position);
            MusicPlayer.getInstance(this).setPlaylist(list, position);

            if (song.isOnline() && song.getId() != null) {
                songRepository.incrementPlayCount(song.getId());
            }

            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putExtra("songId", song.getId());
            intent.putExtra("title", song.getTitle());
            intent.putExtra("artist", song.getArtist());
            intent.putExtra("imageUrl", song.getImageUrl());
            intent.putExtra("audioUrl", song.getAudioUrl());
            intent.putExtra("songIndex", position);
            intent.putExtra("isOnline", song.isOnline());
            startActivity(intent);
        });
        recyclerView.setAdapter(songAdapter);
    }

    private void loadSongsByGenre() {
        setLoading(true);

        // Gọi hàm tìm kiếm theo Tag trong Repository
        songRepository.getSongsByTag(genreName, new SongRepository.OnResultListener<List<Song>>() {
            @Override
            public void onSuccess(List<Song> result) {
                songList.clear();
                if (result != null && !result.isEmpty()) {
                    songList.addAll(result);
                    tvEmpty.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.VISIBLE);
                }

                runOnUiThread(() -> {
                    songAdapter.notifyDataSetChanged();
                    setLoading(false);
                });
            }

            @Override
            public void onError(Exception error) {
                setLoading(false);
                runOnUiThread(() ->
                        Toast.makeText(GenreDetailActivity.this, "Error loading genre: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }
}