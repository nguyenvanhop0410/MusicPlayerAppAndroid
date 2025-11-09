package com.example.musicapplication.main;

import android.Manifest;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicapplication.R;
import com.example.musicapplication.adapter.SongAdapter;
import com.example.musicapplication.model.Song;
import com.example.musicapplication.player.PlaylistManager;

import java.util.ArrayList;
import java.util.List;

public class SongListFragment extends AppCompatActivity {
    private static final String TAG = "SongListFragment";
    private static final int REQ_PERM = 1001;
    private RecyclerView recyclerView;
    private Button btnRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_songs);
        recyclerView = findViewById(R.id.recycler_songs);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnRefresh = findViewById(R.id.btn_refresh_songs);
        btnRefresh.setOnClickListener(v -> {
            Toast.makeText(this, "Đang quét file nhạc...", Toast.LENGTH_SHORT).show();
            scanMediaFiles();
        });

        if (!hasPermission()) {
            requestPermission();
        } else {
            scanMediaFiles(); // Quét ngay khi mở
        }
    }

    private boolean hasPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_AUDIO}, REQ_PERM);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_PERM);
        }
    }

    private void scanMediaFiles() {
        // Quét thư mục Download và Music
        String[] paths = {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath(),
            "/sdcard/Download",
            "/sdcard/Music"
        };

        MediaScannerConnection.scanFile(this, paths, null, (path, uri) -> {
            Log.d(TAG, "Scanned: " + path + " -> " + uri);
            runOnUiThread(() -> {
                // Sau khi quét xong, load bài hát
                loadSongs();
            });
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
                    MediaStore.Audio.Media.ALBUM_ID  // Add ALBUM_ID to get album art
            };

            // Lấy tất cả file audio, không filter IS_MUSIC
            String selection = null; // Lấy tất cả

            try {
                Cursor cursor = getContentResolver().query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        selection,
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
            runOnUiThread(() -> {
                if (songCount == 0) {
                    Toast.makeText(this, "Không tìm thấy bài hát!\nHãy thêm file MP3 vào Download/Music và nhấn 'Quét lại'", Toast.LENGTH_LONG).show();
                    Log.w(TAG, "No songs loaded - MediaStore is empty");
                } else {
                    Toast.makeText(this, "Tìm thấy " + songCount + " bài hát", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Successfully loaded " + songCount + " songs");
                }

                // Lưu playlist vào PlaylistManager
                PlaylistManager.getInstance().setPlaylist(songs);
                Log.d(TAG, "✅ Playlist saved to PlaylistManager: " + songs.size() + " songs");

                // Verify playlist was saved
                int savedSize = PlaylistManager.getInstance().getPlaylistSize();
                Log.d(TAG, "✅ Verified playlist size in PlaylistManager: " + savedSize);

                recyclerView.setAdapter(new SongAdapter(SongListFragment.this, songs));
            });
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadSongs();
            } else {
                Toast.makeText(this, "Permission is required to load songs", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
