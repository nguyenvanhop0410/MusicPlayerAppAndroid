# CHƯƠNG 4: TRIỂN KHAI HỆ THỐNG

## 4.1. Môi trường và cấu hình dự án

### 4.1.1. Môi trường phát triển

Chương này trình bày quá trình triển khai hệ thống ứng dụng nghe nhạc trên nền tảng Android, từ thiết lập môi trường phát triển, cấu hình dự án cho đến hiện thực các tầng dữ liệu, nghiệp vụ và giao diện người dùng. Môi trường phát triển sử dụng Android Studio phiên bản Hedgehog 2023.1.1 Patch 2 với bộ JRE 17.0.7 và Kotlin 1.9.0, kết hợp Android SDK 34 (Android 14) và bộ công cụ build 34.0.0. Hệ thống xây dựng dựa trên Gradle 8.2 cùng Android Gradle Plugin 8.2.0, bảo đảm tương thích với các thư viện AndroidX hiện đại.

Nền tảng backend được cung cấp bởi Firebase. Ứng dụng được cấu hình như một dự án trên Firebase Console với Project ID riêng và đặt tại vùng us-central, cho phép sử dụng đồng bộ các dịch vụ Firebase Authentication, Cloud Firestore và Cloud Storage. Các thư viện Firebase được quản lý thông qua Firebase BOM, giúp đồng bộ phiên bản giữa nhiều module khác nhau.

### 4.1.2. Cấu hình Gradle và quản lý phụ thuộc

Cấu hình ở cấp dự án chỉ định hai kho lưu trữ chính là Google và Maven Central, đồng thời khai báo plugin Android Application và Google Services. Ở cấp mô-đun, tệp `app/build.gradle` định nghĩa không gian tên `com.example.musicapplication`, thiết lập các tham số `compileSdk`, `minSdk`, `targetSdk`, phiên bản ứng dụng và bộ chạy kiểm thử. Đồng thời, dự án bật cả `viewBinding` và `dataBinding` để đơn giản hóa việc tương tác với layout và tránh lỗi cast thủ công.

Các phụ thuộc cốt lõi bao gồm AndroidX AppCompat, Material Components, Activity, ConstraintLayout và CardView để xây dựng giao diện hiện đại; Firebase Authentication, Firestore và Storage để xác thực, lưu trữ dữ liệu và tệp đa phương tiện; Glide để tải và hiển thị ảnh bìa album; cùng bộ thư viện kiểm thử JUnit và Espresso. Toàn bộ số phiên bản được trích xuất sang tệp `gradle/libs.versions.toml`, đóng vai trò một phiên bản catalog, giúp tách biệt cấu hình phiên bản khỏi tệp build và tạo điều kiện thuận lợi cho việc nâng cấp thư viện.

### 4.1.3. Tích hợp Firebase và cấu hình AndroidManifest

Để kết nối ứng dụng với Firebase, người phát triển tạo dự án trên Firebase Console, đăng ký ứng dụng Android với package name `com.example.musicapplication` và tải tệp cấu hình `google-services.json` về thư mục `app/`. Plugin Google Services được kích hoạt ở mô-đun ứng dụng để tự động hợp nhất cấu hình này vào quá trình build.

Tệp `AndroidManifest.xml` khai báo các quyền truy cập Internet, trạng thái mạng và quyền đọc, ghi bộ nhớ ngoài phục vụ tải và lưu trữ tệp âm thanh. Thẻ `application` chỉ định lớp `MusicApplication` để khởi tạo các thành phần toàn cục, đồng thời khai báo các Activity chính như `LoginActivity`, `RegisterActivity`, `MainActivity` và `PlayerActivity` kèm theo `intent-filter` cho màn hình khởi động. Cấu hình này đảm bảo ứng dụng có đầy đủ quyền và điểm vào cần thiết cho các chức năng đã thiết kế.

## 4.2. Triển khai tầng dữ liệu (Data Layer)

### 4.2.1. Các lớp mô hình

Tầng dữ liệu được xây dựng xoay quanh các lớp mô hình ánh xạ trực tiếp cấu trúc dữ liệu lưu trữ trên Firestore. Trong đó, `Song` là mô hình trung tâm, biểu diễn một bài hát với các thuộc tính như mã định danh, tiêu đề, nghệ sĩ, album, thể loại, thời lượng, đường dẫn âm thanh, đường dẫn ảnh bìa, số lượt phát và thông tin về thời điểm, người tải lên. Lớp này được khai báo triển khai `Serializable` để có thể truyền qua `Intent` giữa các Activity. Để xử lý vấn đề kiểu dữ liệu trên Firestore, trường `playCount` được lưu dưới dạng chuỗi, đi kèm các phương thức trợ giúp chuyển đổi và định dạng:

