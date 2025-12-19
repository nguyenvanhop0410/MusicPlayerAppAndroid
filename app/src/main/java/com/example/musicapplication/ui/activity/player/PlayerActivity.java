package com.example.musicapplication.ui.activity.player;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText; // Thêm import EditText
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.palette.graphics.Palette;
import com.example.musicapplication.R;
import com.example.musicapplication.data.repository.FavoriteRepository;
import com.example.musicapplication.data.repository.HistoryRepository;
import com.example.musicapplication.data.repository.PlaylistRepository; // Import PlaylistRepository
import com.example.musicapplication.data.repository.SongRepository;
import com.example.musicapplication.model.Song;
import com.example.musicapplication.model.Playlist; // Import Playlist Model
import com.example.musicapplication.player.MusicPlayer;
import com.example.musicapplication.player.PlaylistManager;
import com.example.musicapplication.utils.ImageLoader;
import com.example.musicapplication.utils.ToastUtils;
import com.example.musicapplication.utils.TimeFormatter;
import com.example.musicapplication.utils.Logger;
import com.google.firebase.firestore.ListenerRegistration; // Import ListenerRegistration

import java.util.ArrayList;
import java.util.List;

public class PlayerActivity extends AppCompatActivity {

    private MusicPlayer player;
    private SongRepository songRepository;
    private FavoriteRepository favoriteRepository;
    private HistoryRepository historyRepository;
    private PlaylistRepository playlistRepository; // Khai báo PlaylistRepository

    private String currentAudioUrl;
    private String currentSongId;
    private String currentSongTitle;
    private String currentSongArtist;
    private MusicPlayer.OnCompletionListener playerListener;

    private ImageView albumArt;
    private TextView title, artist, currentTime, totalTime;
    private ImageButton btnShuffle, btnPrevious, btnNext, btnMinimize, btnMenu, btnPlayPause, btnRepeat, btnLike;
    private SeekBar seekBar;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isPlaying = false;
    private boolean isLiked = false;
    private View playerRoot;

    // Biến quản lý Playlist
    private List<Playlist> userPlaylists = new ArrayList<>();
    private ListenerRegistration playlistListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        songRepository = new SongRepository(this);
        favoriteRepository = new FavoriteRepository(this);
        historyRepository = new HistoryRepository(this);
        playlistRepository = new PlaylistRepository(this); // Khởi tạo PlaylistRepository

