# Ghi chú thuyết trình kỹ thuật – Music Player Android

Tài liệu này dùng để **thuyết trình**, ban đầu chia phần rõ frontend/backend, nhưng hiện tại đã **phân công lại theo từng màn hình**, mỗi người đều làm cả frontend **và** backend cho các trang mình phụ trách.
- Phần của **Lê Đức Hải** – tập trung vào các màn: Trang chủ (Home), Trình phát nhạc (Player), Nghệ sĩ/Album chi tiết, lõi phát nhạc (MusicPlayer/PlaylistManager) và các Repository liên quan.
- Phần của **Nguyễn Văn Hợp** – tập trung vào các màn: Đăng nhập/Đăng ký, Main + Bottom Navigation, Thư viện (Library), Tìm kiếm (Search), Upload bài hát, Playlist chi tiết, Hồ sơ cá nhân và nhóm Utilities/Adapters.

Các mục dưới đây vừa trình bày **lý thuyết**, vừa chỉ ra **code cụ thể** trong project để có thể mở minh họa khi bảo vệ.

> Lưu ý: Các đường dẫn file bên dưới tương đối theo cấu trúc: `app/src/main/java/com/example/musicapplication/...`

---

## 0. Phân công công việc (tóm tắt)

Hai thành viên đều tham gia cả frontend và backend, nhưng tập trung vào các nhóm màn hình khác nhau:

- **Lê Đức Hải**  
    - Phụ trách các màn **Home**, **Player**, **Nghệ sĩ/Album chi tiết**: **thiết kế và hiện thực UI** cho PlayerActivity và một phần UI Home/Nghệ sĩ-Album (layout, binding dữ liệu, xử lý sự kiện click), đồng thời xử lý luồng dữ liệu với các Repository (Song/Album/Artist/History/Playlist), tích hợp `MusicPlayer` + `PlaylistManager`, thiết kế thuật toán shuffle, debounce tìm kiếm, cơ chế completion counter.  
    - Thiết kế và hiện thực phần **lõi backend**: kiến trúc 3 lớp, các Repository chính, logic tăng lượt nghe, quản lý playlist và trạng thái phát nhạc dùng Singleton.

- **Nguyễn Văn Hợp**  
    - Phụ trách các màn **Đăng nhập/Đăng ký**, **Main + Bottom Navigation**, **Library**, **Search**, **Upload**, **Playlist chi tiết**, **Profile**: thiết kế layout, RecyclerView/Adapter, handler UI, tối ưu UX và điều hướng giữa các màn hình.  
    - Tích hợp **backend trên từng màn hình**: gọi `AuthRepository`, `PlaylistRepository`, `SearchRepository`, `SongUploadRepository`, `UserRepository`, sử dụng `ValidationUtils`, `ImageLoader`, `ToastUtils` để kiểm tra dữ liệu, upload file và hiển thị kết quả cho người dùng.

---

## 1. Phần thuyết trình – Lê Đức Hải

### 1.1. Kiến trúc 3 lớp (3-Layer Architecture)

**Lý thuyết ngắn gọn**
- Ứng dụng được thiết kế theo **3-layer architecture**:
  - **Presentation/UI Layer**: Activity, Fragment, Handlers hiển thị giao diện và nhận input.
  - **Business Logic Layer**: quản lý trạng thái phát nhạc, playlist, logic xử lý nghiệp vụ.
  - **Data Layer**: làm việc với Firebase (Firestore, Storage, Auth) thông qua các Repository.
- Ưu điểm:
  - Tách biệt rõ ràng giữa UI – Logic – Dữ liệu (Separation of Concerns).
  - Dễ bảo trì, test, thay thế backend (ví dụ sau này không dùng Firebase nữa chỉ cần đổi Data Layer).

**Code minh họa kiến trúc**
- File cấu trúc tổng quan: `PROJECT_STRUCTURE.md`.
- Các package chính:
  - `data/` – Repository và services (Firebase).
  - `player/` – MusicPlayer và PlaylistManager (core business logic).
  - `ui/` – Activity, Fragment, Adapter, Handlers (UI layer).

