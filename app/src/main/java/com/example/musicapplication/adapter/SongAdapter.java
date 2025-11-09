package com.example.musicapplication.adapter;

import android.content.Context;
import android.content.Intent;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.musicapplication.main.PlayerFragment;
import com.example.musicapplication.R;
import com.example.musicapplication.model.Song;
import com.example.musicapplication.player.PlaylistManager;

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {
    private final List<Song> songs;
    private final Context context;

    public SongAdapter(Context context, List<Song> songs) {
        this.context = context;
        this.songs = songs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song s = songs.get(position);
        holder.title.setText(s.title);
        holder.artist.setText(s.artist != null ? s.artist : "Unknown");

        // Load album art directly from MP3 file metadata
        loadAlbumArt(holder.image, s.uri);

        holder.itemView.setOnClickListener(view -> {
            // Lưu playlist vào PlaylistManager trước khi mở PlayerFragment
            PlaylistManager.getInstance().setPlaylist(songs, position);

            Intent i = new Intent(context, PlayerFragment.class);
            i.putExtra("uri", s.uri);
            i.putExtra("title", s.title);
            i.putExtra("artist", s.artist);
            i.putExtra("albumId", s.albumId);
            i.putExtra("songIndex", position);
            i.putExtra("playlistSize", songs.size());
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        });

        // Long click to show song details including albumId
        holder.itemView.setOnLongClickListener(view -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(view.getContext());
            builder.setTitle("📝 Thông tin bài hát");
            builder.setMessage(
                    "🎵 Tên: " + s.title + "\n" +
                    "🎤 Nghệ sĩ: " + (s.artist != null ? s.artist : "Unknown") + "\n" +
                    "💿 Album ID: " + s.albumId + "\n\n" +
                    "👉 Sử dụng Album ID này trong AlbumsFragment!"
            );
            builder.setPositiveButton("Sao chép ID", (dialog, which) -> {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                    context.getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Album ID",
                    String.valueOf(s.albumId));
                clipboard.setPrimaryClip(clip);
                android.widget.Toast.makeText(context, "✅ Đã sao chép Album ID: " + s.albumId,
                    android.widget.Toast.LENGTH_SHORT).show();
            });
            builder.setNegativeButton("Đóng", null);
            builder.show();
            return true;
        });
    }

    private void loadAlbumArt(ImageView imageView, String uriString) {
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
                e.printStackTrace();
                try {
                    retriever.release();
                } catch (Exception ignored) {}
            }

            final Bitmap finalAlbumArt = albumArt;
            ((android.app.Activity) context).runOnUiThread(() -> {
                if (finalAlbumArt != null) {
                    Glide.with(context)
                            .load(finalAlbumArt)
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .centerCrop()
                            .into(imageView);
                } else {
                    Glide.with(context)
                            .load(R.drawable.ic_music)
                            .into(imageView);
                }
            });
        }).start();
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, artist;
        ImageView image;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.item_song_title);
            artist = itemView.findViewById(R.id.item_song_artist);
            image = itemView.findViewById(R.id.imageView);
        }
    }
}
