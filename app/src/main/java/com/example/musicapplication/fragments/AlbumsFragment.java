package com.example.musicapplication.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicapplication.R;
import com.example.musicapplication.adapter.AlbumAdapter;
import com.example.musicapplication.data.repository.SongRepository;
import com.example.musicapplication.model.Album;
import com.example.musicapplication.domain.model.Song;
import com.example.musicapplication.main.AlbumDetailActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AlbumsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private AlbumAdapter albumAdapter;
    private List<Album> albumList = new ArrayList<>();
    private SongRepository songRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_albums, container, false);

        songRepository = new SongRepository(getContext());

        initViews(view);
        loadAlbumsFromFirebase();

        return view;
    }

    private void initViews(View view) {
        progressBar = view.findViewById(R.id.progress_bar); // Nhớ thêm ProgressBar vào XML nếu chưa có
        recyclerView = view.findViewById(R.id.recycler_albums);

        // Sử dụng Grid 2 cột
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Khởi tạo Adapter với Listener xử lý sự kiện Click
        albumAdapter = new AlbumAdapter(getContext(), albumList, album -> {
            // Khi click vào Album -> Mở màn hình chi tiết
            Intent intent = new Intent(getContext(), AlbumDetailActivity.class);
            intent.putExtra("albumName", album.getTitle());      // Truyền tên Album
            intent.putExtra("albumImage", album.getCoverUrl());  // Truyền ảnh bìa
            startActivity(intent);
        });

        recyclerView.setAdapter(albumAdapter);
    }

    private void loadAlbumsFromFirebase() {
        setLoading(true);

        // Lấy toàn bộ bài hát về, sau đó lọc ra các Album duy nhất
        songRepository.getAllSongs(new SongRepository.OnResultListener<List<Song>>() {
            @Override
            public void onSuccess(List<Song> songs) {
                albumList.clear();
                Set<String> uniqueAlbumNames = new HashSet<>(); // Dùng Set để tránh trùng tên Album

                for (Song song : songs) {
                    String albumName = song.getAlbum();

                    // Nếu bài hát có tên Album và Album đó chưa có trong danh sách
                    if (albumName != null && !albumName.isEmpty() && !uniqueAlbumNames.contains(albumName)) {
                        uniqueAlbumNames.add(albumName);

                        // Tạo đối tượng Album từ thông tin bài hát
                        // ID, Tên Album, Tên Ca sĩ, Ảnh bìa
                        Album album = new Album(song.getId(), albumName, song.getArtist(), song.getImageUrl());
                        albumList.add(album);
                    }
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        albumAdapter.notifyDataSetChanged();
                        setLoading(false);
                    });
                }
            }

            @Override
            public void onError(Exception error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Lỗi tải Album: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        setLoading(false);
                    });
                }
            }
        });
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }
}