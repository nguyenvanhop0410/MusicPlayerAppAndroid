package com.example.musicapplication.main;

import android.content.Intent;
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
import com.example.musicapplication.auth.LoginActivity;
import com.example.musicapplication.data.repository.AuthRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private ViewPager2 viewPager;
    private ViewPagerAdapter adapter;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check authentication
        authRepository = new AuthRepository(this);
        if (!authRepository.isLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bottomNavigation = findViewById(R.id.bottom_navigation);
        viewPager = findViewById(R.id.view_pager);

        // Setup ViewPager2 with adapter
        adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);
        
        // Disable swipe for ViewPager2
        viewPager.setUserInputEnabled(false);

        // Connect BottomNavigationView with ViewPager2
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                viewPager.setCurrentItem(0);
                return true;
            } else if (itemId == R.id.nav_discover) {
                viewPager.setCurrentItem(1);
                return true;
            } else if (itemId == R.id.nav_library) {
                viewPager.setCurrentItem(2);
                return true;
            } else if (itemId == R.id.nav_profile) {
                viewPager.setCurrentItem(3);
                return true;
            }
            return false;
        });
        
        // Sync ViewPager2 changes with BottomNavigationView
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                switch (position) {
                    case 0:
                        bottomNavigation.setSelectedItemId(R.id.nav_home);
                        break;
                    case 1:
                        bottomNavigation.setSelectedItemId(R.id.nav_discover);
                        break;
                    case 2:
                        bottomNavigation.setSelectedItemId(R.id.nav_library);
                        break;
                    case 3:
                        bottomNavigation.setSelectedItemId(R.id.nav_profile);
                        break;
                }
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Check authentication again in case user logged out
        if (!authRepository.isLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
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