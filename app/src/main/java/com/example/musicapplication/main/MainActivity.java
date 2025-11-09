package com.example.musicapplication.main;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.musicapplication.R;
import com.example.musicapplication.adapter.ViewPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ViewPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);

        // Setup ViewPager2 with adapter
        adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Songs");
                    tab.setIcon(R.drawable.ic_music);
                    break;
                case 1:
                    tab.setText("Albums");
                    tab.setIcon(R.drawable.ic_album);
                    break;
                case 2:
                    tab.setText("Profile");
                    tab.setIcon(R.drawable.ic_profile);
                    break;
            }
        }).attach();
    }

    // Method to switch to Songs tab with album filter
    public void switchToSongsWithAlbumFilter(long albumId, String albumName) {
        // Switch to Songs tab (index 0)
        viewPager.setCurrentItem(0);

        // Wait a bit for fragment to be ready, then apply filter
        viewPager.postDelayed(() -> {
            // Get the SongsFragment from adapter
            Fragment fragment = getSupportFragmentManager().findFragmentByTag("f0"); // f0 = first tab
            if (fragment instanceof com.example.musicapplication.fragments.SongsFragment) {
                ((com.example.musicapplication.fragments.SongsFragment) fragment).applyAlbumFilter(albumId, albumName);
            }
        }, 100);
    }

    // Method to switch to Songs tab with artist filter
    public void switchToSongsWithArtistFilter(String artistName, String albumName) {
        // Switch to Songs tab (index 0)
        viewPager.setCurrentItem(0);

        // Wait a bit for fragment to be ready, then apply filter
        viewPager.postDelayed(() -> {
            // Get the SongsFragment from adapter
            Fragment fragment = getSupportFragmentManager().findFragmentByTag("f0"); // f0 = first tab
            if (fragment instanceof com.example.musicapplication.fragments.SongsFragment) {
                ((com.example.musicapplication.fragments.SongsFragment) fragment).applyArtistFilter(artistName, albumName);
            }
        }, 100);
    }
}