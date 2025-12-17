package com.example.musicapplication.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicapplication.R;
import com.example.musicapplication.model.Playlist;

import java.util.ArrayList;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private Context context;
    private List<Playlist> playlists;
    private List<Playlist> filteredPlaylists;
    private OnPlaylistClickListener listener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }

    public PlaylistAdapter(Context context, List<Playlist> playlists, OnPlaylistClickListener listener) {
        this.context = context;
        this.playlists = playlists;
        this.filteredPlaylists = new ArrayList<>(playlists);
        this.listener = listener;
    }

    public void updateList(List<Playlist> newList) {
        this.filteredPlaylists.clear();
        this.filteredPlaylists.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng layout item_playlist chuyên biệt
        View view = LayoutInflater.from(context).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = filteredPlaylists.get(position);
        holder.tvName.setText(playlist.getName());

        int count = playlist.getSongIds() != null ? playlist.getSongIds().size() : 0;
        holder.tvCount.setText(count + " songs");

        holder.itemView.setOnClickListener(v -> listener.onPlaylistClick(playlist));
    }

    @Override
    public int getItemCount() {
        return filteredPlaylists.size();
    }

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCount;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_playlist_name);
            tvCount = itemView.findViewById(R.id.tv_song_count);
        }
    }
}