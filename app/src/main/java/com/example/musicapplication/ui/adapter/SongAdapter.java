package com.example.musicapplication.ui.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
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
import com.example.musicapplication.utils.Logger;

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {
    private final List<Song> songs;
    private final Context context;
    private OnSongClickListener onSongClickListener;

    public interface OnSongClickListener {
        void onSongClick(Song song, int position, List<Song> playlist);
    }

    public SongAdapter(Context context, List<Song> songs) {
        this.context = context;
        this.songs = songs;
    }

    public SongAdapter(Context context, List<Song> songs, OnSongClickListener listener) {
        this.context = context;
        this.songs = songs;
        this.onSongClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_modern, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.title.setText(song.getTitle());
        holder.artist.setText(song.getArtist());

        // Load album art - support both online and local
        if (song.isOnline() && song.getImageUrl() != null && !song.getImageUrl().isEmpty()) {
            // Load from URL using Glide
            ImageLoader.load(context, song.getImageUrl(), holder.image);
        } else if (song.isLocal() && song.uri != null) {
            // Load from local file metadata
            loadAlbumArtFromLocal(holder.image, song.uri);
        } else {
            // Default icon
            ImageLoader.load(context, R.drawable.ic_music, holder.image);
        }

        holder.itemView.setOnClickListener(view -> {
            if (onSongClickListener != null) {
                onSongClickListener.onSongClick(song, position, songs);
            }
        });
    }

    private void loadAlbumArtFromLocal(ImageView imageView, String uriString) {
        new Thread(() -> {
            Bitmap albumArt = null;
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();

            try {
                retriever.setDataSource(context, Uri.parse(uriString));
                byte[] art = retriever.getEmbeddedPicture();

                if (art != null) {
                    albumArt = BitmapFactory.decodeByteArray(art, 0, art.length);
                }
                retriever.release();
            } catch (Exception e) {
                Logger.e("Error loading album art from local file: " + e.getMessage());
                try {
                    retriever.release();
                } catch (Exception ignored) {}
            }

            final Bitmap finalAlbumArt = albumArt;
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).runOnUiThread(() -> {
                    if (finalAlbumArt != null) {
                        imageView.setImageBitmap(finalAlbumArt);
                    } else {
                        ImageLoader.load(context, R.drawable.ic_music, imageView);
                    }
                });
            }
        }).start();
    }

    @Override
    public int getItemCount() {
        return songs != null ? songs.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, artist;
        ImageView image;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.txt_title);
            artist = itemView.findViewById(R.id.txt_artist);
            image = itemView.findViewById(R.id.img_album_art);
        }
    }
}
