package com.example.musicapplication.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicapplication.R;
import com.example.musicapplication.model.Artist;

import java.util.List;

public class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder> {

    private Context context;
    private List<Artist> artistList;
    private OnArtistClickListener listener;

    public interface OnArtistClickListener {
        void onArtistClick(Artist artist);
    }

    public ArtistAdapter(Context context, List<Artist> artistList, OnArtistClickListener listener) {
        this.context = context;
        this.artistList = artistList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ArtistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng layout item_artist.xml
        View view = LayoutInflater.from(context).inflate(R.layout.item_artist, parent, false);
        return new ArtistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtistViewHolder holder, int position) {
        Artist artist = artistList.get(position);

        holder.tvName.setText(artist.getName());

        if (artist.getImageUrl() != null && !artist.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(artist.getImageUrl())
                    .placeholder(R.drawable.ic_profile) // Ảnh mặc định
                    .error(R.drawable.ic_profile)
                    .circleCrop() // Bo tròn ảnh nghệ sĩ
                    .into(holder.imgArtist);
        } else {
            holder.imgArtist.setImageResource(R.drawable.ic_profile);
        }

        holder.itemView.setOnClickListener(v -> listener.onArtistClick(artist));
    }

    @Override
    public int getItemCount() {
        return artistList.size();
    }

    public static class ArtistViewHolder extends RecyclerView.ViewHolder {
        ImageView imgArtist;
        TextView tvName;

        public ArtistViewHolder(@NonNull View itemView) {
            super(itemView);
            imgArtist = itemView.findViewById(R.id.img_artist);
            tvName = itemView.findViewById(R.id.txt_artist_name);
        }
    }
}