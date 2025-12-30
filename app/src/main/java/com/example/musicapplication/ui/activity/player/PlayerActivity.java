package com.example.musicapplication.ui.activity.player;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.musicapplication.R;
import com.example.musicapplication.data.repository.FavoriteRepository;
import com.example.musicapplication.data.repository.HistoryRepository;
import com.example.musicapplication.data.repository.PlaylistRepository;
import com.example.musicapplication.model.Song;
import com.example.musicapplication.player.MusicPlayer;
import com.example.musicapplication.player.PlaylistManager;
import com.example.musicapplication.ui.activity.player.handlers.PlayerControlHandler;
import com.example.musicapplication.ui.activity.player.handlers.PlayerDownloadHandler;
import com.example.musicapplication.ui.activity.player.handlers.PlayerImageHandler;
import com.example.musicapplication.ui.activity.player.handlers.PlayerLikeHandler;
import com.example.musicapplication.ui.activity.player.handlers.PlayerPlaylistHandler;
import com.example.musicapplication.ui.activity.player.handlers.PlayerSeekBarHandler;
import com.example.musicapplication.ui.activity.player.handlers.PlayerShareHandler;
import com.example.musicapplication.ui.activity.player.handlers.PlayerVolumeHandler;
import com.example.musicapplication.utils.ToastUtils;
import java.util.ArrayList;
import java.util.List;

public class PlayerActivity extends AppCompatActivity {

    private MusicPlayer player;
    private HistoryRepository historyRepository;

    // Khai báo các Handlers
    private PlayerControlHandler controlHandler;
    private PlayerSeekBarHandler seekBarHandler;
    private PlayerImageHandler imageHandler;
    private PlayerLikeHandler likeHandler;
    private PlayerPlaylistHandler playlistHandler;
    private PlayerDownloadHandler downloadHandler;
    private PlayerShareHandler shareHandler;
    private PlayerVolumeHandler volumeHandler;

    private TextView title, artist;
    private ImageButton btnMinimize, btnMenu;

    // Data
    private String currentAudioUrl;
    private String currentSongId;
    private String currentSongTitle;
    private String currentSongArtist;
    private MusicPlayer.OnCompletionListener playerListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        
        initDataAndRepos();
        initViews();
        initHandlers();