```java
public class Song implements Serializable {
    private String id;
    private String title;
    private String artist;
    private long duration;
    private String audioUrl;
    private String imageUrl;
    private String playCount;    // Lưu dưới dạng String từ Firestore
    // ... các trường và phương thức khác ...

    public long getPlayCountAsLong() {
        try {
            return Long.parseLong(playCount != null ? playCount : "0");
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String getFormattedDuration() {
        return TimeFormatter.formatDuration(duration);
    }
}
```

Bên cạnh `Song`, các lớp `Playlist`, `User`, `Album`, `Artist` và `History` được định nghĩa với cấu trúc tương tự, lưu trữ thông tin danh sách bài hát, người dùng, album, nghệ sĩ và lịch sử nghe nhạc. Chẳng hạn, `Playlist` duy trì danh sách các mã bài hát, trường `songCount` dạng chuỗi và phương thức chuyển đổi sang số nguyên, đồng thời bổ sung các dấu thời gian tạo và cập nhật để hỗ trợ sắp xếp và theo dõi thay đổi.

### 4.2.2. Repository Pattern và truy cập Firestore

Việc truy cập dữ liệu được đóng gói trong các lớp repository, tiêu biểu là `SongRepository` và `PlaylistRepository`, theo hướng tiếp cận Repository Pattern. `SongRepository` giữ tham chiếu đến `FirebaseFirestore` và `Context`, đồng thời định nghĩa các giao diện callback để trả về danh sách bài hát hoặc một bài hát đơn lẻ. Các phương thức chính bao gồm lấy danh sách bài hát thịnh hành sắp xếp theo `playCount`, lấy các bài hát mới dựa trên thời điểm tải lên, truy vấn theo nghệ sĩ, tìm kiếm theo từ khóa, lấy một bài hát theo mã định danh và lấy nhiều bài hát theo danh sách ID với xử lý phân lô do giới hạn tối đa mười ID của truy vấn `whereIn` trên Firestore.

Ví dụ rút gọn dưới đây minh họa cách `SongRepository` kết hợp kiểm tra mạng và truy vấn Firestore để lấy danh sách bài hát thịnh hành:

```java
public void getTrendingSongs(int limit, SongsCallback callback) {
    if (!NetworkUtils.isNetworkAvailable(context)) {
        callback.onError("Không có kết nối mạng");
        return;
    }
    firestore.collection("songs")
             .orderBy("playCount", Query.Direction.DESCENDING)
             .limit(limit)
             .get()
             .addOnSuccessListener(snapshot ->
                 callback.onSuccess(convertToSongList(snapshot)))
             .addOnFailureListener(e -> callback.onError(e.getMessage()));
}
```

Các phương thức còn lại tái sử dụng cùng một hàm trợ giúp `convertToSongList`, chuyển từng `DocumentSnapshot` thành đối tượng `Song`, đặt lại thuộc tính `id` từ mã tài liệu và trả về một danh sách hoàn chỉnh. Ngoài ra, `incrementPlayCount` sử dụng `FieldValue.increment` để tăng bộ đếm lượt phát một cách an toàn trên phía máy chủ. Đối với `PlaylistRepository`, các thao tác chính bao gồm lấy tất cả playlist của một người dùng, tạo playlist mới, thêm và xóa bài hát khỏi playlist bằng giao dịch Firestore nhằm đồng thời cập nhật danh sách bài hát, số lượng bài hát và thời điểm cập nhật, cũng như xóa toàn bộ playlist.

### 4.2.3. Xử lý lỗi mạng và giới hạn nền tảng

Để nâng cao độ tin cậy, mỗi phương thức trong các repository đều kiểm tra trạng thái kết nối thông qua lớp tiện ích `NetworkUtils` trước khi truy cập Firestore. Lớp này cung cấp các phương thức tĩnh kiểm tra kết nối và nhận diện loại mạng dựa trên `ConnectivityManager` và `NetworkInfo`. Bên cạnh đó, việc tìm kiếm bài hát không thể dựa trên full-text search của Firestore, do đó hệ thống tải toàn bộ tập kết quả thô về thiết bị và lọc client-side theo tiêu đề, nghệ sĩ hoặc album, chấp nhận đánh đổi giữa tính linh hoạt của truy vấn và chi phí truyền dữ liệu trong phạm vi ứng dụng mẫu.

