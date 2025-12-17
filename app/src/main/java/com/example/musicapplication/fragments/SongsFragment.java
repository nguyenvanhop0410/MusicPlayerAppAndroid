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

import android.content.Intent;

import com.example.musicapplication.R;
import com.example.musicapplication.adapter.SongAdapter;
import com.example.musicapplication.domain.model.Song;
import com.example.musicapplication.data.repository.SongRepository;
import com.example.musicapplication.data.repository.AuthRepository;
import com.example.musicapplication.main.PlayerActivity;
import com.example.musicapplication.player.PlaylistManager;

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
    
    // Firebase repositories
    private SongRepository songRepository;
    private AuthRepository authRepository;

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
            Toast.makeText(getContext(), "Đang tải lại danh sách nhạc...", Toast.LENGTH_SHORT).show();
            // Reload both Firebase and local songs
            if (authRepository.isLoggedIn()) {
                loadSongsFromFirebase();
            }
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

        // Initialize Firebase repositories
        songRepository = new SongRepository(requireContext());
        authRepository = new AuthRepository(requireContext());
        
        // Load songs from Firebase if user is logged in
        if (authRepository.isLoggedIn()) {
            loadSongsFromFirebase();
        }
        
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

    private void loadSongsFromFirebase() {
        if (songRepository == null) {
            songRepository = new SongRepository(requireContext());
        }
        
        songRepository.getAllSongs(new SongRepository.OnResultListener<List<Song>>() {
            @Override
            public void onSuccess(List<Song> firebaseSongs) {
                Log.d(TAG, "Loaded " + firebaseSongs.size() + " songs from Firebase");
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Merge Firebase songs with local songs
                        mergeSongs(firebaseSongs);
                    });
                }
            }
            
            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error loading songs from Firebase: " + error.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Không thể tải nhạc từ Firebase: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    private void mergeSongs(List<Song> firebaseSongs) {
        // Create a new list starting with Firebase songs
        List<Song> mergedSongs = new ArrayList<>(firebaseSongs);
        
        // Add local songs that are not duplicates (check by title and artist)
        for (Song localSong : allSongs) {
            if (localSong.isLocal()) { // Only add local songs
                boolean isDuplicate = false;
                for (Song firebaseSong : firebaseSongs) {
                    if (localSong.getTitle() != null && firebaseSong.getTitle() != null &&
                        localSong.getTitle().equalsIgnoreCase(firebaseSong.getTitle()) &&
                        localSong.getArtist() != null && firebaseSong.getArtist() != null &&
                        localSong.getArtist().equalsIgnoreCase(firebaseSong.getArtist())) {
                        isDuplicate = true;
                        break;
                    }
                }
                if (!isDuplicate) {
                    mergedSongs.add(localSong);
                }
            }
        }
        
        allSongs = mergedSongs;
        refreshSongList();
        
        int totalCount = allSongs.size();
        int firebaseCount = firebaseSongs.size();
        int localCount = totalCount - firebaseCount;
        
        Log.d(TAG, "Merged songs: " + totalCount + " total (" + firebaseCount + " from Firebase, " + localCount + " local)");
        
        if (totalCount > 0) {
            Toast.makeText(getContext(), 
                "Đã tải " + totalCount + " bài hát (" + firebaseCount + " từ Firebase, " + localCount + " local)", 
                Toast.LENGTH_SHORT).show();
        }
    }
    
    private void mergeSongsWithLocal(List<Song> localSongs) {
        // Add local songs that are not duplicates
        for (Song localSong : localSongs) {
            boolean isDuplicate = false;
            for (Song existingSong : allSongs) {
                if (existingSong.isOnline() && 
                    localSong.getTitle() != null && existingSong.getTitle() != null &&
                    localSong.getTitle().equalsIgnoreCase(existingSong.getTitle()) &&
                    localSong.getArtist() != null && existingSong.getArtist() != null &&
                    localSong.getArtist().equalsIgnoreCase(existingSong.getArtist())) {
                    isDuplicate = true;
                    break;
                }
            }
            if (!isDuplicate) {
                allSongs.add(localSong);
            }
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
                getActivity().runOnUiThread(() -> {
                    loadSongs();
                    // After loading local songs, merge with Firebase songs if available
                    if (authRepository != null && authRepository.isLoggedIn() && songRepository != null) {
                        loadSongsFromFirebase();
                    }
                });
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
                    // Store local songs separately
                    List<Song> localSongs = songs;
                    
                    // If we have Firebase songs, merge them; otherwise just use local songs
                    if (allSongs.isEmpty() || !authRepository.isLoggedIn()) {
                        allSongs = localSongs;
                    } else {
                        // Merge local songs with existing Firebase songs
                        mergeSongsWithLocal(localSongs);
                    }

                    if (songCount == 0 && (allSongs.isEmpty() || !authRepository.isLoggedIn())) {
                        Toast.makeText(getContext(), "Không tìm thấy bài hát!\nHãy thêm file MP3 vào Download/Music và nhấn 'Quét lại'", Toast.LENGTH_LONG).show();
                        Log.w(TAG, "No songs loaded - MediaStore is empty");
                    } else if (songCount > 0) {
                        Log.d(TAG, "Successfully loaded " + songCount + " local songs");
                    }

                    // Apply filter if exists
                    refreshSongList();
                });
            }
        }).start();
    }

    private void refreshSongList() {
        List<Song> songsToShow;
        
        if (filterAlbumId != null) {
            // Filter songs by album
            songsToShow = new ArrayList<>();
            for (Song song : allSongs) {
                if (song.getAlbumId() == filterAlbumId) {
                    songsToShow.add(song);
                }
            }
            Toast.makeText(getContext(), "Tìm thấy " + songsToShow.size() + " bài hát trong album này", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Filtered songs by albumId=" + filterAlbumId + ", count: " + songsToShow.size());
        } else if (filterArtistName != null) {
            // Filter songs by artist
            songsToShow = new ArrayList<>();
            for (Song song : allSongs) {
                if (song.getArtist().equalsIgnoreCase(filterArtistName)) {
                    songsToShow.add(song);
                }
            }
            Toast.makeText(getContext(), "Tìm thấy " + songsToShow.size() + " bài hát của nghệ sĩ này", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Filtered songs by artist=" + filterArtistName + ", count: " + songsToShow.size());
        } else {
            // Show all songs
            songsToShow = allSongs;
            Log.d(TAG, "Showing all songs, count: " + allSongs.size());
        }
        
        // Create adapter with click listener
        songAdapter = new SongAdapter(getContext(), songsToShow, this::onSongClick);
        recyclerView.setAdapter(songAdapter);
    }
    
    private void onSongClick(Song song, int position, List<Song> playlist) {
        // Set playlist to PlaylistManager
        PlaylistManager.getInstance().setPlaylist(playlist, position);
        
        // Increment play count if online song
        if (song.isOnline() && song.getId() != null && songRepository != null) {
            songRepository.incrementPlayCount(song.getId());
        }
        
        // Open player activity
        Intent intent = new Intent(getContext(), PlayerActivity.class);
        intent.putExtra("songId", song.getId());
        intent.putExtra("title", song.getTitle());
        intent.putExtra("artist", song.getArtist());
        intent.putExtra("imageUrl", song.getImageUrl());
        intent.putExtra("audioUrl", song.getAudioUrl());
        intent.putExtra("uri", song.getPlaybackUrl());
        intent.putExtra("songIndex", position);
        intent.putExtra("playlistSize", playlist.size());
        intent.putExtra("isOnline", song.isOnline());
        if (song.getAlbumId() > 0) {
            intent.putExtra("albumId", song.getAlbumId());
        }
        startActivity(intent);
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
