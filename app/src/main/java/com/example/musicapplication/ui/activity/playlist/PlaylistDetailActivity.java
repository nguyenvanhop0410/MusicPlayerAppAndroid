package com.example.musicapplication.ui.activity.playlist;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicapplication.R;
import com.example.musicapplication.ui.activity.player.PlayerActivity;
import com.example.musicapplication.ui.adapter.SongListAdapter;
import com.example.musicapplication.data.repository.HistoryRepository;
import com.example.musicapplication.data.repository.PlaylistRepository;
import com.example.musicapplication.data.repository.SongRepository;
import com.example.musicapplication.model.Song;
import com.example.musicapplication.model.Playlist;
import com.example.musicapplication.player.MusicPlayer;
import com.example.musicapplication.player.PlaylistManager;
import com.example.musicapplication.utils.ImageLoader;
import com.example.musicapplication.utils.Logger;
import com.example.musicapplication.utils.ToastUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlaylistDetailActivity extends AppCompatActivity {

    private Playlist currentPlaylist;
    private TextView tvName, tvOwner, tvEmpty;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private ImageButton btnMenu;
    private FloatingActionButton btnPlay; // Nút play màu xanh
    private MaterialButton btnAddSongsBig;
    private ImageView imgPlaylistCover;

    private SongListAdapter songAdapter;
    private List<Song> songList = new ArrayList<>();

    // Khai báo 2 Repository riêng biệt
    private SongRepository songRepository;
    private PlaylistRepository playlistRepository;
    private HistoryRepository historyRepository;

    // Launcher chọn ảnh
    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadImage(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        currentPlaylist = (Playlist) getIntent().getSerializableExtra("playlist");
        if (currentPlaylist == null) {
            finish();
            return;
        }

        // Khởi tạo Repositories
        songRepository = new SongRepository(this);
        playlistRepository = new PlaylistRepository(this);
        historyRepository = new HistoryRepository(this);

        initViews();
        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPlaylistData();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
        tvName = findViewById(R.id.tv_playlist_name);
        tvOwner = findViewById(R.id.tv_owner_name);
        tvEmpty = findViewById(R.id.tv_empty_songs);
        recyclerView = findViewById(R.id.recycler_playlist_songs);
        progressBar = findViewById(R.id.progress_bar);
        imgPlaylistCover = findViewById(R.id.img_playlist_cover);

        btnMenu = findViewById(R.id.btn_menu);
        btnPlay = findViewById(R.id.btn_play);
        btnAddSongsBig = findViewById(R.id.btn_add_songs_big);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        songAdapter = new SongListAdapter(this, songList, (song, position, list) -> {
            // Click để phát nhạc
            playMusic(position);
        });

        // XỬ LÝ NHẤN GIỮ ĐỂ XÓA BÀI HÁT
        songAdapter.setOnSongLongClickListener((song, position) -> {
            showRemoveSongDialog(song);
        });

        recyclerView.setAdapter(songAdapter);

        // Events
        btnMenu.setOnClickListener(this::showOptionsMenu);

        btnAddSongsBig.setOnClickListener(v -> {
            // Mở màn hình AddSongToPlaylistActivity
            Intent intent = new Intent(this, AddSongPlaylistActivity.class);
            intent.putExtra("playlistId", currentPlaylist.getId());
            startActivity(intent);
        });

        // Nút Play to màu xanh
        btnPlay.setOnClickListener(v -> {
            if (songList != null && !songList.isEmpty()) {
                // Phát từ bài đầu tiên (index 0)
                playMusic(0);
                ToastUtils.showInfo(this, "Đang phát playlist...");
            } else {
                ToastUtils.showWarning(this, "Playlist trống");
            }
        });
    }

    private void updateUI() {
        tvName.setText(currentPlaylist.getName());
        String userName = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getDisplayName() : "Bạn";
        tvOwner.setText(userName != null && !userName.isEmpty() ? userName : "Bạn");

        // Load ảnh bìa nếu có
        if (currentPlaylist.getCoverImageUrl() != null && !currentPlaylist.getCoverImageUrl().isEmpty()) {
            ImageLoader.loadRounded(this, currentPlaylist.getCoverImageUrl(), imgPlaylistCover, 12);
        }
    }

    private void playMusic(int position) {
        PlaylistManager.getInstance().setPlaylist(songList, position);
        MusicPlayer.getInstance(this).setPlaylist(songList, position);
        if (position >= 0 && position < songList.size()) {
            Song song = songList.get(position);
            historyRepository.addToHistory(song.getId(), null);
            if (song.isOnline()) {
                songRepository.incrementPlayCount(song.getId());
            }
        }
        Intent intent = new Intent(this, PlayerActivity.class);
        Song song = songList.get(position);
        intent.putExtra("songId", song.getId());
        intent.putExtra("title", song.getTitle());
        intent.putExtra("artist", song.getArtist());
        intent.putExtra("imageUrl", song.getImageUrl());
        intent.putExtra("audioUrl", song.getAudioUrl());
        intent.putExtra("songIndex", position);
        intent.putExtra("isOnline", song.isOnline());
        startActivity(intent);
    }

    private void refreshPlaylistData() {
        setLoading(true);
        // Sử dụng PlaylistRepository để lấy thông tin mới nhất
        playlistRepository.getPlaylistById(currentPlaylist.getId(), new PlaylistRepository.OnResultListener<Playlist>() {
            @Override
            public void onSuccess(Playlist result) {
                currentPlaylist = result;
                updateUI();
                loadSongs();
            }

            @Override
            public void onError(Exception error) {
                setLoading(false);
                ToastUtils.showError(PlaylistDetailActivity.this, "Lỗi tải playlist");
            }
        });
    }

    private void loadSongs() {
        if (currentPlaylist.getSongIds() == null || currentPlaylist.getSongIds().isEmpty()) {
            songList.clear();
            songAdapter.notifyDataSetChanged();
            tvEmpty.setVisibility(View.VISIBLE);
            setLoading(false);
            return;
        }

        // Sử dụng SongRepository để lấy chi tiết bài hát
        songRepository.getSongsByIds(currentPlaylist.getSongIds(), new SongRepository.OnResultListener<List<Song>>() {
            @Override
            public void onSuccess(List<Song> result) {
                songList.clear();
                songList.addAll(result);
                runOnUiThread(() -> {
                    songAdapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(songList.isEmpty() ? View.VISIBLE : View.GONE);
                    setLoading(false);
                });
            }
            @Override public void onError(Exception e) { setLoading(false); }
        });
    }

    private void showOptionsMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        // Đảm bảo file res/menu/menu_playlist_options.xml tồn tại
        popup.getMenuInflater().inflate(R.menu.menu_playlist, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_image) {
                pickImageLauncher.launch("image/*");
            } else if (id == R.id.action_update) {
                showRenameDialog();
            } else if (id == R.id.action_delete) {
                showDeleteConfirmDialog();
            }
            return true;
        });
        popup.show();
    }

    private void showRenameDialog() {
        EditText input = new EditText(this);
        input.setText(currentPlaylist.getName());
        new AlertDialog.Builder(this)
                .setTitle("Đổi tên Playlist")
                .setView(input)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newName = input.getText().toString();
                    if (!newName.isEmpty()) {
                        // Sử dụng PlaylistRepository để cập nhật thông tin
                        playlistRepository.updatePlaylistInfo(currentPlaylist.getId(), newName, null, new PlaylistRepository.OnResultListener<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                currentPlaylist.setName(newName);
                                tvName.setText(newName);
                                ToastUtils.showSuccess(PlaylistDetailActivity.this, "Đã đổi tên!");
                            }
                            @Override public void onError(Exception e) {
                                ToastUtils.showError(PlaylistDetailActivity.this, "Lỗi cập nhật tên");
                            }
                        });
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa Playlist")
                .setMessage("Bạn có chắc muốn xóa playlist này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    // Sử dụng PlaylistRepository để xóa
                    playlistRepository.deletePlaylist(currentPlaylist.getId(), new PlaylistRepository.OnResultListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            ToastUtils.showSuccess(PlaylistDetailActivity.this, "Đã xóa!");
                            finish();
                        }
                        @Override public void onError(Exception e) {
                            ToastUtils.showError(PlaylistDetailActivity.this, "Lỗi xóa playlist");
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showRemoveSongDialog(Song song) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa khỏi Playlist")
                .setMessage("Bạn muốn xóa bài hát '" + song.getTitle() + "' khỏi playlist này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    // Sử dụng PlaylistRepository để xóa nhạc
                    playlistRepository.removeSongFromPlaylist(currentPlaylist.getId(), song.getId(), new PlaylistRepository.OnResultListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            ToastUtils.showSuccess(PlaylistDetailActivity.this, "Đã xóa bài hát");
                            // Refresh lại data
                            refreshPlaylistData();
                        }
                        @Override public void onError(Exception e) {
                            ToastUtils.showError(PlaylistDetailActivity.this, "Lỗi xóa bài hát");
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void uploadImage(Uri imageUri) {
        setLoading(true);
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] data = baos.toByteArray();

            playlistRepository.updatePlaylistImage(currentPlaylist.getId(), data, new PlaylistRepository.OnResultListener<String>() {
                @Override
                public void onSuccess(String imageUrl) {
                    currentPlaylist.setCoverImageUrl(imageUrl);
                    ImageLoader.loadRounded(PlaylistDetailActivity.this, imageUrl, imgPlaylistCover, 12);
                    ToastUtils.showSuccess(PlaylistDetailActivity.this, "Cập nhật ảnh bìa thành công!");
                    setLoading(false);
                }

                @Override
                public void onError(Exception error) {
                    ToastUtils.showError(PlaylistDetailActivity.this, "Lỗi cập nhật ảnh: " + error.getMessage());
                    setLoading(false);
                }
            });
        } catch (IOException e) {
            Logger.e("PlaylistDetail", e);
            ToastUtils.showError(this, "Không thể tải ảnh");
            setLoading(false);
        }
    }

    private void setLoading(boolean loading) {
        if (progressBar != null)
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}