---

### 1.2. Repository Pattern – Tầng Data

**Lý thuyết**
- **Repository Pattern** tạo lớp trung gian giữa UI/Business và nguồn dữ liệu (Firestore).
- UI không truy cập Firestore trực tiếp mà gọi qua hàm Repository: `getTrendingSongs()`, `getUserPlaylists()`…
- Giúp:
  - Dễ thay đổi nguồn dữ liệu (Firestore → REST API) mà không ảnh hưởng UI.
  - Gom logic truy vấn/phân tích dữ liệu vào một nơi.

**Code tiêu biểu**

1. **SongRepository** – quản lý dữ liệu bài hát  
   File: `data/repository/SongRepository.java`

   ```java
   public class SongRepository {
       private static final String SONGS_COLLECTION = "songs";
       private FirebaseFirestore firestore;
       private Context context;

       public SongRepository(Context context) {
           this.context = context.getApplicationContext();
           this.firestore = FirebaseFirestore.getInstance();
       }

       // Lấy bài hát trending (nhiều lượt nghe nhất)
       public void getTrendingSongs(int limit, OnResultListener<List<Song>> listener) {
           if (!NetworkUtils.isNetworkAvailable(context)) {
               listener.onError(new Exception("Không có kết nối mạng"));
               return;
           }
           firestore.collection(SONGS_COLLECTION)
                   .orderBy("playCount", Query.Direction.DESCENDING)
                   .limit(limit)
                   .get()
                   .addOnSuccessListener(s -> parseSongs(s, listener))
                   .addOnFailureListener(listener::onError);
       }
   }
   ```

   - Kỹ thuật dùng:
     - Kiểm tra mạng bằng `NetworkUtils` trước khi gọi Firestore.
     - Sử dụng `orderBy("playCount", DESC)` → cần Composite Index.

2. **incrementPlayCount – cập nhật lượt nghe**  
   Trong `SongRepository.java`:

   ```java
   public void incrementPlayCount(String songId) {
       firestore.collection(SONGS_COLLECTION).document(songId)
               .update("playCount", FieldValue.increment(1));
   }
   ```

   - Dùng `FieldValue.increment(1)` để update **atomic** trên Firestore.

3. Các Repository khác (chỉ cần nói tên khi thuyết trình):
   - `AlbumRepository`, `ArtistRepository`, `PlaylistRepository`, `HistoryRepository`, `FavoriteRepository`, `AuthRepository`, `UserRepository`, `SearchRepository`, `SongUploadRepository`.

---

### 1.3. Firebase: Authentication, Firestore, Storage, Security Rules, Indexes

**Lý thuyết chính**
- Dùng Firebase như Backend-as-a-Service:
  - **Firebase Authentication**: đăng ký/đăng nhập email & password.
  - **Cloud Firestore**: lưu metadata bài hát, user, playlist, lịch sử.
  - **Firebase Storage**: lưu file nhạc (.mp3) và ảnh bìa.
- Thiết kế **cấu trúc collection**:
  - `users`: hồ sơ user, favorites.
  - `songs`: thông tin bài hát (`title`, `artist`, `audioUrl`, `imageUrl`, `playCount`, `uploadDate`, `tags` …).
  - `playlists`: playlist cá nhân (`name`, `ownerId`, `songIds` …).
  - `history`: lịch sử nghe theo user.

**Ví dụ truy vấn Firestore trong code**

- Lấy bài hát mới nhất: `getRecentlyAddedSongs` trong `SongRepository.java`:

  ```java
  firestore.collection(SONGS_COLLECTION)
          .orderBy("uploadDate", Query.Direction.DESCENDING)
          .limit(limit)
          .get()
          .addOnSuccessListener(s -> parseSongs(s, listener))
          .addOnFailureListener(listener::onError);
  ```

