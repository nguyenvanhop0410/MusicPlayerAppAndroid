package com.example.musicapplication.player;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.example.musicapplication.model.Song;

import java.io.IOException;
import java.util.List;

public class MusicPlayer {
    private static final String TAG = "MusicPlayer";
    private static MusicPlayer instance;
    private final MediaPlayer mediaPlayer;
    private String currentUri;
    private final Context ctx;
    private AudioManager audioManager;
    private boolean isRepeatEnabled = false;
    private OnCompletionListener onCompletionListener;
    private List<Song> playlist;
    private int currentSongIndex = -1;
    private boolean isPreparing = false; // Thêm flag để tránh double-call

    // Interface for completion callback
    public interface OnCompletionListener {
        void onCompletion();
        void onNextSong(Song song);
        void onPreviousSong(Song song);
    }

    private MusicPlayer(Context context) {
        ctx = context.getApplicationContext();
        mediaPlayer = new MediaPlayer();
        audioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);

        // Set audio attributes for music playback
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            mediaPlayer.setAudioAttributes(audioAttributes);
        } else {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }

        Log.d(TAG, "MusicPlayer initialized with audio attributes");
    }

    public static synchronized MusicPlayer getInstance(Context context) {
        if (instance == null) instance = new MusicPlayer(context);
        return instance;
    }

    public void play(String uri) {
        try {
            if (uri == null) {
                Log.e(TAG, "URI is null, cannot play");
                return;
            }

            // Nếu đang chuẩn bị bài khác, bỏ qua
            if (isPreparing) {
                Log.d(TAG, "Already preparing another song, skipping...");
                return;
            }

            // Kiểm tra volume chỉ lần đầu
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

            if (currentVolume == 0) {
                // Tự động set volume về 50%
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume / 2, AudioManager.FLAG_SHOW_UI);
            }

            Log.d(TAG, "Attempting to play: " + uri);

            if (uri.equals(currentUri) && mediaPlayer.isPlaying()) {
                Log.d(TAG, "Already playing this song");
                return;
            }

            isPreparing = true; // Đánh dấu đang chuẩn bị
            mediaPlayer.reset();

            // Set lại audio attributes sau reset
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                mediaPlayer.setAudioAttributes(audioAttributes);
            } else {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }

            currentUri = uri;

            Uri audioUri = Uri.parse(uri);
            Log.d(TAG, "Setting data source: " + audioUri);
            mediaPlayer.setDataSource(ctx, audioUri);

            // Dùng prepareAsync để không block UI thread
            mediaPlayer.setOnPreparedListener(mp -> {
                isPreparing = false; // Reset flag
                Log.d(TAG, "MediaPlayer prepared, starting playback");
                Log.d(TAG, "MediaPlayer duration: " + mp.getDuration() + "ms");
                mp.setVolume(1.0f, 1.0f); // Set volume to max
                mp.start();
                Log.d(TAG, "MediaPlayer started. isPlaying: " + mp.isPlaying());
                Toast.makeText(ctx, "🎵 Đang phát nhạc...", Toast.LENGTH_SHORT).show();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                isPreparing = false; // Reset flag khi có lỗi
                String errorMsg = "Unknown error";
                if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
                    errorMsg = "Media server died";
                } else if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN) {
                    errorMsg = "Unknown media error";
                }
                Log.e(TAG, "MediaPlayer error - what: " + what + " (" + errorMsg + "), extra: " + extra);
                // Chỉ hiện toast lỗi nghiêm trọng
                if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
                    Toast.makeText(ctx, "❌ Lỗi phát nhạc nghiêm trọng", Toast.LENGTH_SHORT).show();
                }
                return true; // Đã xử lý lỗi
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "Playback completed");
                if (onCompletionListener != null) {
                    onCompletionListener.onCompletion();
                }
            });

            mediaPlayer.prepareAsync();
            Log.d(TAG, "PrepareAsync called");

        } catch (IOException e) {
            isPreparing = false;
            Log.e(TAG, "IOException while playing music: " + e.getMessage(), e);
            Toast.makeText(ctx, "❌ Không thể mở file nhạc", Toast.LENGTH_SHORT).show();
        } catch (IllegalStateException e) {
            isPreparing = false;
            Log.e(TAG, "IllegalStateException: " + e.getMessage(), e);
            // Không hiện toast vì thường tự recover được
        } catch (Exception e) {
            isPreparing = false;
            Log.e(TAG, "Unexpected error: " + e.getMessage(), e);
        }
    }

    public void pause() {
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                Log.d(TAG, "Paused");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error pausing", e);
        }
    }

    public void resume() {
        try {
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                Log.d(TAG, "Resumed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resuming", e);
        }
    }

    public void stop() {
        try {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.reset();
            currentUri = null;
            Log.d(TAG, "Stopped");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping", e);
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public String getCurrentUri() {
        return currentUri;
    }

    public int getCurrentPosition() {
        try {
            if (mediaPlayer != null) {
                return mediaPlayer.getCurrentPosition();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting current position", e);
        }
        return 0;
    }

    public int getDuration() {
        try {
            if (mediaPlayer != null) {
                return mediaPlayer.getDuration();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting duration", e);
        }
        return 0;
    }

    public void seekTo(int position) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.seekTo(position);
                Log.d(TAG, "Seeked to position: " + position);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error seeking", e);
        }
    }

    public void setRepeatEnabled(boolean repeatEnabled) {
        isRepeatEnabled = repeatEnabled;
        Log.d(TAG, "Repeat enabled: " + isRepeatEnabled);
    }

    public boolean isRepeatEnabled() {
        return isRepeatEnabled;
    }

    public void setOnCompletionListener(OnCompletionListener listener) {
        this.onCompletionListener = listener;
    }

    public void setPlaylist(List<Song> playlist) {
        this.playlist = playlist;
        Log.d(TAG, "Playlist set with " + playlist.size() + " songs");
    }

    public void setPlaylist(List<Song> playlist, int currentIndex) {
        this.playlist = playlist;
        this.currentSongIndex = currentIndex;
        Log.d(TAG, "Playlist set with " + playlist.size() + " songs, current index: " + currentIndex);
    }

    public void playNext() {
        if (playlist == null || playlist.isEmpty()) {
            Log.d(TAG, "❌ Playlist is empty, cannot play next");
            Toast.makeText(ctx, "❌ Playlist trống!", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "🔵 playNext() called - Current index: " + currentSongIndex);
        Log.d(TAG, "🔵 Playlist size: " + playlist.size());

        // Chuyển sang bài tiếp theo
        currentSongIndex++;

        // Nếu vượt qua bài cuối cùng, quay về bài đầu tiên
        if (currentSongIndex >= playlist.size()) {
            currentSongIndex = 0;
            Log.d(TAG, "🔁 Reached end of playlist, going back to first song");
        }

        Log.d(TAG, "▶️ Playing next song at index " + currentSongIndex);

        Song nextSong = playlist.get(currentSongIndex);
        Log.d(TAG, "🎵 Playing next song: " + nextSong.title + " (URI: " + nextSong.uri + ")");
        play(nextSong.uri);

        if (onCompletionListener != null) {
            Log.d(TAG, "📢 Calling onNextSong callback");
            onCompletionListener.onNextSong(nextSong);
        }
    }

    public void playPrevious() {
        if (playlist == null || playlist.isEmpty()) {
            Log.d(TAG, "❌ Playlist is empty, cannot play previous");
            Toast.makeText(ctx, "❌ Playlist trống!", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "🔵 playPrevious() called - Current index: " + currentSongIndex);
        Log.d(TAG, "🔵 Playlist size: " + playlist.size());

        // Chuyển sang bài trước đó
        currentSongIndex--;

        // Nếu lùi về trước bài đầu tiên, quay về bài cuối cùng
        if (currentSongIndex < 0) {
            currentSongIndex = playlist.size() - 1;
            Log.d(TAG, "🔁 Reached beginning of playlist, going to last song");
        }

        Log.d(TAG, "▶️ Playing previous song at index " + currentSongIndex);

        Song previousSong = playlist.get(currentSongIndex);
        Log.d(TAG, "🎵 Playing previous song: " + previousSong.title + " (URI: " + previousSong.uri + ")");
        play(previousSong.uri);

        if (onCompletionListener != null) {
            Log.d(TAG, "📢 Calling onPreviousSong callback");
            onCompletionListener.onPreviousSong(previousSong);
        }
    }

    public int getCurrentSongIndex() {
        return currentSongIndex;
    }

    public Song getCurrentSong() {
        if (playlist != null && currentSongIndex >= 0 && currentSongIndex < playlist.size()) {
            return playlist.get(currentSongIndex);
        }
        return null;
    }

    public void setCurrentSongIndex(int index) {
        this.currentSongIndex = index;
    }
}
