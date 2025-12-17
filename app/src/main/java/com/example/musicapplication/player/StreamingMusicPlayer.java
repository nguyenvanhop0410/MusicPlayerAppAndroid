package com.example.musicapplication.player;

import android.content.Context;
import android.util.Log;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import com.example.musicapplication.domain.model.Song;
import java.util.ArrayList;
import java.util.List;

public class StreamingMusicPlayer {
    private static final String TAG = "StreamingMusicPlayer";
    private static StreamingMusicPlayer instance;
    private ExoPlayer player;
    private Song currentSong;
    private List<Song> playlist;
    private int currentIndex = -1;
    private Player.Listener playerListener;
    
    private StreamingMusicPlayer(Context context) {
        player = new ExoPlayer.Builder(context).build();
        setupPlayerListener();
    }
    
    public static synchronized StreamingMusicPlayer getInstance(Context context) {
        if (instance == null) {
            instance = new StreamingMusicPlayer(context.getApplicationContext());
        }
        return instance;
    }
    
    private void setupPlayerListener() {
        playerListener = new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    // Auto play next song
                    if (playlist != null && !playlist.isEmpty()) {
                        playNext();
                    }
                }
            }
        };
        player.addListener(playerListener);
    }
    
    // Phát bài hát từ URL
    public void playSong(Song song) {
        if (song == null) {
            Log.e(TAG, "Song is null");
            return;
        }
        
        String url = song.getPlaybackUrl();
        if (url == null || url.isEmpty()) {
            Log.e(TAG, "Song URL is null or empty");
            return;
        }
        
        currentSong = song;
        Log.d(TAG, "Playing song: " + song.getTitle() + " from URL: " + url);
        
        MediaItem mediaItem = MediaItem.fromUri(url);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }
    
    // Phát playlist
    public void playPlaylist(List<Song> songs, int startIndex) {
        if (songs == null || songs.isEmpty()) {
            Log.e(TAG, "Playlist is null or empty");
            return;
        }
        
        if (startIndex < 0 || startIndex >= songs.size()) {
            Log.e(TAG, "Invalid start index: " + startIndex);
            return;
        }
        
        playlist = new ArrayList<>(songs);
        currentIndex = startIndex;
        
        List<MediaItem> mediaItems = new ArrayList<>();
        for (Song song : songs) {
            String url = song.getPlaybackUrl();
            if (url != null && !url.isEmpty()) {
                mediaItems.add(MediaItem.fromUri(url));
            }
        }
        
        if (mediaItems.isEmpty()) {
            Log.e(TAG, "No valid media items in playlist");
            return;
        }
        
        player.setMediaItems(mediaItems);
        player.seekTo(startIndex, 0);
        player.prepare();
        player.play();
        
        currentSong = songs.get(startIndex);
        Log.d(TAG, "Playing playlist with " + songs.size() + " songs, starting at index " + startIndex);
    }
    
    public void pause() {
        if (player != null && player.isPlaying()) {
            player.pause();
            Log.d(TAG, "Playback paused");
        }
    }
    
    public void resume() {
        if (player != null && !player.isPlaying()) {
            player.play();
            Log.d(TAG, "Playback resumed");
        }
    }
    
    public void stop() {
        if (player != null) {
            player.stop();
            player.clearMediaItems();
            currentSong = null;
            currentIndex = -1;
            Log.d(TAG, "Playback stopped");
        }
    }
    
    public void seekTo(long position) {
        if (player != null) {
            player.seekTo(position);
        }
    }
    
    public long getCurrentPosition() {
        return player != null ? player.getCurrentPosition() : 0;
    }
    
    public long getDuration() {
        return player != null ? player.getDuration() : 0;
    }
    
    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }
    
    public ExoPlayer getPlayer() {
        return player;
    }
    
    public Song getCurrentSong() {
        return currentSong;
    }
    
    public int getCurrentIndex() {
        return currentIndex;
    }
    
    public void playNext() {
        if (playlist == null || playlist.isEmpty()) {
            Log.d(TAG, "Playlist is empty, cannot play next");
            return;
        }
        
        currentIndex++;
        if (currentIndex >= playlist.size()) {
            currentIndex = 0; // Loop back to start
        }
        
        player.seekToNext();
        currentSong = playlist.get(currentIndex);
        Log.d(TAG, "Playing next song: " + currentSong.getTitle());
    }
    
    public void playPrevious() {
        if (playlist == null || playlist.isEmpty()) {
            Log.d(TAG, "Playlist is empty, cannot play previous");
            return;
        }
        
        currentIndex--;
        if (currentIndex < 0) {
            currentIndex = playlist.size() - 1; // Loop to end
        }
        
        player.seekToPrevious();
        currentSong = playlist.get(currentIndex);
        Log.d(TAG, "Playing previous song: " + currentSong.getTitle());
    }
    
    public void setPlaylist(List<Song> songs, int currentIndex) {
        this.playlist = songs != null ? new ArrayList<>(songs) : new ArrayList<>();
        this.currentIndex = currentIndex;
    }
    
    public List<Song> getPlaylist() {
        return playlist != null ? new ArrayList<>(playlist) : new ArrayList<>();
    }
    
    public void release() {
        if (player != null) {
            player.removeListener(playerListener);
            player.release();
            player = null;
            instance = null;
        }
    }
}

