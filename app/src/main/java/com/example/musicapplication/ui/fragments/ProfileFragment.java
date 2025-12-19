package com.example.musicapplication.ui.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.musicapplication.R;
import com.example.musicapplication.ui.activity.auth.LoginActivity;
import com.example.musicapplication.data.repository.AuthRepository;
import com.example.musicapplication.data.repository.PlaylistRepository;
import com.example.musicapplication.data.repository.ProfileRepository;
import com.example.musicapplication.data.repository.UserRepository;
import com.example.musicapplication.model.User;
import com.example.musicapplication.ui.activity.other.AboutActivity;
import com.example.musicapplication.ui.activity.other.PrivacyActivity;
import com.example.musicapplication.model.Playlist;
import com.example.musicapplication.player.MusicPlayer;
import com.example.musicapplication.utils.ImageLoader;
import com.example.musicapplication.utils.ToastUtils;
import com.example.musicapplication.utils.Logger;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class ProfileFragment extends Fragment {

    // Header Section
    private ImageView imgAvatar;
    private TextView tvDisplayName;
    private TextView tvEmail;
    private TextView tvFavoritesCount;
    private TextView tvPlaylistsCount;

    // Settings Section
    private TextView btnEditProfile;

    // Support Section
    private TextView btnPrivacy;
    private TextView btnAbout;

    // Logout
    private Button btnLogout;

    private AuthRepository authRepository;
    private UserRepository userRepository;
    private ProfileRepository profileRepository;
    private PlaylistRepository playlistRepository;

    // Edit Profile Variables
    private Uri selectedImageUri;
    private ImageView dialogAvatarPreview;

    // Launcher chọn ảnh
    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    if (dialogAvatarPreview != null) {
                        dialogAvatarPreview.setImageURI(uri);
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize repositories
        authRepository = new AuthRepository(requireContext());
        userRepository = new UserRepository(requireContext());
        profileRepository = new ProfileRepository(requireContext());
        playlistRepository = new PlaylistRepository(requireContext());
        initViews(view);
        setupListeners();
        loadUserInfo();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserInfo();
    }

    private void initViews(View view) {
        // Header
        imgAvatar = view.findViewById(R.id.img_avatar);
        tvDisplayName = view.findViewById(R.id.tv_display_name);
        tvEmail = view.findViewById(R.id.tv_email);
        tvFavoritesCount = view.findViewById(R.id.tv_favorites_count);
        tvPlaylistsCount = view.findViewById(R.id.tv_playlists_count);

        // Settings
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        // Support
        btnPrivacy = view.findViewById(R.id.btn_privacy);
        btnAbout = view.findViewById(R.id.btn_about);

        // Logout
        btnLogout = view.findViewById(R.id.btn_logout);
    }

    private void setupListeners() {
        // Settings Listeners
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        }

        // Support Listeners
        if (btnPrivacy != null) {
            btnPrivacy.setOnClickListener(v -> {
                // SỬA: Dùng requireContext() thay vì getContext()
                Intent intent = new Intent(requireContext(), PrivacyActivity.class);
                startActivity(intent);
            });
        }
        if (btnAbout != null) {
            btnAbout.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), AboutActivity.class);
                startActivity(intent);
            });
        }

        // Logout
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> logout());
        }
    }

    private void loadUserInfo() {
        // --- SỬA LỖI: Lấy trực tiếp FirebaseUser để có getPhotoUrl và getDisplayName ---
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            displayDefaultInfo();
            return;
        }

        // Hiển thị thông tin cơ bản ngay lập tức
        displayBasicUserInfo(currentUser);

        // Tải thêm thông tin chi tiết từ Firestore
        userRepository.getUser(currentUser.getUid(), new UserRepository.OnResultListener<User>() {
            @Override
            public void onSuccess(User user) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> displayFullUserInfo(user));
                }
            }
            @Override
            public void onError(Exception error) {
                Logger.e("Error loading detailed user data: " + error.getMessage());
            }
        });

        playlistRepository.getRealtimeUserPlaylists(new PlaylistRepository.OnResultListener<List<Playlist>>() {
            @Override
            public void onSuccess(List<Playlist> result) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (tvPlaylistsCount != null) {
                            tvPlaylistsCount.setText(String.valueOf(result.size()));
                        }
                    });
                }
            }

            @Override
            public void onError(Exception error) {
                Logger.e("Error loading playlists count: " + error.getMessage());
            }
        });
    }

    // Thay đổi tham số thành FirebaseUser (của SDK) thay vì FirebaseUserWrapper (của bạn)
    private void displayBasicUserInfo(FirebaseUser user) {
        if (tvDisplayName != null) {
            String name = user.getDisplayName();
            if (name == null || name.isEmpty()) {
                name = user.getEmail();
                if (name != null && name.contains("@")) {
                    name = name.split("@")[0];
                }
            }
            tvDisplayName.setText(name != null ? name : "User");
        }
        if (tvEmail != null) {
            tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        }
        if (imgAvatar != null) {
            if (user.getPhotoUrl() != null) {
                ImageLoader.loadCircle(getContext(), user.getPhotoUrl().toString(), imgAvatar);
            } else {
                imgAvatar.setImageResource(R.drawable.ic_profile);
            }
        }
    }

    private void displayFullUserInfo(User user) {
        if (user == null) return;

        if (tvDisplayName != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            tvDisplayName.setText(user.getDisplayName());
        }

        if (tvEmail != null && user.getEmail() != null) {
            tvEmail.setText(user.getEmail());
        }

        if (imgAvatar != null && user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
            ImageLoader.loadCircle( getContext(), user.getPhotoUrl(), imgAvatar);
        }

        if (tvFavoritesCount != null) {
            tvFavoritesCount.setText(String.valueOf(user.getFavoriteSongs() != null ? user.getFavoriteSongs().size() : 0));
        }
        if (tvPlaylistsCount != null) {
            tvPlaylistsCount.setText(String.valueOf(user.getPlaylists() != null ? user.getPlaylists().size() : 0));
        }
    }

    private void displayDefaultInfo() {
        if (tvDisplayName != null) tvDisplayName.setText("Guest User");
        if (tvEmail != null) tvEmail.setText("guest@musicapp.com");
        if (imgAvatar != null) imgAvatar.setImageResource(R.drawable.ic_profile);
        if (tvFavoritesCount != null) tvFavoritesCount.setText("0");
        if (tvPlaylistsCount != null) tvPlaylistsCount.setText("0");
    }

    // --- CHỨC NĂNG CHỈNH SỬA HỒ SƠ ---
    private void showEditProfileDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            ToastUtils.showWarning(getContext(), "Vui lòng đăng nhập");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Chỉnh sửa hồ sơ");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        dialogAvatarPreview = new ImageView(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(200, 200);
        params.bottomMargin = 30;
        dialogAvatarPreview.setLayoutParams(params);
        dialogAvatarPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);

        if (user.getPhotoUrl() != null) {
            ImageLoader.loadCircle(getContext(), user.getPhotoUrl().toString(), dialogAvatarPreview);
        } else {
            dialogAvatarPreview.setImageResource(R.drawable.ic_profile);
        }

        dialogAvatarPreview.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        TextView tvHint = new TextView(getContext());
        tvHint.setText("Chạm để đổi ảnh");
        tvHint.setTextSize(12);
        tvHint.setGravity(Gravity.CENTER);
        tvHint.setPadding(0, 0, 0, 30);

        final EditText inputName = new EditText(getContext());
        inputName.setHint("Tên hiển thị");
        inputName.setText(tvDisplayName.getText()); // Lấy tên hiện tại đang hiển thị

        layout.addView(dialogAvatarPreview);
        layout.addView(tvHint);
        layout.addView(inputName);
        builder.setView(layout);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newName = inputName.getText().toString().trim();
            if (newName.isEmpty()) {
                ToastUtils.showWarning(getContext(), "Tên không được để trống");
                return;
            }
            performUpdateProfile(newName);
        });

        builder.setNegativeButton("Hủy", (d, w) -> {
            selectedImageUri = null;
            d.cancel();
        });

        builder.show();
    }

    private void performUpdateProfile(String newName) {
        ProgressDialog pd = new ProgressDialog(getContext());
        pd.setMessage("Đang cập nhật...");
        pd.setCancelable(false);
        pd.show();

        if (selectedImageUri != null) {
            // 1. Có đổi ảnh -> Upload trước
            profileRepository.uploadProfileImage(selectedImageUri, new ProfileRepository.OnImageUploadListener() {
                @Override
                public void onSuccess(String imageUrl) {
                    // 2. Upload xong -> Cập nhật Profile (Tên + URL ảnh mới)
                    profileRepository.updateProfile(newName, imageUrl, new ProfileRepository.OnProfileUpdateListener() {
                        @Override
                        public void onSuccess() {
                            pd.dismiss();
                            ToastUtils.showSuccess(getContext(), "Cập nhật thành công!");
                            selectedImageUri = null;
                            loadUserInfo(); // Reload UI
                        }

                        @Override
                        public void onError(Exception e) {
                            pd.dismiss();
                            ToastUtils.showError(getContext(), "Lỗi cập nhật profile: " + e.getMessage());
                        }
                    });
                }

                @Override
                public void onError(Exception e) {
                    pd.dismiss();
                    ToastUtils.showError(getContext(), "Lỗi upload ảnh: " + e.getMessage());
                }
            });
        } else {
            // 3. Không đổi ảnh -> Chỉ cập nhật tên (truyền null cho photoUrl để giữ nguyên)
            profileRepository.updateProfile(newName, null, new ProfileRepository.OnProfileUpdateListener() {
                @Override
                public void onSuccess() {
                    pd.dismiss();
                    ToastUtils.showSuccess(getContext(), "Cập nhật thành công!");
                    loadUserInfo();
                }

                @Override
                public void onError(Exception e) {
                    pd.dismiss();
                    ToastUtils.showError(getContext(), "Lỗi: " + e.getMessage());
                }
            });
        }
    }

    private void logout() {
        MusicPlayer.getInstance(getContext()).stop();
        MusicPlayer.getInstance(getContext()).release();
        authRepository.logout();
        ToastUtils.showInfo(getContext(), "Đã đăng xuất");

        Intent intent = new Intent(getContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}