package com.example.musicapplication.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicapplication.R;
import com.example.musicapplication.adapter.AlbumAdapter;
import com.example.musicapplication.model.Album;

import java.util.ArrayList;
import java.util.List;

public class AlbumsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_albums, container, false);

        RecyclerView rv = view.findViewById(R.id.recycler_albums);
        // Use GridLayoutManager with 2 columns for better space efficiency
        rv.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Tạo album theo NGHỆ SĨ - dễ dàng hơn!
        // Chỉ cần điền đúng tên nghệ sĩ như trong file MP3
        List<Album> albums = new ArrayList<>();

        // VD: Nếu bạn có bài hát của "Sơn Tùng M-TP", điền đúng tên đó
        albums.add(new Album("SKY", "Sơn Tùng(MTP)", R.drawable.sontungmtp,
                "artist", "Sơn Tùng M-TP"));

        albums.add(new Album("Chập Chờn", "Dương Domic", R.drawable.duongdomic,
                "artist", "Dương Domic"));

        albums.add(new Album("Trình", "HIEUTHUHAI", R.drawable.hieuthuhai,
                "artist", "HIEUTHUHAI"));
        albums.add(new Album("Gã Săn Cá", "Lâm Bảo Ngọc", R.drawable.lambaongoc,
                "artist", "Lâm Bảo Ngọc"));

        rv.setAdapter(new AlbumAdapter(getContext(), albums));

        return view;
    }
}