- Lấy realtime history trong `HistoryRepository` (không copy đầy đủ, chỉ giải thích):
  - Dùng `addSnapshotListener()` để lắng nghe thay đổi trong collection `users/{uid}/history`.

- **Security Rules & Indexes**: thể hiện trong phần thiết kế Firestore (trình bày trong báo cáo, khi thuyết trình nêu lý thuyết + dẫn tới các truy vấn trên).

---

### 1.4. Singleton Pattern – MusicPlayer & PlaylistManager

#### 1.4.1. MusicPlayer – quản lý MediaPlayer toàn app

**Lý thuyết**
- Chỉ nên có **một** đối tượng phát nhạc (`MediaPlayer`) trong toàn bộ ứng dụng → dùng **Singleton**.
- Tránh:
  - Xung đột âm thanh khi nhiều Activity cùng tạo MediaPlayer.
  - Rò rỉ bộ nhớ khi Activity bị hủy mà MediaPlayer vẫn giữ Context cũ.

**Code** – file `player/MusicPlayer.java`

```java
public class MusicPlayer {
    private static MusicPlayer instance;
    private final MediaPlayer mediaPlayer;
    private final Context ctx;

    private MusicPlayer(Context context) {
        ctx = context.getApplicationContext();
        mediaPlayer = new MediaPlayer();
        // Cấu hình AudioAttributes...
    }

    public static synchronized MusicPlayer getInstance(Context context) {
        if (instance == null) instance = new MusicPlayer(context);
        return instance;
    }
}
```

**Một số hàm quan trọng**

- Hàm `play(String uri)` – xử lý logic phát nhạc, set datasource, `prepareAsync()`, bắt sự kiện `onPrepared`, `onCompletion`:

```java
public void play(String uri) {
    try {
        if (uri == null || uri.isEmpty()) { Logger.e("URI is null/empty"); return; }
        if (isPreparing) return;
        if (uri.equals(currentUri) && mediaPlayer.isPlaying()) { return; }

        mediaPlayer.reset();
        isPreparing = true;
        isPrepared = false;
        currentUri = uri;

        Uri audioUri = Uri.parse(uri);
        mediaPlayer.setDataSource(ctx, audioUri);

        // Cập nhật trạng thái lặp lại
        mediaPlayer.setLooping(isRepeatEnabled);

        mediaPlayer.setOnPreparedListener(mp -> {
            isPreparing = false;
            isPrepared = true;
            mp.start();
            ToastUtils.showInfo(ctx, "Đang phát: " + getSongTitle(uri));
        });

        mediaPlayer.setOnCompletionListener(mp -> {
            if (!mediaPlayer.isLooping()) {
                notifyCompletion();
            }
        });

        mediaPlayer.prepareAsync();
    } catch (Exception e) {
        isPreparing = false;
        Logger.e("Exception: " + e.getMessage());
        ToastUtils.showError(ctx, "Lỗi phát " + getSongTitle(uri));
    }
}
```

- Hàm quản lý listener cho MiniPlayer, nhiều màn hình cùng lắng nghe trạng thái:

```java
private final List<OnCompletionListener> listeners = new ArrayList<>();

public interface OnCompletionListener {
    void onCompletion();
    void onNextSong(Song song);
    void onPreviousSong(Song song);
}

public void addListener(OnCompletionListener listener) {
    if (!listeners.contains(listener)) {
        listeners.add(listener);
    }
}

private void notifyCompletion() {
    for (OnCompletionListener listener : listeners) {
        listener.onCompletion();
    }
}
```

#### 1.4.2. PlaylistManager – quản lý danh sách phát

- File: `player/PlaylistManager.java`

```java
public class PlaylistManager {
    private static PlaylistManager instance;
    private List<Song> playlist;
    private int currentPosition = 0;

    private PlaylistManager() { playlist = new ArrayList<>(); }

    public static synchronized PlaylistManager getInstance() {
        if (instance == null) { instance = new PlaylistManager(); }
        return instance;
    }

    public void setPlaylist(List<Song> songs, int position) {
        this.playlist = new ArrayList<>(songs);
        this.currentPosition = position;
    }

    public Song getSongAt(int index) { ... }
}
```

