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
import com.example.musicapplication.model.Album;
import com.example.musicapplication.utils.ImageLoader;

import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {

    private Context context;
    private List<Album> albumList;
    private OnAlbumClickListener listener;

    public interface OnAlbumClickListener {
        void onAlbumClick(Album album);
    }

    public AlbumAdapter(Context context, List<Album> albumList, OnAlbumClickListener listener) {
        this.context = context;
        this.albumList = albumList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_album, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        Album album = albumList.get(position);

        holder.tvName.setText(album.getTitle());
        holder.tvArtist.setText(album.getArtist());

        // LOGIC HIỂN THỊ ẢNH THÔNG MINH
        if (album.getCoverUrl() != null && !album.getCoverUrl().isEmpty()) {
            // Trường hợp 1: Có link Online -> Dùng Glide tải
            ImageLoader.load(context, album.getCoverUrl(), holder.imgCover);
        } else if (album.getArtResId() != 0) {
            // Trường hợp 2: Có ảnh Offline (Resource ID) -> Set trực tiếp
            holder.imgCover.setImageResource(album.getArtResId());
        } else {
            // Trường hợp 3: Không có gì -> Hiện ảnh mặc định
            holder.imgCover.setImageResource(R.drawable.ic_music);
        }

        holder.itemView.setOnClickListener(v -> listener.onAlbumClick(album));
    }

    @Override
    public int getItemCount() {
        return albumList.size();
    }

    public static class AlbumViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView tvName, tvArtist;

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.img_album_cover);
            tvName = itemView.findViewById(R.id.tv_album_name);
            tvArtist = itemView.findViewById(R.id.tv_album_artist);
        }
    }
}