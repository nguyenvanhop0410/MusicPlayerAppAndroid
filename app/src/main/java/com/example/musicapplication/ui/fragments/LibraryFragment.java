package com.example.musicapplication.ui.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicapplication.R;
import com.example.musicapplication.ui.adapter.PlaylistAdapter;
import com.example.musicapplication.ui.adapter.SongListAdapter;
import com.example.musicapplication.data.repository.FavoriteRepository;
import com.example.musicapplication.data.repository.HistoryRepository;
import com.example.musicapplication.data.repository.PlaylistRepository;
import com.example.musicapplication.data.repository.SongRepository;
import com.example.musicapplication.data.repository.UserRepository;
import com.example.musicapplication.model.Song;
import com.example.musicapplication.model.User;
import com.example.musicapplication.ui.activity.player.PlayerActivity;
import com.example.musicapplication.ui.activity.playlist.PlaylistDetailActivity;
import com.example.musicapplication.model.Playlist;
import com.example.musicapplication.player.MusicPlayer;
import com.example.musicapplication.player.PlaylistManager;
import com.example.musicapplication.ui.activity.upload.UploadSongActivity;
import com.example.musicapplication.utils.ImageLoader;
import com.example.musicapplication.utils.ToastUtils;
import com.example.musicapplication.utils.Logger;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class LibraryFragment extends Fragment {

    // UI Components
    private RecyclerView recyclerPlaylists, recyclerLiked, recyclerHistory;
    private ProgressBar progressBar;
    private MaterialButton btnCreatePlaylist, btnUpload;
    private TextInputEditText etSearch;
    private ImageView imgProfile;
    private TextView tvClearHistory; // Nút xóa lịch sử

    // Adapters
    private PlaylistAdapter playlistAdapter;
    private SongListAdapter likedSongAdapter;
    private SongListAdapter historyAdapter;

    // Data Lists (Original - dùng để lưu trữ dữ liệu gốc từ Firebase)
    private List<Playlist> allPlaylists = new ArrayList<>();
    private List<Song> allLikedSongs = new ArrayList<>();
    private List<Song> allHistorySongs = new ArrayList<>();

    // Data Lists (Display - dùng để hiển thị và filter)
    private List<Playlist> displayPlaylists = new ArrayList<>();
    private List<Song> displayLikedSongs = new ArrayList<>();
    private List<Song> displayHistorySongs = new ArrayList<>();

    // Repositories
    private SongRepository songRepository;
    private FavoriteRepository favoriteRepository;
    private PlaylistRepository playlistRepository;
    private HistoryRepository historyRepository;
    private UserRepository userRepository;

    // Listeners (Để hủy đăng ký khi thoát màn hình)
    private ListenerRegistration likedSongsListener;
    private ListenerRegistration playlistsListener;
    private ListenerRegistration historyListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        // Khởi tạo các Repository
        Context context = getContext();
        songRepository = new SongRepository(context);
        favoriteRepository = new FavoriteRepository(context);
        playlistRepository = new PlaylistRepository(context);
        historyRepository = new HistoryRepository(context);
        userRepository = new UserRepository(context);

        initViews(view);
        loadData();
        return view;
    }

    private void initViews(View view) {
        etSearch = view.findViewById(R.id.et_library_search);
        btnCreatePlaylist = view.findViewById(R.id.btn_create_playlist);
        btnUpload = view.findViewById(R.id.btn_upload);
        progressBar = view.findViewById(R.id.progress_bar);
        imgProfile = view.findViewById(R.id.img_profile);
        tvClearHistory = view.findViewById(R.id.tv_clear_history); // Ánh xạ nút xóa

        imgProfile = view.findViewById(R.id.img_profile);
        loadUserProfile();

        setupRecyclerViews(view);
        setupEvents();
    }

    private void loadUserProfile() {
        if (imgProfile == null) return;
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userRepository.getUser(currentUser.getUid(), new UserRepository.OnResultListener<User>() {
                @Override
                public void onSuccess(User user) {
                    if (getActivity() != null && user != null && user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                        getActivity().runOnUiThread(() -> {
                            ImageLoader.loadCircle(getContext(), user.getPhotoUrl(), imgProfile);
                        });
                    }
                }
                @Override
                public void onError(Exception error) {
                    Logger.e("Error loading user profile", error);
                }
            });
        } else {
            // Chưa đăng nhập
            imgProfile.setImageResource(R.drawable.ic_profile);
        }
    }

    private void setupRecyclerViews(View view) {
        // 1. Playlists
        recyclerPlaylists = view.findViewById(R.id.recycler_playlists);
        recyclerPlaylists.setLayoutManager(new LinearLayoutManager(getContext()));
        playlistAdapter = new PlaylistAdapter(getContext(), displayPlaylists, playlist -> {
            Intent intent = new Intent(getContext(), PlaylistDetailActivity.class);
            intent.putExtra("playlist", playlist);
            startActivity(intent);
        });
        recyclerPlaylists.setAdapter(playlistAdapter);

        // 2. Liked Songs
        recyclerLiked = view.findViewById(R.id.recycler_liked_songs);
        recyclerLiked.setLayoutManager(new LinearLayoutManager(getContext()));
        likedSongAdapter = new SongListAdapter(getContext(), displayLikedSongs, this::onSongClick);
        recyclerLiked.setAdapter(likedSongAdapter);

        // 3. History
        recyclerHistory = view.findViewById(R.id.recycler_history);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        historyAdapter = new SongListAdapter(getContext(), displayHistorySongs, this::onSongClick);

        // Gán sự kiện nhấn giữ để xóa lịch sử
        historyAdapter.setOnSongLongClickListener((song, position) -> {
            showDeleteHistoryDialog(song);
        });

        recyclerHistory.setAdapter(historyAdapter);
    }

    private void setupEvents() {
        btnCreatePlaylist.setOnClickListener(v -> showCreatePlaylistDialog());

        btnUpload.setOnClickListener(v ->{
            Intent intent = new Intent(getContext(), UploadSongActivity.class);
            startActivity(intent);
        });

        // Sự kiện click nút Xóa lịch sử
        if (tvClearHistory != null) {
            tvClearHistory.setOnClickListener(v -> showClearHistoryConfirmation());
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Gọi filterData mỗi khi text thay đổi để cập nhật list hiển thị
                filterData(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadData() {
        setLoading(true);

        // 1. Listen Playlists (Từ PlaylistRepository)
        playlistsListener = playlistRepository.getRealtimeUserPlaylists(new PlaylistRepository.OnResultListener<List<Playlist>>() {
            @Override
            public void onSuccess(List<Playlist> result) {
                // Log để kiểm tra dữ liệu có về không
                Logger.d("Playlists loaded: " + result.size());

                allPlaylists.clear();
                allPlaylists.addAll(result);

                // Cập nhật danh sách hiển thị và Adapter
                refreshDisplayLists();

                setLoading(false);
            }
            @Override
            public void onError(Exception e) {
                Logger.e("Error loading playlists", e);
                setLoading(false);
            }
        });

        // 2. Listen Liked Songs (Từ FavoriteRepository)
        likedSongsListener = favoriteRepository.listenToLikedSongs(new FavoriteRepository.OnResultListener<List<Song>>() {
            @Override
            public void onSuccess(List<Song> result) {
                allLikedSongs.clear();
                allLikedSongs.addAll(result);
                refreshDisplayLists();
            }
            @Override public void onError(Exception e) { Logger.e("Error loading liked songs", e); }
        });

        // 3. Listen History (Từ HistoryRepository)
        historyListener = historyRepository.getRealtimeHistory(new HistoryRepository.OnResultListener<List<Song>>() {
            @Override
            public void onSuccess(List<Song> result) {
                allHistorySongs.clear();
                allHistorySongs.addAll(result);
                refreshDisplayLists();
            }
            @Override public void onError(Exception e) { Logger.e("Error loading history", e); }
        });
    }

    // --- Các hàm Dialog (Xóa/Tạo) giữ nguyên ---
    private void showDeleteHistoryDialog(Song song) {
        new AlertDialog.Builder(getContext())
                .setTitle("Xóa khỏi lịch sử")
                .setMessage("Bạn có muốn xóa bài hát '" + song.getTitle() + "' khỏi lịch sử nghe không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    historyRepository.removeFromHistory(song.getId(), null);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showClearHistoryConfirmation() {
        if (allHistorySongs.isEmpty()) {
            ToastUtils.showInfo(getContext(), "Lịch sử đang trống!");
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Xoá lịch sử")
                .setMessage("Bạn có chắc chắn muốn xoá toàn bộ lịch sử nghe nhạc không?")
                .setPositiveButton("Xoá", (dialog, which) -> {
                    historyRepository.clearHistory(null);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showCreatePlaylistDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Tạo Playlist Mới");

        final EditText input = new EditText(getContext());
        input.setHint("Tên Playlist");
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                playlistRepository.createPlaylist(name, new PlaylistRepository.OnResultListener<String>() {
                    @Override
                    public void onSuccess(String result) {
                        ToastUtils.showSuccess(getContext(), "Đã tạo Playlist!");
                    }
                    @Override
                    public void onError(Exception e) {
                        ToastUtils.showError(getContext(), "Failed: " + e.getMessage());
                    }
                });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // --- LOGIC CẬP NHẬT LIST HIỂN THỊ ---

    // Hàm này được gọi mỗi khi dữ liệu gốc thay đổi HOẶC ô tìm kiếm thay đổi
    private void refreshDisplayLists() {
        if (etSearch != null) {
            // Lọc lại dữ liệu dựa trên text hiện tại trong ô tìm kiếm
            filterData(etSearch.getText().toString());
        } else {
            // Fallback: Nếu chưa có view tìm kiếm, hiển thị tất cả
            copyAllToDisplay();
            updateAdapters();
        }
    }
    private void filterData(String query) {
        String lowerQuery = query.toLowerCase().trim();

        // Clear danh sách hiển thị cũ
        displayPlaylists.clear();
        displayLikedSongs.clear();
        displayHistorySongs.clear();

        if (lowerQuery.isEmpty()) {
            // Nếu không tìm kiếm -> Hiển thị tất cả
            displayPlaylists.addAll(allPlaylists);
            displayLikedSongs.addAll(allLikedSongs);
            displayHistorySongs.addAll(allHistorySongs);
        } else {
            // Nếu có tìm kiếm -> Lọc
            for (Playlist p : allPlaylists) {
                if (p.getName().toLowerCase().contains(lowerQuery)) {
                    displayPlaylists.add(p);
                }
            }
            for (Song s : allLikedSongs) {
                if (s.getTitle().toLowerCase().contains(lowerQuery) ||
                        s.getArtist().toLowerCase().contains(lowerQuery)) {
                    displayLikedSongs.add(s);
                }
            }
            for (Song s : allHistorySongs) {
                if (s.getTitle().toLowerCase().contains(lowerQuery) ||
                        s.getArtist().toLowerCase().contains(lowerQuery)) {
                    displayHistorySongs.add(s);
                }
            }
        }

        updateAdapters();
    }
    // Helper copy list (khi không tìm kiếm)
    private void copyAllToDisplay() {
        displayPlaylists.clear();
        displayPlaylists.addAll(allPlaylists);

        displayLikedSongs.clear();
        displayLikedSongs.addAll(allLikedSongs);

        displayHistorySongs.clear();
        displayHistorySongs.addAll(allHistorySongs);
    }

    // Cập nhật giao diện (Notify Adapters)
    private void updateAdapters() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (playlistAdapter != null) {
                    playlistAdapter.updateList(displayPlaylists);
                }
                if (likedSongAdapter != null) likedSongAdapter.notifyDataSetChanged();
                if (historyAdapter != null) historyAdapter.notifyDataSetChanged();
            });
        }
    }

    private void onSongClick(Song song, int position, List<Song> playlist) {
        PlaylistManager.getInstance().setPlaylist(playlist, position);
        MusicPlayer.getInstance(getContext()).setPlaylist(playlist, position);

        if (song.isOnline() && song.getId() != null) {
            songRepository.incrementPlayCount(song.getId());
        }
        if (historyRepository != null) {
            historyRepository.addToHistory(song.getId(), null);
        }

        Intent intent = new Intent(getContext(), PlayerActivity.class);
        intent.putExtra("songId", song.getId());
        intent.putExtra("title", song.getTitle());
        intent.putExtra("artist", song.getArtist());
        intent.putExtra("imageUrl", song.getImageUrl());
        intent.putExtra("audioUrl", song.getAudioUrl());
        intent.putExtra("songIndex", position);
        intent.putExtra("isOnline", song.isOnline());
        startActivity(intent);
    }
    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (likedSongsListener != null) likedSongsListener.remove();
        if (playlistsListener != null) playlistsListener.remove();
        if (historyListener != null) historyListener.remove();
    }
}