- Được dùng ở `HomeFragment` và `ArtistDetailActivity` để thiết lập playlist trước khi mở `PlayerActivity`.

---

### 1.5. Handler Pattern – Tách nhỏ logic phức tạp

#### 1.5.1. PlayerActivity sử dụng 8 Handler

**Lý thuyết**
- Trước khi refactor: `PlayerActivity` ~500 dòng code, chứa toàn bộ logic UI + business → "God Object".
- Sau khi refactor: tách thành nhiều handler, mỗi handler xử lý **một nhiệm vụ**:
  - Điều khiển phát nhạc, seekbar, like, volume, ảnh bìa, playlist, chia sẻ, tải xuống.

**Code PlayerActivity** – file `ui/activity/player/PlayerActivity.java`

```java
public class PlayerActivity extends AppCompatActivity {
    private MusicPlayer player;

    // Khai báo các Handlers
    private PlayerControlHandler controlHandler;
    private PlayerSeekBarHandler seekBarHandler;
    private PlayerImageHandler imageHandler;
    private PlayerLikeHandler likeHandler;
    private PlayerPlaylistHandler playlistHandler;
    private PlayerDownloadHandler downloadHandler;
    private PlayerShareHandler shareHandler;
    private PlayerVolumeHandler volumeHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        initDataAndRepos();
        initViews();
        initHandlers();
        setupPlayer();
    }

    private void initHandlers() {
        controlHandler = new PlayerControlHandler(this, player);
        seekBarHandler = new PlayerSeekBarHandler(this, player);
        imageHandler = new PlayerImageHandler(this);
        likeHandler = new PlayerLikeHandler(this, new FavoriteRepository(this));
        playlistHandler = new PlayerPlaylistHandler(this, new PlaylistRepository(this));
        downloadHandler = new PlayerDownloadHandler(this);
        volumeHandler = new PlayerVolumeHandler(this);
        shareHandler = new PlayerShareHandler(this);
    }
}
```

- `PlayerControlHandler` (ví dụ) – file `ui/activity/player/handlers/PlayerControlHandler.java` (tóm tắt):
  - Nhận reference tới `PlayerActivity` + `MusicPlayer`.
  - Gắn `OnClickListener` cho nút Play/Pause/Next/Previous.
  - Gọi `player.playNext()`, `player.playPrevious()`, `activity.updatePlayPauseUI(...)`.

#### 1.5.2. HomeFragment với các Handler Home*

- File: `ui/fragments/home/HomeFragment.java`

```java
private void initHandlers(View view) {
    sliderHandler = new HomeSliderHandler(view);
    albumsHandler = new HomeAlbumsHandler(getContext(), view, albumRepository, songRepository);
    artistsHandler = new HomeArtistsHandler(getContext(), view, artistRepository, this::onArtistClick);
    popularHandler = new HomePopularHandler(getContext(), view, songRepository, this::onSongClick);
    newSongsHandler = new HomeNewSongsHandler(getContext(), view, songRepository, this::onSongClick);
    profileHandler = new HomeProfileHandler(getContext(), view, userRepository);
}
```

- Cơ chế **Completion Counter** để ẩn progress bar đúng lúc:

```java
private void loadData() {
    if (!authRepository.isLoggedIn()) { setLoading(false); return; }
    setLoading(true);

    final int[] completedHandlers = {0};
    final int totalHandlers = 4; // albums, artists, popular, newSongs

    Runnable checkCompletion = () -> {
        completedHandlers[0]++;
        if (completedHandlers[0] >= totalHandlers) {
            setLoading(false);
        }
    };

    albumsHandler.loadData(checkCompletion);
    artistsHandler.loadData(checkCompletion);
    popularHandler.loadData(checkCompletion);
    newSongsHandler.loadData(checkCompletion);
    profileHandler.loadUserProfile();
}
```