## 4.3. Triển khai tầng nghiệp vụ (Business Logic Layer)

### 4.3.1. Lớp phát nhạc MusicPlayer

Tầng nghiệp vụ chịu trách nhiệm hiện thực các quy tắc xử lý liên quan đến phát nhạc và quản lý danh sách phát. Lớp `MusicPlayer` được thiết kế theo mô hình Singleton bao bọc `MediaPlayer` của Android, bảo đảm chỉ tồn tại một thực thể phát nhạc trong suốt vòng đời ứng dụng. Lớp này lưu trữ bài hát hiện tại, trạng thái phát, cho phép phát một URL âm thanh bất đồng bộ với cấu hình `AudioAttributes`, tạm dừng, tiếp tục, dừng hoàn toàn, tua đến vị trí bất kỳ, truy vấn vị trí hiện tại và tổng thời lượng. Cơ chế lắng nghe sự kiện hoàn tất bài hát được hiện thực thông qua danh sách `OnCompletionListener` tùy biến, cho phép các thành phần giao diện đăng ký nhận thông báo khi bài hát kết thúc.

### 4.3.2. Quản lý danh sách phát PlaylistManager

Lớp `PlaylistManager` cũng được cài đặt dưới dạng Singleton, quản lý danh sách các bài hát hiện thời, chỉ số bài hát đang phát, chế độ phát ngẫu nhiên và chế độ lặp (tắt, lặp cả danh sách, lặp một bài). Khi bật chế độ shuffle, lớp này lưu lại bản sao của danh sách gốc nhằm có thể phục hồi thứ tự ban đầu, đồng thời đảm bảo bài hát hiện tại luôn được giữ lại vị trí đầu tiên sau khi xáo trộn. Các phương thức `getNextSong` và `getPreviousSong` xác định bài hát kế tiếp hoặc trước đó dựa trên chế độ lặp hiện tại, trong khi `cycleRepeatMode` cho phép luân phiên giữa các chế độ OFF, ALL và ONE theo cách quen thuộc với người dùng.

### 4.3.3. Các lớp tiện ích hỗ trợ

Để tránh lặp lại mã và chuẩn hóa cách trình bày dữ liệu, hệ thống sử dụng một tập các lớp tiện ích. `ImageLoader` là một lớp bao bọc Glide, cung cấp các phương thức tĩnh để tải ảnh bìa dạng thông thường, bo góc hoặc hình tròn, cũng như tải ảnh dưới dạng `Bitmap` với callback tuỳ biến. `TimeFormatter` hiện thực các hàm định dạng thời lượng bài hát theo mm:ss, rút gọn lượt phát bằng hậu tố K hoặc M và hiển thị thời điểm tải lên dưới dạng “x phút trước” hoặc “x ngày trước”. Các lớp `ToastUtils` và `ValidationUtils` được sử dụng xuyên suốt để hiển thị thông báo thống nhất và kiểm tra dữ liệu đầu vào của người dùng.

## 4.4. Triển khai tầng giao diện (UI Layer)

### 4.4.1. MainActivity – khung chứa và điều hướng

Tầng giao diện là nơi thể hiện rõ nhất hiệu quả của việc tổ chức lại mã nguồn theo hướng module hóa. `MainActivity` đóng vai trò khung chứa chính, sử dụng `FragmentContainerView` để hiển thị bốn fragment chức năng và `BottomNavigationView` để điều hướng giữa trang chủ, thư viện, tìm kiếm và trang cá nhân. Hoạt động khởi tạo bao gồm kiểm tra trạng thái đăng nhập thông qua `FirebaseAuth`, chuyển hướng về `LoginActivity` nếu người dùng chưa xác thực, và thiết lập listener cho thanh điều hướng dưới đáy. Phương thức `loadFragment` chịu trách nhiệm thay thế fragment trong container, đồng thời tránh nạp lại fragment cùng loại để tối ưu hiệu năng.

### 4.4.2. HomeFragment và Handler Pattern