        setupPlayer();
        playlistHandler.startListening(); // Bắt đầu lắng nghe playlist
    }

    private void initDataAndRepos() {
        player = MusicPlayer.getInstance(this);
        historyRepository = new HistoryRepository(this);
    }

    private void initViews() {
        title = findViewById(R.id.player_title);
        artist = findViewById(R.id.player_artist);
        btnMinimize = findViewById(R.id.btn_minimize);
        btnMenu = findViewById(R.id.btn_menu);
        title.setSelected(true);

        btnMinimize.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, android.R.anim.slide_out_right);
        });

        btnMenu.setOnClickListener(this::showPopupMenu);
    }

    private void initHandlers() {
        controlHandler = new PlayerControlHandler(this, player);
        seekBarHandler = new PlayerSeekBarHandler(this, player);
        imageHandler = new PlayerImageHandler(this);
        likeHandler = new PlayerLikeHandler(this, new FavoriteRepository(this));
        playlistHandler = new PlayerPlaylistHandler(this, new PlaylistRepository(this));
        downloadHandler = new PlayerDownloadHandler(this);
        volumeHandler = new PlayerVolumeHandler(this);
        shareHandler = new PlayerShareHandler(this);
    }

    private void setupPlayer() {
        // Lấy dữ liệu từ Intent
        currentAudioUrl = getIntent().getStringExtra("audioUrl");
        if (currentAudioUrl == null) currentAudioUrl = getIntent().getStringExtra("uri");
        currentSongId = getIntent().getStringExtra("songId");
        currentSongTitle = getIntent().getStringExtra("title");
        currentSongArtist = getIntent().getStringExtra("artist");
        String imageUrl = getIntent().getStringExtra("imageUrl");
        boolean isOnline = getIntent().getBooleanExtra("isOnline", false);

        // Update Initial UI
        updateSongInfoUI(currentSongTitle, currentSongArtist);
        imageHandler.loadCoverImage(isOnline, imageUrl, currentAudioUrl);
        controlHandler.updateShuffleRepeatUI();
        likeHandler.checkLikeStatus(currentSongId);

        // Setup Playlist in MusicPlayer
        List<Song> playlist = PlaylistManager.getInstance().getPlaylist();
        int songIndex = getIntent().getIntExtra("songIndex", 0);
        if (playlist == null) playlist = new ArrayList<>();
        player.setPlaylist(playlist, songIndex);

        setupPlayerListeners();

        // Logic phát nhạc
        if (currentAudioUrl != null && !currentAudioUrl.isEmpty()) {
            if (currentAudioUrl.equals(player.getCurrentUri()) && player.isPlaying()) {
                updatePlayPauseUI(true);
                seekBarHandler.startUpdating();
            } else {
                playMusic(currentAudioUrl);
            }
        }
    }

    public void playMusic(String url) {
        try {
            player.play(url);
            updatePlayPauseUI(true);
            seekBarHandler.startUpdating();
            if (currentSongId != null) {
                historyRepository.addToHistory(currentSongId, null);
            }
        } catch (Exception e) {
            ToastUtils.showError(this, e.getMessage());
        }
    }

    // Callback từ Player khi chuyển bài
    private void updateUI(Song song) {
        if (song == null) return;
        currentAudioUrl = song.getAudioUrl();
        currentSongId = song.getId();
        currentSongTitle = song.getTitle();
        currentSongArtist = song.getArtist();

        updateSongInfoUI(currentSongTitle, currentSongArtist);
        imageHandler.loadCoverImage(song.isOnline(), song.getImageUrl(), currentAudioUrl);
        updatePlayPauseUI(true);
        likeHandler.checkLikeStatus(currentSongId);

        seekBarHandler.reset();
        seekBarHandler.startUpdating();

        if (currentSongId != null) {
            historyRepository.addToHistory(currentSongId, null);
        }
    }

    private void setupPlayerListeners() {
        playerListener = new MusicPlayer.OnCompletionListener() {
            @Override
            public void onCompletion() {
                if (!player.isRepeatEnabled()) {
                    player.playNext();
                }
            }
            @Override public void onNextSong(Song song) { updateUI(song); }
            @Override public void onPreviousSong(Song song) { updateUI(song); }
        };
        player.addListener(playerListener);
    }

    private void showPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.menu_player_more, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_add_to_playlist) {
                playlistHandler.showAddToPlaylistDialog(currentSongId);
                return true;
            } else if (id == R.id.action_download) {
                downloadHandler.downloadSong(currentAudioUrl, currentSongTitle);
                return true;
            } else if (id == R.id.action_share) {
                shareHandler.shareSong(currentSongTitle, currentSongArtist, currentAudioUrl);
                return true;
            }
            return false;
        });
        popup.show();
    }
    // --- Public methods for Handlers to callback ---
    public void updatePlayPauseUI(boolean isPlaying) {
        controlHandler.updatePlayPauseUI(isPlaying);
        if (isPlaying) {
            seekBarHandler.startUpdating();
        } else {
            seekBarHandler.stopUpdating();
        }
    }

    public void updateSongInfoUI(String titleText, String artistText) {
        title.setText(titleText != null ? titleText : "Unknown");
        artist.setText(artistText != null ? artistText : "Unknown Artist");
    }

    public String getCurrentSongId() { return currentSongId; }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        seekBarHandler.stopUpdating();
        playlistHandler.stopListening();
        if (player != null && playerListener != null) {
            player.removeListener(playerListener);
        }
    }
}