- Đây là ví dụ **áp dụng kỹ thuật bất đồng bộ + callback** thay cho `postDelayed()` cố định.

---

### 1.6. Luồng phát nhạc từ Home → Player (Sequence)

**Bước 1 – User chọn bài trên Home**  
Trong `HomeFragment`:

```java
private void onSongClick(Song song, int position, List<Song> playlist) {
    PlaylistManager.getInstance().setPlaylist(playlist, position);
    MusicPlayer.getInstance(getContext()).setPlaylist(playlist, position);
    if (song.isOnline() && song.getId() != null) songRepository.incrementPlayCount(song.getId());

    Intent intent = new Intent(getContext(), PlayerActivity.class);
    intent.putExtra("songId", song.getId());
    intent.putExtra("title", song.getTitle());
    intent.putExtra("artist", song.getArtist());
    intent.putExtra("imageUrl", song.getImageUrl());
    intent.putExtra("audioUrl", song.getAudioUrl());
    intent.putExtra("songIndex", position);
    intent.putExtra("isOnline", song.isOnline());
    startActivity(intent);
}
```

**Bước 2 – PlayerActivity nhận dữ liệu và phát nhạc**

```java
private void setupPlayer() {
    currentAudioUrl = getIntent().getStringExtra("audioUrl");
    currentSongId = getIntent().getStringExtra("songId");
    currentSongTitle = getIntent().getStringExtra("title");
    currentSongArtist = getIntent().getStringExtra("artist");

    updateSongInfoUI(currentSongTitle, currentSongArtist);
    imageHandler.loadCoverImage(isOnline, imageUrl, currentAudioUrl);

    List<Song> playlist = PlaylistManager.getInstance().getPlaylist();
    int songIndex = getIntent().getIntExtra("songIndex", 0);
    if (playlist == null) playlist = new ArrayList<>();
    player.setPlaylist(playlist, songIndex);

    setupPlayerListeners();

    if (currentAudioUrl != null && !currentAudioUrl.isEmpty()) {
        if (currentAudioUrl.equals(player.getCurrentUri()) && player.isPlaying()) {
            updatePlayPauseUI(true);
            seekBarHandler.startUpdating();
        } else {
            playMusic(currentAudioUrl);
        }
    }
}
```

---

## 2. Phần thuyết trình – Nguyễn Văn Hợp

### 2.1. Thiết kế UI/UX theo Material Design

**Lý thuyết**
- Mục tiêu giao diện:
  - Đơn giản, hiện đại, tập trung vào **ảnh bìa + tiêu đề bài hát**.
  - Đồng bộ màu sắc (primary color), icon, typography theo **Material Design**.
- Thành phần chính:
  - `BottomNavigationView` – điều hướng giữa Home, Search, Library.
  - `RecyclerView + CardView` – hiển thị danh sách bài hát, album, playlist.
  - `CollapsingToolbarLayout` – màn hình chi tiết nghệ sĩ/album.
  - `MiniPlayerFragment` – trình phát thu nhỏ ở đáy màn hình.

**Code minh họa**
- `ui/activity/main/MainActivity.java` – chứa `BottomNavigationView` + `NavHost` / `FragmentContainerView`.
- `ui/fragments/home/fragment_home.xml` – bố cục trang chủ: slider, danh sách album, nghệ sĩ, bài hát phổ biến, bài mới.

Khi thuyết trình có thể mở layout XML và chỉ ra các component Material.

---

### 2.2. RecyclerView, Adapter và ViewHolder Pattern

**Lý thuyết**
- `RecyclerView` thay thế `ListView` để hiển thị danh sách lớn:
  - Tái sử dụng View (recycle) → cuộn mượt hơn.
  - Tách rõ trách nhiệm giữa **Adapter** và **ViewHolder**.