`HomeFragment` minh họa rõ ràng quá trình refactor từ một lớp đơn khối dài khoảng bốn trăm dòng sang cấu trúc gồm một fragment điều phối và năm handler riêng biệt: albums, nghệ sĩ, bài hát phổ biến, bài hát mới và slider. Fragment chỉ giữ vai trò khởi tạo và kích hoạt các handler, đồng thời quản lý trạng thái hiển thị thanh tiến trình. Mỗi handler sau khi hoàn thành việc tải dữ liệu sẽ gọi callback báo hoàn tất, để fragment ẩn giao diện loading chỉ khi toàn bộ các nguồn dữ liệu đã sẵn sàng. Đoạn mã sau thể hiện cơ chế này một cách rút gọn:

```java
private void initHandlers() {
    OnHandlerLoadCompleteListener callback = () -> {
        loadedHandlers++;
        if (loadedHandlers >= TOTAL_HANDLERS) {
            hideLoading();
        }
    };
    albumsHandler = new HomeAlbumsHandler(binding.recyclerViewAlbums,
                                          requireContext(), callback);
    popularSongsHandler = new HomePopularSongsHandler(binding.recyclerViewPopular,
                                                      requireContext(), callback);
    // Các handler còn lại được khởi tạo tương tự
}

private void loadHomeData() {
    showLoading();
    loadedHandlers = 0;
    albumsHandler.loadData();
    popularSongsHandler.loadData();
    // Gọi loadData() cho các handler còn lại
}
```

Một ví dụ tiêu biểu là `HomePopularSongsHandler`, nơi `RecyclerView` ngang được cấu hình với `SongAdapter` và dữ liệu được lấy từ `SongRepository.getTrendingSongs`. Khi dữ liệu tải thành công, handler cập nhật danh sách bài hát cho adapter và gọi callback để báo cho fragment rằng một nguồn dữ liệu đã hoàn tất; khi xảy ra lỗi, handler hiển thị thông báo thông qua `ToastUtils` nhưng vẫn gọi callback để tránh khóa giao diện người dùng.

### 4.4.3. PlayerActivity và hệ thống handler chuyên biệt

`PlayerActivity` là trường hợp điển hình cho việc áp dụng Handler Pattern trong một Activity phức tạp. Trước khi refactor, toàn bộ logic điều khiển phát nhạc, theo dõi thanh tiến trình, thích hoặc bỏ thích, điều chỉnh âm lượng, xử lý ảnh bìa, thêm vào playlist và chia sẻ bài hát đều tập trung trong một lớp duy nhất với hơn năm trăm dòng mã. Sau khi tái cấu trúc, `PlayerActivity` chủ yếu đảm nhiệm vai trò phối hợp bảy handler: `PlayerControlHandler`, `PlayerSeekBarHandler`, `PlayerLikeHandler`, `PlayerVolumeHandler`, `PlayerImageHandler`, `PlayerPlaylistHandler` và `PlayerShareHandler`. Activity khởi tạo binding, nhận `songId` từ `Intent`, tải dữ liệu bài hát thông qua `SongRepository`, cập nhật giao diện cơ bản, chuyển dữ liệu cần thiết cho từng handler và gọi tăng lượt phát. Việc dọn dẹp được thực hiện trong `onDestroy`, trong đó các handler như `PlayerSeekBarHandler` và `PlayerLikeHandler` được yêu cầu giải phóng listener và callback để tránh rò rỉ bộ nhớ.

Cấu trúc của một handler điển hình có thể quan sát qua `PlayerControlHandler`. Lớp này nhận các nút phát, kế tiếp và trước đó, đồng thời làm việc trực tiếp với hai Singleton `MusicPlayer` và `PlaylistManager`:

```java
public class PlayerControlHandler {
    private final ImageView btnPlay, btnNext, btnPrevious;
    private final MusicPlayer musicPlayer = MusicPlayer.getInstance();
    private final PlaylistManager playlistManager = PlaylistManager.getInstance();

    public PlayerControlHandler(ImageView btnPlay, ImageView btnNext,
                                ImageView btnPrevious, OnSongChangeListener listener) {
        this.btnPlay = btnPlay;
        this.btnNext = btnNext;
        this.btnPrevious = btnPrevious;
        setupListeners(listener);
    }

    private void setupListeners(OnSongChangeListener listener) {
        btnPlay.setOnClickListener(v -> togglePlayPause());
        btnNext.setOnClickListener(v -> playNext(listener));
        btnPrevious.setOnClickListener(v -> playPrevious(listener));
    }
}
```

