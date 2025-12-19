package com.example.musicapplication.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musicapplication.R;
import com.example.musicapplication.model.Song;
import com.example.musicapplication.utils.ImageLoader;

import java.util.List;
import java.util.Locale;

public class SongListAdapter extends RecyclerView.Adapter<SongListAdapter.SongRowViewHolder> {

    private Context context;
    private List<Song> songList;
    private OnSongClickListener listener;
    private OnSongLongClickListener longClickListener; // Thêm listener long click

    public interface OnSongClickListener {
        void onSongClick(Song song, int position, List<Song> list);
    }

    // Interface mới cho sự kiện nhấn giữ
    public interface OnSongLongClickListener {
        void onSongLongClick(Song song, int position);
    }

    public SongListAdapter(Context context, List<Song> songList, OnSongClickListener listener) {
        this.context = context;
        this.songList = songList;
        this.listener = listener;
    }

    // Hàm setter để gán listener nhấn giữ từ Activity
    public void setOnSongLongClickListener(OnSongLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public SongRowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song_row, parent, false);
        return new SongRowViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongRowViewHolder holder, int position) {
        Song song = songList.get(position);

        holder.tvTitle.setText(song.getTitle());
        holder.tvArtist.setText(song.getArtist());
        holder.tvPlayCount.setText(formatPlayCount(song.getPlayCount()));

        if (song.getImageUrl() != null && !song.getImageUrl().isEmpty()) {
            ImageLoader.loadRounded(context, song.getImageUrl(), holder.imgSong, 10);
        } else {
            holder.imgSong.setImageResource(R.drawable.ic_music);
        }

        holder.itemView.setOnClickListener(v -> listener.onSongClick(song, position, songList));

        // Xử lý nhấn giữ
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onSongLongClick(song, position);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    private String formatPlayCount(int count) {
        if (count < 1000) return String.valueOf(count);
        if (count < 1000000) return String.format(Locale.US, "%.1fK", count / 1000.0);
        return String.format(Locale.US, "%.1fM", count / 1000000.0);
    }

    public static class SongRowViewHolder extends RecyclerView.ViewHolder {
        ImageView imgSong;
        TextView tvTitle, tvArtist, tvPlayCount;

        public SongRowViewHolder(@NonNull View itemView) {
            super(itemView);
            imgSong = itemView.findViewById(R.id.img_song);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvArtist = itemView.findViewById(R.id.tv_artist);
            tvPlayCount = itemView.findViewById(R.id.tv_play_count);
        }
    }
}