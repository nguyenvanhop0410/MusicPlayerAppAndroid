# 📁 CẤU TRÚC DỰ ÁN MUSIC PLAYER APP

## 🎯 Tổng quan
Dự án đã được tổ chức lại theo cấu trúc rõ ràng, dễ quản lý và bảo trì.

---

## 📂 Cấu trúc thư mục

```
app/src/main/java/com/example/musicapplication/
│
├── 📁 constants/                    # Các hằng số toàn cục
│   ├── AppConstants.java            # Hằng số ứng dụng (limits, timeouts, formats)
│   ├── FirebaseConstants.java       # Tên collection và field của Firebase
│   └── IntentKeys.java              # Keys cho Intent extras
│
├── 📁 data/                         # Data layer
│   ├── repository/                  # Repositories
│   │   ├── AlbumRepository.java     # Quản lý albums
│   │   ├── AuthRepository.java      # Xác thực người dùng
│   │   ├── FavoriteRepository.java  # Quản lý bài hát yêu thích
│   │   ├── HistoryRepository.java   # Lịch sử nghe nhạc
│   │   ├── PlaylistRepository.java  # Quản lý playlists
│   │   ├── ProfileRepository.java   # Quản lý profile
│   │   ├── SearchRepository.java    # Tìm kiếm bài hát
│   │   ├── SongRepository.java      # Quản lý bài hát cơ bản
│   │   ├── SongUploadRepository.java # Upload bài hát
│   │   └── UserRepository.java      # Quản lý user
│   │
│   └── services/                    # Services
│       ├── FirebaseStorageService.java
│       └── StorageService.java
│
├── 📁 model/                        # Data models
│   ├── Album.java
│   ├── Genre.java
│   ├── History.java
│   ├── Playlist.java
│   ├── SliderItem.java
│   ├── Song.java
│   └── User.java
│
├── 📁 player/                       # Music player logic
│   ├── MusicPlayer.java             # Singleton music player
│   └── PlaylistManager.java         # Quản lý playlist phát nhạc
│
├── 📁 ui/                           # UI layer
│   ├── activity/                    # Tất cả Activities
│   │   ├── album/
│   │   │   ├── AlbumDetailActivity.java
│   │   │   └── AllAlbumsActivity.java
│   │   ├── auth/
│   │   │   ├── LoginActivity.java
│   │   │   └── RegisterActivity.java
│   │   ├── genre/
│   │   │   └── GenreDetailActivity.java
│   │   ├── main/
│   │   │   └── MainActivity.java
│   │   ├── other/
│   │   │   ├── AboutActivity.java
│   │   │   └── PrivacyActivity.java
│   │   ├── player/
│   │   │   └── PlayerActivity.java
│   │   ├── playlist/
│   │   │   ├── AddSongPlaylistActivity.java
│   │   │   └── PlaylistDetailActivity.java
│   │   └── upload/
│   │       └── UploadSongActivity.java
│   │
│   ├── adapter/                     # RecyclerView Adapters
│   │   ├── AlbumAdapter.java
│   │   ├── GenreAdapter.java
│   │   ├── PlaylistAdapter.java
│   │   ├── SliderAdapter.java
│   │   ├── SongAdapter.java
│   │   ├── SongListAdapter.java
│   │   └── ViewPagerAdapter.java
│   │
│   └── fragments/                   # Fragments
│       ├── HomeFragment.java
│       ├── LibraryFragment.java
│       ├── MiniPlayerFragment.java
│       ├── ProfileFragment.java
│       └── SearchFragment.java
│
├── 📁 utils/                        # Utility classes
│   ├── ImageLoader.java             # Load ảnh với Glide
│   ├── Logger.java                  # Logging
│   ├── NetworkUtils.java            # Kiểm tra network
│   ├── PermissionUtils.java         # Xử lý permissions
│   ├── TimeFormatter.java           # Format thời gian và số
│   ├── ToastUtils.java              # Hiển thị Toast
│   └── ValidationUtils.java         # Validate input
│
└── MusicApplication.java            # Application class
```

---

## 🔧 Các file Utility đã tạo

### 1. **ImageLoader.java**
- `load()` - Load ảnh cơ bản
- `loadRounded()` - Load ảnh góc bo tròn
- `loadCircle()` - Load ảnh tròn (avatar)
- `loadWithCallback()` - Load ảnh với callback
- `loadFromResource()` - Load từ resource

### 2. **TimeFormatter.java**
- `formatDuration()` - Format mm:ss
- `formatDurationLong()` - Format hh:mm:ss
- `formatPlayCount()` - Format 1K, 1M
- `formatCount()` - Format số lượng
- `formatTimeAgo()` - Format "2 giờ trước"

### 3. **Logger.java**
- `d()` - Debug log
- `e()` - Error log
- `i()` - Info log
- `w()` - Warning log
- `logRepositoryError()` - Log lỗi repository

### 4. **ToastUtils.java**
- `showShort()` - Toast ngắn
- `showLong()` - Toast dài
- `showError()` - Toast lỗi
- `showSuccess()` - Toast thành công
- `showWarning()` - Toast cảnh báo
- `showInfo()` - Toast thông tin

