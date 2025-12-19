package com.example.musicapplication.ui.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicapplication.R;
import com.example.musicapplication.ui.adapter.GenreAdapter;
import com.example.musicapplication.ui.adapter.SongListAdapter;
import com.example.musicapplication.data.repository.SearchRepository;
import com.example.musicapplication.data.repository.SongRepository;
import com.example.musicapplication.model.Song;
import com.example.musicapplication.ui.activity.genre.GenreDetailActivity; // Import Activity mới
import com.example.musicapplication.ui.activity.player.PlayerActivity;
import com.example.musicapplication.model.Genre;
import com.example.musicapplication.player.MusicPlayer;
import com.example.musicapplication.player.PlaylistManager;
import com.google.android.material.textfield.TextInputEditText;
import com.example.musicapplication.utils.ToastUtils;
import com.example.musicapplication.utils.Logger;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    // ... (Giữ nguyên các biến)
    private RecyclerView recyclerGenres;
    private RecyclerView recyclerSearchResults;
    private TextView tvBrowseAll;
    private TextInputEditText etSearch;
    private ProgressBar progressBar;

    private GenreAdapter genreAdapter;
    private SongListAdapter searchResultAdapter;

    private List<Genre> genreList = new ArrayList<>();
    private List<Song> searchResults = new ArrayList<>();
    private SongRepository songRepository;
    private SearchRepository searchRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        songRepository = new SongRepository(getContext());
        searchRepository = new SearchRepository(getContext());
        initViews(view);
        setupGenreData();
        setupSearchLogic();
        return view;
    }

    private void initViews(View view) {
        try {
            etSearch = view.findViewById(R.id.et_search);
            tvBrowseAll = view.findViewById(R.id.tv_browse_all);
            progressBar = view.findViewById(R.id.progress_bar);

            // 1. Setup Grid Thể loại
            recyclerGenres = view.findViewById(R.id.recycler_genres);
            if (recyclerGenres != null && getContext() != null) {
                recyclerGenres.setLayoutManager(new GridLayoutManager(getContext(), 2));

                genreAdapter = new GenreAdapter(getContext(), genreList, genre -> {
                    // --- THAY ĐỔI QUAN TRỌNG TẠI ĐÂY ---
                    // Khi click thẻ, mở màn hình GenreDetailActivity
                    if (getContext() != null) {
                        Intent intent = new Intent(getContext(), GenreDetailActivity.class);
                        intent.putExtra("genreName", genre.getName());
                        startActivity(intent);
                    }
                });
                recyclerGenres.setAdapter(genreAdapter);
            }

            // 2. Setup List Kết quả tìm kiếm (Giữ nguyên)
            recyclerSearchResults = view.findViewById(R.id.recycler_search_results);
            if (recyclerSearchResults != null && getContext() != null) {
                recyclerSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
                searchResultAdapter = new SongListAdapter(getContext(), searchResults, this::onSongClick);
                recyclerSearchResults.setAdapter(searchResultAdapter);
            }
        } catch (Exception e) {
            Logger.e("Error initializing views: " + e.getMessage());
        }
    }

    private void setupGenreData() {
        genreList.clear();
        genreList.add(new Genre("1", "Pop", R.drawable.ic_music, Color.parseColor("#E91E63")));
        genreList.add(new Genre("2", "Ballad", R.drawable.ic_music, Color.parseColor("#9C27B0")));
        genreList.add(new Genre("3", "Rap", R.drawable.ic_music, Color.parseColor("#F44336")));
        genreList.add(new Genre("4", "Indie", R.drawable.ic_music, Color.parseColor("#009688")));
        genreList.add(new Genre("5", "R&B", R.drawable.ic_music, Color.parseColor("#3F51B5")));
        genreList.add(new Genre("6", "Rock", R.drawable.ic_music, Color.parseColor("#FF9800")));
        genreList.add(new Genre("7", "Cach Mang", R.drawable.ic_music, Color.parseColor("#607D8B")));
        genreList.add(new Genre("8", "Remix", R.drawable.ic_music, Color.parseColor("#795548")));
        genreAdapter.notifyDataSetChanged();
    }

    private void setupSearchLogic() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    showGenres(true);
                } else {
                    showGenres(false);
                    performSearch(query);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void performSearch(String query) {
        // Code tìm kiếm theo Tên/Nghệ sĩ
        searchRepository.searchSongs(query, new SearchRepository.OnResultListener<List<Song>>() {
            @Override
            public void onSuccess(List<Song> result) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        searchResults.clear();
                        searchResults.addAll(result);
                        searchResultAdapter.notifyDataSetChanged();
                    });
                }
            }
            @Override
            public void onError(Exception error) {}
        });
    }

    private void showGenres(boolean show) {
        if (show) {
            recyclerGenres.setVisibility(View.VISIBLE);
            tvBrowseAll.setVisibility(View.VISIBLE);
            recyclerSearchResults.setVisibility(View.GONE);
        } else {
            recyclerGenres.setVisibility(View.GONE);
            tvBrowseAll.setVisibility(View.GONE);
            recyclerSearchResults.setVisibility(View.VISIBLE);
        }
    }

    private void onSongClick(Song song, int position, List<Song> playlist) {
        try {
            if (getContext() == null || song == null) {
                Logger.e("Context or song is null");
                return;
            }

            PlaylistManager.getInstance().setPlaylist(playlist, position);
            MusicPlayer.getInstance(getContext()).setPlaylist(playlist, position);

            if (song.isOnline() && song.getId() != null) {
                songRepository.incrementPlayCount(song.getId());
            }

            Intent intent = new Intent(getContext(), PlayerActivity.class);
            intent.putExtra("songId", song.getId());
            intent.putExtra("title", song.getTitle());
            intent.putExtra("artist", song.getArtist());
            intent.putExtra("imageUrl", song.getImageUrl());
            intent.putExtra("audioUrl", song.getAudioUrl());
            intent.putExtra("songIndex", position);
            intent.putExtra("isOnline", song.isOnline());
            startActivity(intent);
        } catch (Exception e) {
            Logger.e("Error handling song click: " + e.getMessage());
            ToastUtils.showError(getContext(), "Error playing song");
        }
    }
}