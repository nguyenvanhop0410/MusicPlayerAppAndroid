package com.example.musicapplication.main;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.musicapplication.player.MusicPlayer;
import com.example.musicapplication.player.PlaylistManager;
import com.example.musicapplication.model.Song;
import com.example.musicapplication.R;

import java.util.List;

public class PlayerFragment extends AppCompatActivity {
    private MusicPlayer player;
    private String uri;
    private ImageView albumArt;
    private TextView title, artist, currentTime, totalTime;
    private ImageButton btnPlayPause, btnPrevious, btnNext, btnBack, btnRepeat, btnStop;
    private SeekBar seekBar;
    private Handler handler = new Handler();
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // Initialize views
        albumArt = findViewById(R.id.player_album_art);
        title = findViewById(R.id.player_title);
        artist = findViewById(R.id.player_artist);
        currentTime = findViewById(R.id.txt_current_time);
        totalTime = findViewById(R.id.txt_total_time);
        seekBar = findViewById(R.id.seek_bar);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnPrevious = findViewById(R.id.btn_previous);
        btnNext = findViewById(R.id.btn_next);
        btnBack = findViewById(R.id.btn_back);
        btnRepeat = findViewById(R.id.btn_repeat);
        btnStop = findViewById(R.id.btn_stop);

        // Get data from intent
        uri = getIntent().getStringExtra("uri");
        String songTitle = getIntent().getStringExtra("title");
        String songArtist = getIntent().getStringExtra("artist");
        long albumId = getIntent().getLongExtra("albumId", -1);

        // Set song info
        title.setText(songTitle != null ? songTitle : "Unknown");
        artist.setText(songArtist != null ? songArtist : "Unknown Artist");

        // Enable marquee for long titles
        title.setSelected(true);

        // Load album art from MP3 metadata
        loadAlbumArtFromMetadata(uri);

        player = MusicPlayer.getInstance(this);

        // Get playlist from PlaylistManager and set it to player
        List<Song> playlist = PlaylistManager.getInstance().getPlaylist();
        int songIndex = getIntent().getIntExtra("songIndex", 0);

        if (playlist == null || playlist.isEmpty()) {
            Log.e("PlayerFragment", "❌ PLAYLIST IS EMPTY! Cannot use Next/Previous");
            playlist = new java.util.ArrayList<>(); // Tạo list rỗng để tránh lỗi
        } else {
            Log.d("PlayerFragment", "✅ Playlist loaded: " + playlist.size() + " songs, current index: " + songIndex);
            // Log all songs in playlist
            for (int i = 0; i < playlist.size(); i++) {
                Log.d("PlayerFragment", "  Song[" + i + "]: " + playlist.get(i).title);
            }
        }

        final List<Song> finalPlaylist = playlist; // Tạo biến final để sử dụng trong lambda
        player.setPlaylist(playlist, songIndex);
        Log.d("PlayerFragment", "Playlist loaded: " + playlist.size() + " songs, current index: " + songIndex);

        // Set completion listener for repeat functionality
        player.setOnCompletionListener(new MusicPlayer.OnCompletionListener() {
            @Override
            public void onCompletion() {
                if (player.isRepeatEnabled()) {
                    // Repeat is enabled, play the song again
                    Log.d("PlayerFragment", "Repeat enabled, replaying song");
                    player.play(uri);
                    isPlaying = true;
                    btnPlayPause.setImageResource(R.drawable.ic_pause);

                    // Update seekbar after a short delay
                    handler.postDelayed(() -> {
                        int duration = player.getDuration();
                        if (duration > 0) {
                            seekBar.setMax(duration);
                            totalTime.setText(formatTime(duration));
                            updateSeekBar();
                        }
                    }, 500);

                    Toast.makeText(PlayerFragment.this, "🔁 Phát lại bài hát", Toast.LENGTH_SHORT).show();
                } else if (finalPlaylist != null && !finalPlaylist.isEmpty()) {
                    // Auto play next song
                    player.playNext();
                } else {
                    // Song finished, update UI
                    isPlaying = false;
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                    seekBar.setProgress(0);
                    currentTime.setText("0:00");
                    handler.removeCallbacks(updateSeekBarRunnable);
                    Toast.makeText(PlayerFragment.this, "✅ Phát xong bài hát", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNextSong(Song song) {
                updateSongUI(song);
            }

            @Override
            public void onPreviousSong(Song song) {
                updateSongUI(song);
            }
        });

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Play/Pause button
        btnPlayPause.setOnClickListener(v -> {
            if (isPlaying) {
                player.pause();
                isPlaying = false;
                btnPlayPause.setImageResource(R.drawable.ic_play);
            } else {
                player.play(uri);
                isPlaying = true;
                btnPlayPause.setImageResource(R.drawable.ic_pause);
                updateSeekBar();
            }
        });

        // Previous button
        btnPrevious.setOnClickListener(v -> {
            player.playPrevious();
            isPlaying = true;
            btnPlayPause.setImageResource(R.drawable.ic_pause);

            // Wait for song to load, then update UI
            handler.postDelayed(() -> {
                int duration = player.getDuration();
                if (duration > 0) {
                    seekBar.setMax(duration);
                    totalTime.setText(formatTime(duration));
                    updateSeekBar();
                }
            }, 500);
        });

        // Next button
        btnNext.setOnClickListener(v -> {
            Log.d("PlayerFragment", "🔵 Next button clicked");
            Log.d("PlayerFragment", "🔵 Playlist size: " + PlaylistManager.getInstance().getPlaylistSize());

            player.playNext();
            isPlaying = true;
            btnPlayPause.setImageResource(R.drawable.ic_pause);

            // Wait for song to load, then update UI
            handler.postDelayed(() -> {
                int duration = player.getDuration();
                Log.d("PlayerFragment", "🔵 Song duration after Next: " + duration);
                if (duration > 0) {
                    seekBar.setMax(duration);
                    totalTime.setText(formatTime(duration));
                    updateSeekBar();
                }
            }, 500);
        });

        // SeekBar change listener
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    player.seekTo(progress);
                    currentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updateSeekBarRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updateSeekBar();
            }
        });

