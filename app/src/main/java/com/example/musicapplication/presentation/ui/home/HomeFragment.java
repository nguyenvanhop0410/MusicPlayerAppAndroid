package com.example.musicapplication.presentation.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.musicapplication.R;
import com.example.musicapplication.adapter.AlbumAdapter;
import com.example.musicapplication.adapter.SliderAdapter;
import com.example.musicapplication.adapter.SongAdapter;
import com.example.musicapplication.adapter.SongListAdapter;
import com.example.musicapplication.data.repository.AuthRepository;
import com.example.musicapplication.data.repository.SongRepository;
import com.example.musicapplication.data.repository.UserRepository;
import com.example.musicapplication.domain.model.User;
import com.example.musicapplication.main.AlbumDetailActivity;
import com.example.musicapplication.main.AllAlbumsActivity; // Import Activity mới
import com.example.musicapplication.main.PlayerActivity;
import com.example.musicapplication.model.Album;
import com.example.musicapplication.domain.model.SliderItem;
import com.example.musicapplication.domain.model.Song;
import com.example.musicapplication.player.MusicPlayer;
import com.example.musicapplication.player.PlaylistManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeFragment extends Fragment {

    // ... (Giữ nguyên các biến UI Components cũ)
    private ViewPager2 vpSlider;
    private RecyclerView recyclerAlbums;
    private RecyclerView recyclerPopular;
    private RecyclerView recyclerNewSongs;
    private ProgressBar progressBar;
    private ImageView imgProfile;
    private TextView tvSeeAllAlbums;

    private AlbumAdapter albumAdapter;
    private SongAdapter newSongAdapter;
    private SongListAdapter popularAdapter;

    private SongRepository songRepository;
    private AuthRepository authRepository;
    private UserRepository userRepository;

    private List<Album> albumList = new ArrayList<>();
    private List<Song> popularList = new ArrayList<>();
    private List<Song> newList = new ArrayList<>();

    private Handler sliderHandler = new Handler();
    private Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (vpSlider != null) vpSlider.setCurrentItem(vpSlider.getCurrentItem() + 1);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        songRepository = new SongRepository(getContext());
        authRepository = new AuthRepository(getContext());
        userRepository = new UserRepository(getContext());
        setupViews(view);
        loadData();
        return view;
    }

    private void setupViews(View view) {
        vpSlider = view.findViewById(R.id.vp_slider);
        setupSlider();

        // --- SETUP ALBUMS (GRID 2 CỘT) ---
        recyclerAlbums = view.findViewById(R.id.recycler_albums);
        recyclerAlbums.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerAlbums.setNestedScrollingEnabled(false); // Fix lỗi hiển thị trong ScrollView

        albumAdapter = new AlbumAdapter(getContext(), albumList, album -> {
            Intent intent = new Intent(getContext(), AlbumDetailActivity.class);
            intent.putExtra("albumName", album.getTitle());
            intent.putExtra("albumImage", album.getCoverUrl());
            startActivity(intent);
        });
        recyclerAlbums.setAdapter(albumAdapter);

        // --- XỬ LÝ SỰ KIỆN NÚT SEE ALL ---
        tvSeeAllAlbums = view.findViewById(R.id.tv_see_all_albums);
        tvSeeAllAlbums.setOnClickListener(v -> {
            // Mở màn hình xem tất cả Album
            Intent intent = new Intent(getContext(), AllAlbumsActivity.class);
            startActivity(intent);
        });

        // ... (Giữ nguyên phần setup Popular và New Songs)
        recyclerPopular = view.findViewById(R.id.recycler_popular);
        recyclerPopular.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        popularAdapter = new SongListAdapter(getContext(), popularList, this::onSongClick);
        recyclerPopular.setAdapter(popularAdapter);

        recyclerNewSongs = view.findViewById(R.id.recycler_new_songs);
        recyclerNewSongs.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        newSongAdapter = new SongAdapter(getContext(), newList, this::onSongClick);
        recyclerNewSongs.setAdapter(newSongAdapter);

        progressBar = view.findViewById(R.id.progress_bar);
        imgProfile = view.findViewById(R.id.img_profile);
        loadUserProfile();
    }

    private void loadUserProfile() {
        if (imgProfile == null) return;
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Cách 2: Lấy từ Firestore để đảm bảo dữ liệu mới nhất (nếu user vừa đổi ảnh)
            userRepository.getUser(currentUser.getUid(), new UserRepository.OnResultListener<User>() {
                @Override
                public void onSuccess(User user) {
                    if (getActivity() != null && user != null && user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                        getActivity().runOnUiThread(() -> {
                            Glide.with(HomeFragment.this)
                                    .load(user.getPhotoUrl())
                                    .placeholder(R.drawable.ic_profile)
                                    .error(R.drawable.ic_profile)
                                    .circleCrop()
                                    .into(imgProfile);
                        });
                    }
                }
                @Override
                public void onError(Exception error) {
                    Log.e("HomeFragment", "Error loading user profile", error);
                }
            });
        } else {
            // Chưa đăng nhập
            imgProfile.setImageResource(R.drawable.ic_profile);
        }
    }

    // ... (Giữ nguyên setupSlider)
    private void setupSlider() {
        List<SliderItem> sliderItems = new ArrayList<>();
        sliderItems.add(new SliderItem("https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=800&q=80"));
        sliderItems.add(new SliderItem("https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800&q=80"));
        sliderItems.add(new SliderItem("https://images.unsplash.com/photo-1493225255756-d9584f8606e9?w=800&q=80"));

        if (vpSlider != null) {
            vpSlider.setAdapter(new SliderAdapter(sliderItems, vpSlider));
            vpSlider.setClipToPadding(false);
            vpSlider.setClipChildren(false);
            vpSlider.setOffscreenPageLimit(3);
            vpSlider.getChildAt(0).setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
            CompositePageTransformer transformer = new CompositePageTransformer();
            transformer.addTransformer(new MarginPageTransformer(40));
            transformer.addTransformer((page, position) -> {
                float r = 1 - Math.abs(position);
                page.setScaleY(0.85f + r * 0.15f);
            });
            vpSlider.setPageTransformer(transformer);
            vpSlider.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    sliderHandler.removeCallbacks(sliderRunnable);
                    sliderHandler.postDelayed(sliderRunnable, 3000);
                }
            });
        }
    }

    private void loadData() {
        if (!authRepository.isLoggedIn()) {
            if (getActivity() != null) getActivity().runOnUiThread(() -> setLoading(false));
            return;
        }
        setLoading(true);

        // 1. LOAD ALBUMS (Giới hạn hiển thị 4 cái)
        songRepository.getAlbums(new SongRepository.OnResultListener<List<Album>>() {
            @Override
            public void onSuccess(List<Album> result) {
                if (result != null && !result.isEmpty()) {
                    processAlbumsForHome(result);
                } else {
                    loadAlbumsFromSongs(); // Fallback
                }
            }
            @Override
            public void onError(Exception error) {
                loadAlbumsFromSongs(); // Fallback
            }
        });

        // ... (Giữ nguyên phần load Popular và New Songs)
        songRepository.getTrendingSongs(10, new SongRepository.OnResultListener<List<Song>>() {
            @Override
            public void onSuccess(List<Song> result) {
                popularList.clear();
                popularList.addAll(result);
                updateUI();
            }
            @Override public void onError(Exception e) {}
        });

        songRepository.getRecentlyAddedSongs(10, new SongRepository.OnResultListener<List<Song>>() {
            @Override
            public void onSuccess(List<Song> result) {
                newList.clear();
                newList.addAll(result);
                updateUI();
            }
            @Override public void onError(Exception e) {}
        });
    }

    // Hàm xử lý giới hạn 4 Album
    private void processAlbumsForHome(List<Album> fullList) {
        albumList.clear();
        // Chỉ lấy tối đa 4 phần tử
        int limit = 4;
        for (int i = 0; i < fullList.size() && i < limit; i++) {
            albumList.add(fullList.get(i));
        }
        updateUI();
    }

    // Fallback: Tự tạo album từ bài hát (Cũng giới hạn 4 cái)
    private void loadAlbumsFromSongs() {
        songRepository.getTrendingSongs(50, new SongRepository.OnResultListener<List<Song>>() {
            @Override
            public void onSuccess(List<Song> result) {
                List<Album> tempAlbums = new ArrayList<>();
                Set<String> added = new HashSet<>();
                for (Song song : result) {
                    String name = song.getAlbum();
                    if (name != null && !added.contains(name)) {
                        tempAlbums.add(new Album(song.getId(), name, song.getArtist(), song.getImageUrl()));
                        added.add(name);
                    }
                }
                processAlbumsForHome(tempAlbums);
            }
            @Override public void onError(Exception e) { updateUI(); }
        });
    }

    private void updateUI() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                albumAdapter.notifyDataSetChanged();
                popularAdapter.notifyDataSetChanged();
                newSongAdapter.notifyDataSetChanged();
                setLoading(false);
            });
        }
    }

    private void onSongClick(Song song, int position, List<Song> playlist) {
        PlaylistManager.getInstance().setPlaylist(playlist, position);
        MusicPlayer.getInstance(getContext()).setPlaylist(playlist, position);
        if (song.isOnline() && song.getId() != null) songRepository.incrementPlayCount(song.getId());

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
        if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        sliderHandler.postDelayed(sliderRunnable, 3000);
        loadUserProfile();
    }
}