        initViews();
        setupPlayer();
        // Bắt đầu lắng nghe danh sách Playlist của User
        startListeningToPlaylists();
    }

    private void initViews() {
        albumArt = findViewById(R.id.player_album_art);
        title = findViewById(R.id.player_title);
        artist = findViewById(R.id.player_artist);
        currentTime = findViewById(R.id.txt_current_time);
        totalTime = findViewById(R.id.txt_total_time);
        seekBar = findViewById(R.id.seek_bar);

        btnShuffle = findViewById(R.id.btn_shuffle);
        btnPrevious = findViewById(R.id.btn_previous);
        btnNext = findViewById(R.id.btn_next);

        btnMinimize = findViewById(R.id.btn_minimize);
        btnMenu = findViewById(R.id.btn_menu);

        btnPlayPause = findViewById(R.id.btn_play);
        btnRepeat = findViewById(R.id.btn_repeat);
        btnLike = findViewById(R.id.btn_like);
        playerRoot = findViewById(R.id.player_root);

        title.setSelected(true);
    }

    // --- HÀM MỚI: Lắng nghe danh sách Playlist ---
    private void startListeningToPlaylists() {
        playlistListener = playlistRepository.getRealtimeUserPlaylists(new PlaylistRepository.OnResultListener<List<Playlist>>() {
            @Override
            public void onSuccess(List<Playlist> result) {
                userPlaylists.clear();
                userPlaylists.addAll(result);
            }

            @Override
            public void onError(Exception error) {
                // Có thể log lỗi nếu cần thiết
            }
        });
    }

    private void setupPlayer() {
        player = MusicPlayer.getInstance(this);

        currentAudioUrl = getIntent().getStringExtra("audioUrl");
        if (currentAudioUrl == null) currentAudioUrl = getIntent().getStringExtra("uri");

        currentSongId = getIntent().getStringExtra("songId");
        currentSongTitle = getIntent().getStringExtra("title");
        currentSongArtist = getIntent().getStringExtra("artist");

        String imageUrl = getIntent().getStringExtra("imageUrl");
        boolean isOnline = getIntent().getBooleanExtra("isOnline", false);

        title.setText(currentSongTitle != null ? currentSongTitle : "Unknown");
        artist.setText(currentSongArtist != null ? currentSongArtist : "Unknown Artist");
        loadCoverImage(isOnline, imageUrl, currentAudioUrl);
        updateShuffleRepeatUI();
        checkLikeStatus();
        List<Song> playlist = PlaylistManager.getInstance().getPlaylist();
        int songIndex = getIntent().getIntExtra("songIndex", 0);
        if (playlist == null) playlist = new ArrayList<>();
        player.setPlaylist(playlist, songIndex);

        setupClickListeners();
        setupPlayerListeners();

        if (currentAudioUrl != null && !currentAudioUrl.isEmpty()) {
            if (currentAudioUrl.equals(player.getCurrentUri()) && player.isPlaying()) {
                isPlaying = true;
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                updateSeekBar();
            } else {
                playMusic(currentAudioUrl);
            }
        }
    }

    private void playMusic(String url) {
        try {
            player.play(url);
            isPlaying = true;
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            updateSeekBar();
            if (currentSongId != null) {
                historyRepository.addToHistory(currentSongId, null);
            }
        } catch (Exception e) {
            ToastUtils.showError(this, e.getMessage());
        }
    }

    private void updateUI(Song song) {
        if (song == null) return;
        currentAudioUrl = song.getAudioUrl();
        currentSongId = song.getId();
        currentSongTitle = song.getTitle();
        currentSongArtist = song.getArtist();

        title.setText(song.getTitle());
        artist.setText(song.getArtist());
        loadCoverImage(song.isOnline(), song.getImageUrl(), currentAudioUrl);

        isPlaying = true;
        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);

        isLiked = false;
        updateLikeUI();
        checkLikeStatus();

        currentTime.setText("0:00");
        updateSeekBar();

        if (currentSongId != null) {
            historyRepository.addToHistory(currentSongId, null);
        }
    }
    private void checkLikeStatus() {
        if (currentSongId == null) return;

        favoriteRepository.checkIsLiked(currentSongId, new FavoriteRepository.OnResultListener<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                isLiked = result;
                updateLikeUI();
            }

            @Override
            public void onError(Exception error) {
                ToastUtils.showError(PlayerActivity.this, "Lỗi kết nối!");
            }
        });
    }
    private void setupClickListeners() {
        if (btnMinimize != null) {
            btnMinimize.setOnClickListener(v -> {
                finish();
                overridePendingTransition(0, android.R.anim.slide_out_right);
            });
        }

        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> showPopupMenu());
        }

        btnPlayPause.setOnClickListener(v -> {
            if (player.isPlaying()) {
                player.pause();
                isPlaying = false;
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
            } else {
                if (currentAudioUrl != null) {
                    if (currentAudioUrl.equals(player.getCurrentUri())) {
                        player.resume();
                    } else {
                        playMusic(currentAudioUrl);
                    }
                    isPlaying = true;
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                    updateSeekBar();
                }
            }
        });

        // ... (Giữ nguyên các listener khác: Next, Prev, Shuffle, Repeat, Like) ...
        btnNext.setOnClickListener(v -> player.playNext());
        btnPrevious.setOnClickListener(v -> player.playPrevious());

        btnShuffle.setOnClickListener(v -> {
            boolean newState = !player.isShuffleEnabled();
            player.setShuffleEnabled(newState);
            updateShuffleRepeatUI();
            ToastUtils.showInfo(this, newState ? "Trộn bài: BẬT" : "Trộn bài: TẮT");
        });

        btnRepeat.setOnClickListener(v -> {
            boolean newState = !player.isRepeatEnabled();
            player.setRepeatEnabled(newState);
            updateShuffleRepeatUI();
            ToastUtils.showInfo(this, newState ? "Lặp lại: BẬT" : "Lặp lại: TẮT");
        });

        btnLike.setOnClickListener(v -> {
            if (currentSongId == null) return;
            isLiked = !isLiked;
            updateLikeUI();

            favoriteRepository.updateFavorite(currentSongId, isLiked, new FavoriteRepository.OnResultListener<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {}
                @Override
                public void onError(Exception error) {
                    isLiked = !isLiked;
                    updateLikeUI();
                    ToastUtils.showError(PlayerActivity.this, "Lỗi kết nối!");
                }
            });
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    player.seekTo(progress);
                    currentTime.setText(TimeFormatter.formatDuration(progress));
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
    }

    private void showPopupMenu() {
        PopupMenu popup = new PopupMenu(this, btnMenu);
        popup.getMenuInflater().inflate(R.menu.menu_player_more, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.action_add_to_playlist) {
                    showAddToPlaylistDialog(); // Gọi hàm dialog mới
                    return true;
                } else if (id == R.id.action_download) {
                    downloadSong();
                    return true;
                } else if (id == R.id.action_share) {
                    shareSong();
                    return true;
                }
                return false;
            }
        });
        popup.show();
    }

    // --- CẬP NHẬT: Hàm hiển thị Dialog chọn Playlist ---
    private void showAddToPlaylistDialog() {
        if (currentSongId == null) {
            ToastUtils.showWarning(this, "Không thể thêm bài hát này");
            return;
        }

        // Tạo danh sách tên playlist để hiển thị
        String[] playlistNames = new String[userPlaylists.size()];
        for (int i = 0; i < userPlaylists.size(); i++) {
            playlistNames[i] = userPlaylists.get(i).getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn Playlist");

        // Hiển thị danh sách playlist
        if (playlistNames.length > 0) {
            builder.setItems(playlistNames, (dialog, which) -> {
                Playlist selectedPlaylist = userPlaylists.get(which);
                addSongToPlaylist(selectedPlaylist);
            });
        } else {
            builder.setMessage("Bạn chưa có Playlist nào.");
        }

        // Nút tạo mới
        builder.setPositiveButton("Tạo Playlist Mới", (dialog, which) -> {
            showCreatePlaylistDialog();
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    // --- HÀM PHỤ: Thêm bài hát vào Playlist ---
    private void addSongToPlaylist(Playlist playlist) {
        playlistRepository.addSongToPlaylist(playlist.getId(), currentSongId, new PlaylistRepository.OnResultListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                ToastUtils.showSuccess(PlayerActivity.this, "Đã thêm vào " + playlist.getName());
            }

            @Override
            public void onError(Exception error) {
                ToastUtils.showError(PlayerActivity.this, error.getMessage());
            }
        });
    }

    // --- HÀM PHỤ: Hiển thị Dialog tạo Playlist mới ---
    private void showCreatePlaylistDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tạo Playlist Mới");

        final EditText input = new EditText(this);
        input.setHint("Tên Playlist");
        builder.setView(input);

        builder.setPositiveButton("Tạo", (dialog, which) -> {
            String name = input.getText().toString();
            if (!name.isEmpty()) {
                playlistRepository.createPlaylist(name, new PlaylistRepository.OnResultListener<String>() {
                    @Override
                    public void onSuccess(String playlistId) {
                        ToastUtils.showSuccess(PlayerActivity.this, "Đã tạo Playlist!");
                        // Sau khi tạo xong, userPlaylists sẽ tự động cập nhật nhờ listener
                        // Có thể tự động thêm bài hát vào playlist vừa tạo (tùy chọn)
                    }

                    @Override
                    public void onError(Exception error) {
                        ToastUtils.showError(PlayerActivity.this, "Lỗi tạo Playlist");
                    }
                });
            }
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    // ... (Giữ nguyên các hàm downloadSong, shareSong, loadCoverImage, v.v.) ...

    private void downloadSong() {
        if (currentAudioUrl == null || !currentAudioUrl.startsWith("http")) {
            ToastUtils.showWarning(this, "Không thể tải bài hát Offline");
            return;
        }
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(currentAudioUrl));
            request.setTitle(currentSongTitle);
            request.setDescription("Đang tải nhạc...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, currentSongTitle + ".mp3");

            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            manager.enqueue(request);
            ToastUtils.showInfo(this, "Đang bắt đầu tải xuống...");
        } catch (Exception e) {
            ToastUtils.showError(this, "Lỗi tải xuống: " + e.getMessage());
        }
    }

    private void shareSong() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String shareBody = "Nghe bài hát cực hay này nhé: " + currentSongTitle + " - " + currentSongArtist + "\n" + currentAudioUrl;
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Chia sẻ bài hát");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ qua"));
    }

    private void updateShuffleRepeatUI() {
        int shuffleColor = player.isShuffleEnabled() ? R.color.accent_orange : R.color.text_secondary;
        btnShuffle.setColorFilter(ContextCompat.getColor(this, shuffleColor));

        int repeatColor = player.isRepeatEnabled() ? R.color.accent_orange : R.color.text_secondary;
        btnRepeat.setColorFilter(ContextCompat.getColor(this, repeatColor));
    }

    private void updateLikeUI() {
        if (isLiked) {
            btnLike.setImageResource(R.drawable.ic_heart_filled);
            btnLike.setColorFilter(ContextCompat.getColor(this, R.color.accent_secondary));
        } else {
            btnLike.setImageResource(R.drawable.ic_heart);
            btnLike.setColorFilter(ContextCompat.getColor(this, R.color.white));
        }
    }

    private void loadCoverImage(boolean isOnline, String imageUrl, String audioUrl) {
        if (isOnline && imageUrl != null && !imageUrl.isEmpty()) {
            // Online: Load từ URL
            ImageLoader.loadWithCallback(this, imageUrl, albumArt, bitmap -> {
                applyGradientFromBitmap(bitmap);
            });
        } else if (audioUrl != null) {
            loadAlbumArtFromMetadata(audioUrl);
        }
    }

    private void applyGradientFromBitmap(Bitmap bitmap) {
        Palette.from(bitmap).generate(palette -> {
            int dominant = palette.getDominantColor(
                    ContextCompat.getColor(this, R.color.background_dark)
            );
            int vibrant = palette.getVibrantColor(dominant);
            int darkVibrant = palette.getDarkVibrantColor(dominant);

            GradientDrawable gradient = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{
                            vibrant,
                            darkVibrant,
                            ContextCompat.getColor(this, R.color.background_dark)
                    }
            );

            gradient.setCornerRadius(0f);
            playerRoot.setBackground(gradient);
        });
    }

    private void loadAlbumArtFromMetadata(String path) {
        new Thread(() -> {
            try {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(this, Uri.parse(path));
                byte[] art = retriever.getEmbeddedPicture();
                retriever.release();
                if (art != null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
                    runOnUiThread(() -> {
                        albumArt.setImageBitmap(bitmap);
                        applyGradientFromBitmap(bitmap);
                    });
                } else {
                    runOnUiThread(() -> albumArt.setImageResource(R.drawable.ic_music));
                }
            } catch (Exception e) {
                runOnUiThread(() -> albumArt.setImageResource(R.drawable.ic_music));
            }
        }).start();
    }

    private final Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPlaying) {
                try {
                    int current = player.getCurrentPosition();
                    int duration = player.getDuration();
                    if (duration > 0) {
                        seekBar.setMax(duration);
                        seekBar.setProgress(current);
                        currentTime.setText(TimeFormatter.formatDuration(current));
                        totalTime.setText(TimeFormatter.formatDuration(duration));
                    }
                    handler.postDelayed(this, 1000);
                } catch (Exception ignored) {}
            }
        }
    };

    private void updateSeekBar() {
        handler.removeCallbacks(updateSeekBarRunnable);
        handler.post(updateSeekBarRunnable);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateSeekBarRunnable);
        if (player != null && playerListener != null) {
            player.removeListener(playerListener);
        }
        // Hủy lắng nghe playlist khi thoát
        if (playlistListener != null) {
            playlistListener.remove();
        }
    }
}