        // Auto play when activity starts
        if (uri != null) {
            player.play(uri);
            isPlaying = true;
            btnPlayPause.setImageResource(R.drawable.ic_pause);

            // Wait a bit for MediaPlayer to prepare, then update seekbar
            handler.postDelayed(() -> {
                int duration = player.getDuration();
                if (duration > 0) {
                    seekBar.setMax(duration);
                    totalTime.setText(formatTime(duration));
                    updateSeekBar();
                }
            }, 500);
        }

        // Repeat button
        btnRepeat.setOnClickListener(v -> {
            boolean repeatEnabled = !player.isRepeatEnabled();
            player.setRepeatEnabled(repeatEnabled);

            if (repeatEnabled) {
                btnRepeat.setAlpha(1.0f);
                btnRepeat.setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
                Toast.makeText(this, "🔁 Repeat: BẬT", Toast.LENGTH_SHORT).show();
            } else {
                btnRepeat.setAlpha(0.5f);
                btnRepeat.setColorFilter(null);
                Toast.makeText(this, "🔁 Repeat: TẮT", Toast.LENGTH_SHORT).show();
            }
        });

        // Stop button - Dừng nhạc và quay về trang Songs
        btnStop.setOnClickListener(v -> {
            // Dừng nhạc hoàn toàn
            player.stop();
            isPlaying = false;

            // Dừng update seekbar
            handler.removeCallbacks(updateSeekBarRunnable);

            // Reset UI
            seekBar.setProgress(0);
            currentTime.setText("0:00");
            btnPlayPause.setImageResource(R.drawable.ic_play);

            // Hiển thị thông báo
            Toast.makeText(this, "⏹️ Đã dừng phát nhạc", Toast.LENGTH_SHORT).show();

            // Đóng activity và quay về trang trước (Songs)
            finish();
        });

        // Initialize button states
        updateRepeatUI();
    }

    private Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPlaying) {
                int current = player.getCurrentPosition();
                int duration = player.getDuration();

                if (duration > 0) {
                    seekBar.setMax(duration);
                    seekBar.setProgress(current);
                    currentTime.setText(formatTime(current));
                    totalTime.setText(formatTime(duration));
                }

                handler.postDelayed(this, 100);
            }
        }
    };

    private void updateSeekBar() {
        handler.postDelayed(updateSeekBarRunnable, 100);
    }

    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateSeekBarRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Don't stop music when activity pauses, let it play in background
    }

    private void updateRepeatUI() {
        // Update repeat button
        if (player.isRepeatEnabled()) {
            btnRepeat.setAlpha(1.0f);
            btnRepeat.setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
        } else {
            btnRepeat.setAlpha(0.5f);
            btnRepeat.setColorFilter(null);
        }
    }

    private void updateSongUI(Song song) {
        // Update song info
        title.setText(song.getTitle());
        artist.setText(song.getArtist());

        // Load album art from MP3 metadata
        loadAlbumArtFromMetadata(song.uri);

        // Update seekbar and time
        int duration = player.getDuration();
        seekBar.setMax(duration);
        totalTime.setText(formatTime(duration));
        currentTime.setText("0:00");

        updateSeekBar();
    }

    /**
     * Load album art directly from MP3 file metadata using MediaMetadataRetriever
     */
    private void loadAlbumArtFromMetadata(String songUri) {
        new Thread(() -> {
            Bitmap albumArtBitmap = null;
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();

            try {
                retriever.setDataSource(this, Uri.parse(songUri));
                byte[] art = retriever.getEmbeddedPicture();

                if (art != null) {
                    albumArtBitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
                }
                retriever.release();
            } catch (Exception e) {
                Log.e("PlayerFragment", "Error loading album art: " + e.getMessage());
                e.printStackTrace();
                try {
                    retriever.release();
                } catch (Exception ignored) {}
            }

            final Bitmap finalAlbumArt = albumArtBitmap;
            runOnUiThread(() -> {
                if (finalAlbumArt != null) {
                    Glide.with(this)
                            .load(finalAlbumArt)
                            .centerCrop()
                            .into(albumArt);
                } else {
                    albumArt.setImageResource(R.drawable.ic_music);
                }
            });
        }).start();
    }
}
