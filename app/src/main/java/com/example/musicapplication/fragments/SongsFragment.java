package com.example.musicapplication.fragments;

import android.Manifest;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicapplication.R;
import com.example.musicapplication.adapter.SongAdapter;
import com.example.musicapplication.model.Song;

import java.util.ArrayList;
import java.util.List;

public class SongsFragment extends Fragment {
    private static final String TAG = "SongsFragment";
    private static final int REQ_PERM = 1001;
    private RecyclerView recyclerView;
    private Button btnRefresh;
    private LinearLayout headerLayout;
    private TextView txtAlbumFilter;
    private Button btnClearFilter;

    // Filter variables
    private Long filterAlbumId = null;
    private String filterAlbumName = null;
    private String filterArtistName = null;
    private List<Song> allSongs = new ArrayList<>();
    private SongAdapter songAdapter;
    private boolean isFilterAppliedFromAlbum = false; // Track if filter came from album click

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_songs, container, false);

        recyclerView = view.findViewById(R.id.recycler_songs);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        headerLayout = view.findViewById(R.id.header_layout);
        txtAlbumFilter = view.findViewById(R.id.txt_album_filter);
        btnClearFilter = view.findViewById(R.id.btn_clear_filter);

        btnRefresh = view.findViewById(R.id.btn_refresh_songs);
        btnRefresh.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Đang quét file nhạc...", Toast.LENGTH_SHORT).show();
            scanMediaFiles();
        });

        // Clear filter button
        btnClearFilter.setOnClickListener(v -> {
            filterAlbumId = null;
            filterAlbumName = null;
            filterArtistName = null;
            headerLayout.setVisibility(View.GONE);
            refreshSongList();
            Toast.makeText(getContext(), "Đã hiển thị tất cả bài hát", Toast.LENGTH_SHORT).show();
        });

        if (!hasPermission()) {
            requestPermission();
        } else {
            scanMediaFiles();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reset filter when user returns to Songs tab (not from album click)
        if (!isFilterAppliedFromAlbum && (filterAlbumId != null || filterArtistName != null)) {
            // Clear filter silently when returning to tab
            filterAlbumId = null;
            filterAlbumName = null;
            filterArtistName = null;
            if (headerLayout != null) {
                headerLayout.setVisibility(View.GONE);
            }
            refreshSongList();
        }
        // Reset the flag after checking
        isFilterAppliedFromAlbum = false;
    }

    // Method to apply album filter from MainActivity
    public void applyAlbumFilter(long albumId, String albumName) {
        this.filterAlbumId = albumId;
        this.filterAlbumName = albumName;
        this.isFilterAppliedFromAlbum = true; // Mark that filter is being applied

        // Show header with album name
        headerLayout.setVisibility(View.VISIBLE);
        txtAlbumFilter.setText("📀 Album: " + albumName);

        // Refresh song list with filter
        refreshSongList();
    }

    // Method mới: lọc theo nghệ sĩ
    public void applyArtistFilter(String artistName, String albumName) {
        this.filterAlbumId = null;
        this.filterAlbumName = albumName;
        this.filterArtistName = artistName;
        this.isFilterAppliedFromAlbum = true; // Mark that filter is being applied

        // Show header with album name
        headerLayout.setVisibility(View.VISIBLE);
        txtAlbumFilter.setText("📀 Album: " + albumName);

        // Refresh song list with filter
        refreshSongList();
    }

    private boolean hasPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.READ_MEDIA_AUDIO}, REQ_PERM);
        } else {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_PERM);
        }
    }

    private void scanMediaFiles() {
        String[] paths = {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath(),
            "/sdcard/Download",
            "/sdcard/Music"
        };

        MediaScannerConnection.scanFile(getContext(), paths, null, (path, uri) -> {
            Log.d(TAG, "Scanned: " + path + " -> " + uri);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> loadSongs());
            }
        });
    }

    private void loadSongs() {
        new Thread(() -> {
            List<Song> songs = new ArrayList<>();
            String[] projection = new String[]{
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.ALBUM_ID // Add ALBUM_ID to get album art
            };

            try {
                Cursor cursor = requireContext().getContentResolver().query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        null,
                        null,
                        MediaStore.Audio.Media.TITLE + " ASC"
                );

                if (cursor != null) {
                    Log.d(TAG, "Total audio files found in MediaStore: " + cursor.getCount());

                    int idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                    int titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                    int artistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                    int dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                    int albumIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idIndex);
                        String title = cursor.getString(titleIndex);
                        String artist = cursor.getString(artistIndex);
                        String path = cursor.getString(dataIndex);
                        long albumId = cursor.getLong(albumIdIndex);
                        String uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString();

                        Log.d(TAG, "Song found: " + title + " | Artist: " + artist + " | AlbumId: " + albumId + " | Path: " + path);
                        songs.add(new Song(id, title, artist, uri, albumId));
                    }
                    cursor.close();
                } else {
                    Log.e(TAG, "Cursor is null - cannot query MediaStore");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading songs: " + e.getMessage());
                e.printStackTrace();
            }

            final int songCount = songs.size();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // Store all songs
                    allSongs = songs;

                    if (songCount == 0) {
                        Toast.makeText(getContext(), "Không tìm thấy bài hát!\nHãy thêm file MP3 vào Download/Music và nhấn 'Quét lại'", Toast.LENGTH_LONG).show();
                        Log.w(TAG, "No songs loaded - MediaStore is empty");
                    } else {
                        Toast.makeText(getContext(), "Tìm thấy " + songCount + " bài hát", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Successfully loaded " + songCount + " songs");
                    }

                    // Apply filter if exists
                    refreshSongList();
                });
            }
        }).start();
    }

    private void refreshSongList() {
        if (filterAlbumId != null) {
            // Filter songs by album
            List<Song> filteredSongs = new ArrayList<>();
            for (Song song : allSongs) {
                if (song.getAlbumId() == filterAlbumId) {
                    filteredSongs.add(song);
                }
            }

            songAdapter = new SongAdapter(getContext(), filteredSongs);
            recyclerView.setAdapter(songAdapter);

            Toast.makeText(getContext(), "Tìm thấy " + filteredSongs.size() + " bài hát trong album này", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Filtered songs by albumId=" + filterAlbumId + ", count: " + filteredSongs.size());
        } else if (filterArtistName != null) {
            // Filter songs by artist
            List<Song> filteredSongs = new ArrayList<>();
            for (Song song : allSongs) {
                if (song.getArtist().equalsIgnoreCase(filterArtistName)) {
                    filteredSongs.add(song);
                }
            }

            songAdapter = new SongAdapter(getContext(), filteredSongs);
            recyclerView.setAdapter(songAdapter);

            Toast.makeText(getContext(), "Tìm thấy " + filteredSongs.size() + " bài hát của nghệ sĩ này", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Filtered songs by artist=" + filterArtistName + ", count: " + filteredSongs.size());
        } else {
            // Show all songs
            songAdapter = new SongAdapter(getContext(), allSongs);
            recyclerView.setAdapter(songAdapter);
            Log.d(TAG, "Showing all songs, count: " + allSongs.size());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadSongs();
            } else {
                Toast.makeText(getContext(), "Permission is required to load songs", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