### 5. **ValidationUtils.java**
- `isValidEmail()` - Validate email
- `isValidPassword()` - Validate password
- `isValidSongTitle()` - Validate tên bài hát
- `isValidPlaylistName()` - Validate tên playlist
- `getEmailError()` - Lấy thông báo lỗi email
- `getPasswordError()` - Lấy thông báo lỗi password

### 6. **NetworkUtils.java**
- `isNetworkAvailable()` - Kiểm tra có mạng
- `isWifiConnected()` - Kiểm tra WiFi
- `isMobileDataConnected()` - Kiểm tra Mobile Data
- `getNetworkType()` - Lấy loại mạng

### 7. **PermissionUtils.java**
- `hasStoragePermission()` - Kiểm tra quyền storage
- `requestStoragePermission()` - Yêu cầu quyền storage
- `hasAudioPermission()` - Kiểm tra quyền audio
- `hasCameraPermission()` - Kiểm tra quyền camera

---

## 📋 Các file Constants

### 1. **FirebaseConstants.java**
- Collections: `COLLECTION_SONGS`, `COLLECTION_ALBUMS`, `COLLECTION_USERS`, etc.
- Fields: `FIELD_TITLE`, `FIELD_ARTIST`, `FIELD_PLAY_COUNT`, etc.
- Storage paths: `STORAGE_SONGS`, `STORAGE_IMAGES`, `STORAGE_AVATARS`

### 2. **IntentKeys.java**
- Song keys: `SONG_ID`, `SONG_TITLE`, `SONG_ARTIST`, etc.
- Album keys: `ALBUM_ID`, `ALBUM_NAME`, `ALBUM_IMAGE`, etc.
- Playlist keys: `PLAYLIST_ID`, `PLAYLIST_NAME`, etc.

### 3. **AppConstants.java**
- Query limits: `TRENDING_SONGS_LIMIT`, `NEW_SONGS_LIMIT`, etc.
- Timeouts: `SEARCH_DEBOUNCE_MS`, `SLIDER_AUTO_SCROLL_MS`, etc.
- Formats: `TIME_FORMAT`, `DATE_FORMAT`, etc.
- Validation: `MIN_PASSWORD_LENGTH`, `MAX_BIO_LENGTH`, etc.
- Messages: `ERROR_NETWORK`, `SUCCESS_UPLOAD`, etc.

---

## ✅ Lợi ích của cấu trúc mới

1. **Dễ tìm file** - Biết ngay file nằm ở đâu
2. **Dễ bảo trì** - Mỗi package có trách nhiệm rõ ràng
3. **Tái sử dụng code** - Utilities giảm code trùng lặp
4. **Nhất quán** - Constants đảm bảo không có typo
5. **Dễ mở rộng** - Thêm tính năng mới không ảnh hưởng code cũ

---

## 🚀 Repositories đã tạo

### **SongRepository.java**
- `getTrendingSongs()` - Lấy bài hát trending
- `getNewSongs()` - Lấy bài hát mới
- `getSongById()` - Lấy bài hát theo ID
- `incrementPlayCount()` - Tăng lượt nghe

### **AlbumRepository.java**
- `getAlbums()` - Lấy danh sách albums
- `getSongsByAlbum()` - Lấy bài hát theo album

### **SearchRepository.java**
- `searchSongs()` - Tìm kiếm bài hát
- `getSongsByTag()` - Tìm theo thể loại
- `removeAccent()` - Xử lý tiếng Việt không dấu

### **FavoriteRepository.java**
- `checkIsLiked()` - Kiểm tra đã yêu thích chưa
- `updateFavorite()` - Cập nhật trạng thái yêu thích
- `listenToLikedSongs()` - Lắng nghe realtime
- `getSongsByIds()` - Lấy bài hát theo IDs

### **SongUploadRepository.java**
- `uploadSong()` - Upload bài hát
- `saveSongToDatabase()` - Lưu metadata
- `deleteSong()` - Xóa bài hát
- `updateSong()` - Cập nhật thông tin

### **PlaylistRepository.java**
- `createPlaylist()` - Tạo playlist
- `getUserPlaylists()` - Lấy playlists của user
- `addSongToPlaylist()` - Thêm bài hát vào playlist
- `removeSongFromPlaylist()` - Xóa bài hát khỏi playlist

### **HistoryRepository.java**
- `addToHistory()` - Thêm vào lịch sử
- `getHistory()` - Lấy lịch sử nghe nhạc
- `clearHistory()` - Xóa lịch sử

### **AuthRepository.java**
- `login()` - Đăng nhập
- `register()` - Đăng ký
- `logout()` - Đăng xuất
- `getCurrentUser()` - Lấy user hiện tại

### **UserRepository.java**
- `getUserById()` - Lấy thông tin user
- `updateUser()` - Cập nhật thông tin
- `uploadAvatar()` - Upload avatar

### **ProfileRepository.java**
- `updateProfile()` - Cập nhật profile
- `updateAvatar()` - Cập nhật avatar
- `getUploadedSongs()` - Lấy bài hát đã upload

---

**Ngày cập nhật:** 2025-12-28

