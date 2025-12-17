package com.example.musicapplication.fragments; // Hoặc package fragments của bạn

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.musicapplication.R;
import com.example.musicapplication.domain.model.Song;
import com.example.musicapplication.main.PlayerActivity;
import com.example.musicapplication.player.MusicPlayer;

public class MiniPlayerFragment extends Fragment implements MusicPlayer.OnCompletionListener {

    private ImageView imgAlbumArt;
    private TextView tvTitle, tvArtist;
    private ImageButton btnPlayPause, btnNext;
    private View layoutRoot;
    private MusicPlayer musicPlayer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mini_player, container, false);

        musicPlayer = MusicPlayer.getInstance(getContext());

        initViews(view);
        setupActions();

        return view;
    }

    private void initViews(View view) {
        imgAlbumArt = view.findViewById(R.id.img_mini_album_art);
        tvTitle = view.findViewById(R.id.tv_mini_song_title);
        tvArtist = view.findViewById(R.id.tv_mini_song_artist);
        btnPlayPause = view.findViewById(R.id.btn_mini_play_pause);
        btnNext = view.findViewById(R.id.btn_mini_next);
        layoutRoot = view.findViewById(R.id.layout_mini_player);
    }

    private void setupActions() {
        // 1. Click vào thanh MiniPlayer -> Mở Full Player
        layoutRoot.setOnClickListener(v -> {
            Song currentSong = musicPlayer.getCurrentSong();
            if (currentSong != null) {
                Intent intent = new Intent(getContext(), PlayerActivity.class);
                // Truyền dữ liệu cần thiết để PlayerActivity hiển thị ngay
                intent.putExtra("songId", currentSong.getId());
                intent.putExtra("title", currentSong.getTitle());
                intent.putExtra("artist", currentSong.getArtist());
                intent.putExtra("imageUrl", currentSong.getImageUrl());
                intent.putExtra("audioUrl", currentSong.getAudioUrl());
                intent.putExtra("isOnline", currentSong.isOnline());
                intent.putExtra("songIndex", musicPlayer.getCurrentSongIndex());
                startActivity(intent);

                // Hiệu ứng chuyển cảnh trượt lên (Slide Up)
                if (getActivity() != null) {
                    getActivity().overridePendingTransition(R.anim.slide_in_up, R.anim.no_animation);
                }
            }
        });

        // 2. Nút Play/Pause
        btnPlayPause.setOnClickListener(v -> {
            if (musicPlayer.isPlaying()) {
                musicPlayer.pause();
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
            } else {
                musicPlayer.resume();
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            }
            updateUI(); // Cập nhật lại icon
        });

        // 3. Nút Next
        btnNext.setOnClickListener(v -> musicPlayer.playNext());
    }

    @Override
    public void onResume() {
        super.onResume();
        // Đăng ký nhận sự kiện khi Fragment hiện lên
        musicPlayer.addListener(this);
        updateUI(); // Cập nhật giao diện ngay lập tức
    }

    @Override
    public void onPause() {
        super.onPause();
        // Hủy đăng ký khi Fragment ẩn đi (để tránh lỗi)
        musicPlayer.removeListener(this);
    }

    // Hàm cập nhật giao diện
    private void updateUI() {
        Song currentSong = musicPlayer.getCurrentSong();

        // Nếu không có bài hát nào -> Ẩn MiniPlayer
        if (currentSong == null) {
            if (getView() != null) getView().setVisibility(View.GONE);
            return;
        }

        // Có bài hát -> Hiện MiniPlayer
        if (getView() != null) getView().setVisibility(View.VISIBLE);

        tvTitle.setText(currentSong.getTitle());
        tvArtist.setText(currentSong.getArtist());

        // Load ảnh
        if (getContext() != null) {
            if (currentSong.isOnline() && currentSong.getImageUrl() != null) {
                Glide.with(getContext()).load(currentSong.getImageUrl()).placeholder(R.drawable.ic_music).into(imgAlbumArt);
            } else {
                imgAlbumArt.setImageResource(R.drawable.ic_music);
            }
        }

        // Cập nhật nút Play/Pause
        if (musicPlayer.isPlaying()) {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    // --- CÁC CALLBACK TỪ MUSICPLAYER ---
    @Override
    public void onCompletion() {
        updateUI();
    }

    @Override
    public void onNextSong(Song song) {
        updateUI();
    }

    @Override
    public void onPreviousSong(Song song) {
        updateUI();
    }
}