package com.example.musicapplication.main;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicapplication.R;
import com.example.musicapplication.adapter.SongListAdapter;
import com.example.musicapplication.data.repository.PlaylistRepository;
import com.example.musicapplication.data.repository.SongRepository;
import com.example.musicapplication.domain.model.Song;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class AddSongPlaylistActivity extends AppCompatActivity {

    private TextInputEditText etSearch;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private SongListAdapter adapter;
    private List<Song> searchResults = new ArrayList<>();
    private SongRepository songRepository;
    private PlaylistRepository playlistRepository;
    private String playlistId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_song_playlist);

        playlistId = getIntent().getStringExtra("playlistId");
        songRepository = new SongRepository(this);
        playlistRepository = new PlaylistRepository(this);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        etSearch = findViewById(R.id.et_search);
        recyclerView = findViewById(R.id.recycler_search_results);
        progressBar = findViewById(R.id.progress_bar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Khi click vào bài hát -> Thêm vào Playlist
        adapter = new SongListAdapter(this, searchResults, (song, position, list) -> {
            addSongToPlaylist(song);
        });
        recyclerView.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Load mặc định một số bài
        performSearch("");
    }

    private void performSearch(String query) {
        progressBar.setVisibility(View.VISIBLE);
        // Nếu query rỗng thì lấy bài hát mới nhất hoặc trending
        if (query.isEmpty()) {
            songRepository.getTrendingSongs(20, new SongRepository.OnResultListener<List<Song>>() {
                @Override
                public void onSuccess(List<Song> result) {
                    updateList(result);
                }
                @Override public void onError(Exception e) { progressBar.setVisibility(View.GONE); }
            });
        } else {
            songRepository.searchSongs(query, new SongRepository.OnResultListener<List<Song>>() {
                @Override
                public void onSuccess(List<Song> result) {
                    updateList(result);
                }
                @Override public void onError(Exception e) { progressBar.setVisibility(View.GONE); }
            });
        }
    }

    private void updateList(List<Song> list) {
        searchResults.clear();
        searchResults.addAll(list);
        runOnUiThread(() -> {
            adapter.notifyDataSetChanged();
            progressBar.setVisibility(View.GONE);
        });
    }

    private void addSongToPlaylist(Song song) {
        if (playlistId == null) return;

        playlistRepository.addSongToPlaylist(playlistId, song.getId(), new PlaylistRepository.OnResultListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(AddSongPlaylistActivity.this, "Đã thêm: " + song.getTitle(), Toast.LENGTH_SHORT).show();
                // Không finish để có thể thêm tiếp bài khác
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(AddSongPlaylistActivity.this, "Lỗi thêm bài hát", Toast.LENGTH_SHORT).show();
            }
        });
    }
}