- Mẫu **ViewHolder Pattern**:
  - Giữ sẵn tham chiếu tới các view con (TextView, ImageView) để không phải gọi `findViewById()` nhiều lần.

**Code minh họa** (ví dụ `SongAdapter` – rút gọn)

```java
public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
    private List<Song> songs;
    private final OnSongClickListener listener;

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.bind(song);
    }

    class SongViewHolder extends RecyclerView.ViewHolder {
        TextView title, artist;
        ImageView cover;

        SongViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_title);
            artist = itemView.findViewById(R.id.tv_artist);
            cover = itemView.findViewById(R.id.iv_cover);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onSongClick(songs.get(pos), pos, songs);
                }
            });
        }

        void bind(Song song) {
            title.setText(song.getTitle());
            artist.setText(song.getArtist());
            ImageLoader.load(itemView.getContext(), song.getImageUrl(), cover);
        }
    }
}
```

- Điểm nhấn khi trình bày:
  - Callback `OnSongClickListener` không xử lý phát nhạc trực tiếp, mà chuyển về Fragment/Activity → tách logic UI và business.

---

### 2.3. Lớp tiện ích (Utility Classes) – DRY & Clean Code

**Mục đích**
- Tránh lặp code ở nhiều nơi (hiển thị Toast, kiểm tra mạng, định dạng thời gian…).
- Tập trung cấu hình chung (placeholder ảnh, format thời gian) vào một chỗ.

#### 2.3.1. ImageLoader – bọc thư viện Glide

File: `utils/ImageLoader.java`

```java
public class ImageLoader {
    public static void load(Context context, String url, ImageView imageView) {
        Glide.with(context)
            .load(url)
            .placeholder(R.drawable.ic_music)
            .error(R.drawable.ic_music)
            .centerCrop()
            .into(imageView);
    }

    public static void loadRounded(Context context, String url, ImageView imageView, int radiusDp) {
        Glide.with(context)
            .load(url)
            .placeholder(R.drawable.ic_music)
            .error(R.drawable.ic_music)
            .transform(new CenterCrop(), new RoundedCorners(radiusDp))
            .into(imageView);
    }

    public static void loadCircle(Context context, String url, ImageView imageView) {
        Glide.with(context)
            .load(url)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .circleCrop()
            .into(imageView);
    }
}
```

- Kỹ thuật dùng:
  - Glide cho caching hình ảnh (memory + disk cache).
  - Một nơi duy nhất cấu hình placeholder/error → sau này đổi icon chỉ cần sửa ở đây.

#### 2.3.2. TimeFormatter – định dạng thời gian, lượt nghe

File: `utils/TimeFormatter.java`

```java
public class TimeFormatter {
    public static String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    public static String formatPlayCount(int count) {
        if (count < 1000) return String.valueOf(count);
        if (count < 1000000) return String.format(Locale.US, "%.1fK", count / 1000.0);
        return String.format(Locale.US, "%.1fM", count / 1000000.0);
    }
}
```

- Dùng trong UI để hiển thị **thời lượng bài hát** và **lượt nghe** theo dạng K/M.

#### 2.3.3. ValidationUtils – validate input

File: `utils/ValidationUtils.java`

```java
public static boolean isValidEmail(String email) {
    return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
}

public static String getPasswordError(String password) {
    if (TextUtils.isEmpty(password)) {
        return "Mật khẩu không được để trống";
    }
    if (password.length() < AppConstants.MIN_PASSWORD_LENGTH) {
        return "Mật khẩu phải có ít nhất " + AppConstants.MIN_PASSWORD_LENGTH + " ký tự";
    }
    return null;
}
```

- Dùng tại `LoginActivity`, `RegisterActivity`, `UploadSongActivity` để báo lỗi thân thiện cho người dùng.

(Trong project còn `NetworkUtils`, `ToastUtils`, `Logger` – có thể nêu tên và vai trò khi trình bày, không cần đọc code hết.)

---

