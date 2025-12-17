package com.example.musicapplication.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicapplication.R;
import com.example.musicapplication.model.Genre;

import java.util.List;

public class GenreAdapter extends RecyclerView.Adapter<GenreAdapter.GenreViewHolder> {

    private Context context;
    private List<Genre> genreList;
    private OnGenreClickListener listener;

    public interface OnGenreClickListener {
        void onGenreClick(Genre genre);
    }

    public GenreAdapter(Context context, List<Genre> genreList, OnGenreClickListener listener) {
        this.context = context;
        this.genreList = genreList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GenreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_genre, parent, false);
        return new GenreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GenreViewHolder holder, int position) {
        Genre genre = genreList.get(position);

        holder.tvName.setText(genre.getName());
        holder.container.setBackgroundColor(genre.getBackgroundColor());

        // Nếu bạn có ảnh riêng cho từng thể loại thì dùng Glide load vào imgGenre
        // Ở đây mình dùng ảnh mặc định và tint màu trắng
        holder.imgGenre.setImageResource(R.drawable.ic_music);

        holder.itemView.setOnClickListener(v -> listener.onGenreClick(genre));
    }

    @Override
    public int getItemCount() {
        return genreList.size();
    }

    public static class GenreViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView imgGenre;
        ConstraintLayout container;

        public GenreViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_genre_name);
            imgGenre = itemView.findViewById(R.id.img_genre);
            container = itemView.findViewById(R.id.container_genre);
        }
    }
}