Các handler khác được xây dựng theo nguyên tắc trách nhiệm đơn lẻ tương tự. `PlayerSeekBarHandler` điều khiển `SeekBar` và hai nhãn thời gian thông qua một `Handler` nội bộ cập nhật vị trí hiện tại mỗi giây, đồng thời cho phép người dùng tua bài. `PlayerLikeHandler` quản lý trạng thái thích bài hát trên Firestore, thiết lập listener thời gian thực cho tài liệu like của người dùng và cập nhật biểu tượng trái tim tương ứng, đồng thời sử dụng `ToastUtils` để phản hồi thao tác thành công hoặc lỗi. `PlayerVolumeHandler` ánh xạ thanh trượt âm lượng và các nút tăng giảm vào `AudioManager`, trong khi `PlayerImageHandler` sử dụng Glide và `Palette` để tải ảnh bìa, trích xuất màu sắc nổi bật và áp dụng gradient nền động cho giao diện player. `PlayerPlaylistHandler` tương tác với `PlaylistRepository` để hiển thị hộp thoại chọn playlist và thêm bài hát hiện tại vào danh sách được chọn, còn `PlayerShareHandler` xây dựng chuỗi văn bản giới thiệu bài hát và khởi chạy `Intent.ACTION_SEND` để chia sẻ qua các ứng dụng khác.

### 4.4.4. Adapters và liên kết dữ liệu với giao diện

Các adapter đóng vai trò cầu nối giữa tầng dữ liệu và giao diện danh sách. `SongAdapter` là ví dụ tiêu biểu, triển khai `RecyclerView.Adapter` với mẫu ViewHolder và một callback để xử lý sự kiện khi người dùng chọn bài hát. Nhờ sử dụng ViewBinding và lớp tiện ích `ImageLoader`, mã nguồn trở nên ngắn gọn nhưng vẫn rõ ràng về mặt ý nghĩa:

```java
class ViewHolder extends RecyclerView.ViewHolder {
    private final ItemSongBinding binding;

    ViewHolder(ItemSongBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    void bind(Song song) {
        binding.tvSongTitle.setText(song.getTitle());
        binding.tvArtist.setText(song.getArtist());
        ImageLoader.loadRounded(binding.getRoot().getContext(),
                                song.getImageUrl(), binding.imgSong, 8);
        binding.getRoot().setOnClickListener(v -> listener.onSongClick(song));
    }
}
```

`PlaylistAdapter` được xây dựng tương tự, nhưng kết hợp thêm việc tải ảnh đại diện dựa trên ảnh bìa của bài hát đầu tiên trong playlist thông qua `SongRepository`, đồng thời cung cấp hai loại callback cho sự kiện mở và xóa playlist. Nhờ vậy, toàn bộ logic hiển thị danh sách và phản hồi tương tác được tách bạch khỏi fragment hoặc activity chứa RecyclerView, góp phần giảm độ phức tạp của lớp giao diện.

## 4.5. Tóm tắt chương

Chương 4 đã trình bày một cách có hệ thống quá trình triển khai hệ thống ứng dụng nghe nhạc trên Android. Từ việc cấu hình môi trường phát triển, Gradle và Firebase, luận văn đã xây dựng một tầng dữ liệu rõ ràng với các mô hình và repository tương tác với Firestore có xét đến giới hạn nền tảng và điều kiện mạng. Trên nền tảng đó, tầng nghiệp vụ hiện thực các chức năng phát nhạc và quản lý danh sách phát thông qua hai Singleton `MusicPlayer` và `PlaylistManager`, được hỗ trợ bởi các lớp tiện ích chuyên biệt.

Điểm nhấn quan trọng của chương là cách tổ chức lại tầng giao diện dựa trên Handler Pattern. Việc tách nhỏ `HomeFragment` và đặc biệt là `PlayerActivity` thành nhiều handler có trách nhiệm đơn lẻ đã giúp giảm đáng kể số dòng mã trong mỗi lớp, đồng thời cải thiện rõ rệt khả năng đọc, kiểm thử, bảo trì và tái sử dụng. Những kỹ thuật này tạo nền tảng vững chắc cho các bước tối ưu hóa và refactor sâu hơn được trình bày ở các chương tiếp theo.