### 2.4. Handler Pattern ở tầng UI (Home, Library, Upload)

**Lý thuyết**
- Mục tiêu: tránh Fragment/Activity quá dài, gom logic theo từng "khối tính năng" UI.

**Ví dụ – HomeFragment**

- `HomeAlbumsHandler`: load danh sách album, gắn Adapter, xử lý click album.
- `HomeArtistsHandler`: load nghệ sĩ, xử lý click mở `ArtistDetailActivity`.
- `HomePopularHandler`: lấy `getTrendingSongs()` từ `SongRepository`, hiển thị list.
- `HomeNewSongsHandler`: lấy `getRecentlyAddedSongs()`.
- `HomeProfileHandler`: hiển thị avatar + tên user.

Trong `HomeFragment`, chỉ cần khởi tạo và gọi `loadData()` như đã minh họa ở mục 1.5.2.

**Ví dụ – UploadSongActivity Handlers**
- `UploadFilePickerHandler`: chọn file audio & ảnh từ bộ nhớ.
- `UploadMetadataHandler`: thu thập thông tin nhập từ người dùng (tên bài, nghệ sĩ, thể loại).
- `UploadFirebaseHandler`: upload file lên Firebase Storage + ghi metadata vào Firestore.
- `UploadValidationHandler`: dùng `ValidationUtils` để kiểm tra input trước khi upload.

---

### 2.5. Luồng UI tổng thể khi sử dụng ứng dụng

Khi thuyết trình, Lê Đức Hải có thể mô tả **user journey**:

1. **Đăng nhập / Đăng ký**  
   - `LoginActivity` / `RegisterActivity` dùng `AuthRepository` + `ValidationUtils`.
2. **MainActivity + Bottom Navigation**  
   - Chứa Home, Search, Library, Profile.
3. **HomeFragment**  
   - Slider banner → `HomeSliderHandler`.
   - Albums → mở `AlbumDetailActivity`.
   - Artists → mở `ArtistDetailActivity`.
   - Popular/New Songs → mở `PlayerActivity` và phát nhạc.
4. **MiniPlayer** luôn hiển thị phía dưới (nếu đang phát nhạc) để user điều khiển nhanh.
5. **LibraryFragment**  
   - Playlist cá nhân, Liked songs, History – dùng các handler tương ứng.

Khi nói, có thể vừa mô tả vừa bấm demo trên app.

---

## 3. Gợi ý cách dùng tài liệu này khi thuyết trình

- **Lê Đức Hải** (Backend lõi + Home/Player/Nghệ sĩ-Album):
    - Tập trung vào các mục: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6.  
        (Kiến trúc 3 lớp, Repository, Firebase, MusicPlayer/PlaylistManager, Handler ở PlayerActivity, luồng Home → Player.)
    - Khi giảng giải, mở các file: `MusicPlayer.java`, `PlaylistManager.java`, `SongRepository.java`, `HomeFragment.java`, một handler tiêu biểu của `PlayerActivity`.

- **Nguyễn Văn Hợp** (UI/UX + Auth/Main/Library/Search/Upload/Profile):
    - Tập trung vào mục: 2.1, 2.2, 2.3, 2.4, 2.5.  
        (Thiết kế UI/UX, RecyclerView/Adapter, các lớp tiện ích, handler UI ở Home/Library/Upload, luồng sử dụng ứng dụng.)
    - Mở các file: một `Adapter`, `ImageLoader.java`, `TimeFormatter.java`, `ValidationUtils.java`, layout `fragment_home.xml`, các handler Home/Library/Upload, `LoginActivity`, `MainActivity`.

- Cả hai có thể kết luận bằng việc nhấn mạnh:
    - Ứng dụng vừa đảm bảo **chức năng** (playback, playlist, search, upload, favorite, history) vừa được thiết kế với **kiến trúc sạch, dễ mở rộng**, và mỗi người đều tham gia cả frontend lẫn backend trên các màn hình mình phụ trách.
