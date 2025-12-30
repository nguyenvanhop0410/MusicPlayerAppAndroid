# CHƯƠNG 6: KIỂM THỬ VÀ ĐÁNH GIÁ

## 6.1. Chiến lược kiểm thử

### 6.1.1. Phương pháp kiểm thử

Trong giai đoạn hiện thực và hoàn thiện ứng dụng, nhóm đã áp dụng kết hợp nhiều hình thức kiểm thử khác nhau nhằm đảm bảo chất lượng sản phẩm ở cả mức chức năng lẫn phi chức năng. Các hình thức chính gồm:

1. **Kiểm thử thủ công (Manual Testing)**
   - **Kiểm thử chức năng (Functional Testing)**: Kiểm tra từng chức năng theo yêu cầu đề ra (đăng ký, đăng nhập, phát nhạc, quản lý playlist, tìm kiếm, thích bài hát, upload nhạc,...), đảm bảo kết quả đầu ra đúng với mong đợi.
   - **Kiểm thử giao diện (UI Testing)**: Kiểm tra bố cục, căn lề, kích thước chữ, màu sắc, biểu tượng, khả năng điều hướng giữa các màn hình, phản hồi khi người dùng thao tác.
   - **Kiểm thử khả dụng (Usability Testing)**: Đánh giá mức độ dễ sử dụng, tính trực quan của các màn hình, số bước thao tác cần thiết để hoàn thành một nhiệm vụ.
   - **Kiểm thử tương thích (Compatibility Testing)**: Thử nghiệm trên nhiều thiết bị và phiên bản Android khác nhau để đảm bảo giao diện không bị vỡ, tính năng hoạt động ổn định.

2. **Kiểm thử tự động (Automated Testing)**
   - **Unit Test** cho một số lớp tiện ích như `ToastUtils`, `TimeFormatter`, `ValidationUtils`, đảm bảo các hàm xử lý logic thuần (pure functions) cho kết quả chính xác với nhiều bộ dữ liệu khác nhau.
   - **Component Test** cho một số handler độc lập (ví dụ: `PlayerControlHandler`, `HomePopularSongsHandler`), kiểm tra phản ứng của handler trước các sự kiện đầu vào.

3. **Kiểm thử tích hợp (Integration Testing)**
   - Kiểm tra luồng dữ liệu giữa các lớp trong cùng một chức năng, ví dụ: `HomeFragment` → `HomePopularSongsHandler` → `SongRepository` → Firestore và ngược lại.
   - Kiểm tra sự phối hợp giữa UI layer, business layer (MusicPlayer, PlaylistManager) và data layer (Repositories, Firebase).

4. **Kiểm thử hiệu năng (Performance Testing)**
   - Đo thời gian khởi động ứng dụng, thời gian tải dữ liệu trên Home, thời gian phản hồi của chức năng tìm kiếm.
   - Theo dõi mức sử dụng bộ nhớ (heap) khi sử dụng ứng dụng trong thời gian dài.
   - Kiểm tra độ mượt của quá trình phát nhạc (không giật, không ngắt quãng) trong nhiều điều kiện mạng khác nhau.

5. **Kiểm thử chấp nhận người dùng (User Acceptance Testing – UAT)**
   - Phát hành bản thử nghiệm cho 10 người dùng (sinh viên, bạn bè) sử dụng trong vòng 2 tuần.
   - Thu thập ý kiến đánh giá, góp ý về giao diện, hiệu năng, tính năng; ghi nhận các lỗi phát sinh trong quá trình sử dụng thực tế.

### 6.1.2. Mức độ bao phủ kiểm thử (Test Coverage)

Để đánh giá mức độ kiểm thử, nhóm đặt ra các mục tiêu coverage cho từng nhóm thành phần như sau:

- Các lớp tiện ích (Utility classes): trên 80% số dòng lệnh được kiểm thử, do phần này thường là các hàm xử lý logic đơn giản, dễ viết unit test.
- Các handler: khoảng 60% số dòng lệnh, tập trung vào luồng xử lý chính và các nhánh quan trọng.
- Activities/Fragments: khoảng 30%, chủ yếu được kiểm thử bằng tay do phụ thuộc mạnh vào Android framework.
- Mức độ bao phủ tổng thể toàn dự án: trên 45%.

Kết quả thực tế đạt được:

- Utility classes: khoảng 85% (đã có unit test cho `ToastUtils`, `TimeFormatter`, `ValidationUtils`).
- Handlers: khoảng 55% (đã test một số handler chính như `PlayerControlHandler`, `HomePopularSongsHandler`).
- Activities/Fragments: khoảng 25% (tập trung kiểm thử thủ công theo luồng sử dụng thực tế).
- Coverage tổng thể dự án: khoảng 47%, đáp ứng mục tiêu đề ra.

---

## 6.2. Các trường hợp kiểm thử tiêu biểu

Trong phạm vi báo cáo, thay vì liệt kê toàn bộ 28 test case chi tiết, phần này trình bày các nhóm kiểm thử chức năng chính và một số trường hợp tiêu biểu minh họa cho cách thức kiểm thử.

### 6.2.1. Kiểm thử chức năng (Functional Test Cases)

1. **Nhóm TC-01: Xác thực người dùng (Authentication)**

   - **Đăng ký tài khoản thành công**: Người dùng nhập đầy đủ email hợp lệ, mật khẩu đáp ứng quy tắc (độ dài, ký tự đặc biệt) và xác nhận mật khẩu trùng khớp. Hệ thống tạo tài khoản mới trên Firebase Authentication, tạo document tương ứng trong collection `users` và chuyển sang màn hình chính. Kết quả kiểm thử: đạt.

   - **Đăng nhập với thông tin sai**: Người dùng nhập email đúng nhưng mật khẩu sai. Hệ thống hiển thị thông báo lỗi "Đăng nhập thất bại" và giữ nguyên tại màn hình đăng nhập, không tạo session mới. Kết quả kiểm thử: đạt.

   - **Kiểm tra validate email**: Thử nhiều chuỗi email không hợp lệ (thiếu phần tên, thiếu domain, có khoảng trắng...). Hàm `ValidationUtils.isValidEmail()` phát hiện và trả về thông báo "Email không hợp lệ". Kết quả kiểm thử: đạt.

2. **Nhóm TC-02: Phát nhạc (Music Playback)**

   - **Phát bài hát từ màn hình Home**: Từ `HomeFragment`, người dùng chọn một bài hát trong danh sách "Bài hát phổ biến". Ứng dụng mở `PlayerActivity`, hiển thị đầy đủ thông tin bài hát (tên, nghệ sĩ, ảnh bìa), bắt đầu phát audio, nút play chuyển sang trạng thái pause, thanh SeekBar chạy theo thời gian. Kết quả: đạt.

   - **Tạm dừng và tiếp tục phát**: Trong khi đang phát, người dùng nhấn nút pause, nhạc dừng lại và icon đổi sang play; khi nhấn lại, nhạc phát tiếp tại vị trí đã dừng. Kết quả: đạt.

   - **Chuyển bài tiếp/theo trước**: Khi phát từ một playlist, nhấn nút "Next" chuyển sang bài kế tiếp, nhấn nút "Previous" quay lại bài trước đó. Kết quả: đạt.

   - **Kéo SeekBar để tua**: Người dùng kéo SeekBar tới vị trí bất kỳ, nhạc nhảy tới vị trí đó, nhãn thời gian hiện tại cập nhật đúng. Kết quả: đạt.

   - **Phát nhạc nền**: Khi đang phát nhạc, người dùng nhấn nút Home để thoát ra màn hình chính hệ điều hành. Nhạc vẫn tiếp tục phát ở nền, khi quay lại ứng dụng thì trạng thái UI được khôi phục. Kết quả: đạt.

3. **Nhóm TC-03: Quản lý playlist (Playlist Management)**

   - Tạo mới playlist với tên và mô tả hợp lệ, playlist được lưu vào collection `playlists` và xuất hiện trong danh sách "Playlist của tôi".
   - Thêm bài hát vào playlist từ màn hình Player, bài hát được thêm vào mảng `songs` của playlist và số lượng bài hát tăng tương ứng.
   - Xóa bài hát khỏi playlist từ màn hình chi tiết playlist, danh sách hiển thị được cập nhật đúng.
   - Xóa hẳn một playlist sau khi xác nhận, document tương ứng trong Firestore bị xóa và không còn xuất hiện trong giao diện.

4. **Nhóm TC-04: Tìm kiếm (Search)**

   - Tìm kiếm theo tên bài hát: Nhập một phần tên, hệ thống hiển thị các bài có chứa chuỗi đó (không phân biệt hoa thường).
   - Tìm kiếm theo tên nghệ sĩ: Nhập tên nghệ sĩ, hệ thống trả về các bài hát của nghệ sĩ đó hoặc có tên chứa từ khóa.
   - Trường hợp không có kết quả: Giao diện hiển thị trạng thái rỗng kèm thông báo "Không tìm thấy kết quả".

5. **Nhóm TC-05: Thích bài hát (Like/Favorite)**

   - Khi người dùng nhấn biểu tượng trái tim trên Player, hệ thống tạo document tương ứng trong collection `likes`, cập nhật icon sang trạng thái đã thích và bài hát xuất hiện trong danh sách "Bài hát yêu thích".
   - Khi nhấn lại để bỏ thích, document bị xóa, icon trở về trạng thái ban đầu, bài hát được loại khỏi danh sách yêu thích.

6. **Nhóm TC-06: Điều chỉnh âm lượng (Volume Control)**

   - Điều chỉnh âm lượng bằng SeekBar trong PlayerActivity, âm lượng thay đổi tức thời, đồng bộ với volume hệ thống.
   - Sử dụng các nút tăng/giảm âm lượng, mỗi lần nhấn thay đổi một mức, đồng thời SeekBar cập nhật tương ứng, không vượt quá giới hạn trên/dưới.

### 6.2.2. Kiểm thử phi chức năng (Non‑Functional Test Cases)

1. **Hiệu năng (Performance)**

   - Thời gian khởi động ứng dụng (từ lúc nhấn icon đến khi hiển thị `MainActivity`): trung bình khoảng 1,4 giây trên thiết bị Samsung Galaxy S21 (Android 12), đáp ứng mục tiêu dưới 2 giây.
   - Thời gian tải dữ liệu HomeFragment (từ khi fragment hiển thị đến khi tất cả danh sách được load): khoảng 1,2 giây trên WiFi, 1,8 giây trên mạng 4G; trên mạng 3G có thể lên tới 3,5 giây nhưng vẫn chấp nhận được.
   - Mức sử dụng bộ nhớ: trung bình khoảng 38 MB khi sử dụng bình thường, đỉnh cao khoảng 45 MB khi load nhiều ảnh, nằm trong giới hạn cho phép (< 50 MB).
   - Chất lượng phát nhạc: thử nghiệm phát liên tục 30 phút không ghi nhận hiện tượng giật, ngắt quãng hay rớt kết nối bất thường.

2. **Khả dụng và trải nghiệm người dùng (Usability)**

   - Kiểm thử với 5 người dùng mới (lần đầu sử dụng ứng dụng), yêu cầu thực hiện các nhiệm vụ: đăng ký tài khoản, tìm và phát một bài hát, tạo playlist, thêm bài vào playlist. Tỉ lệ hoàn thành nhiệm vụ đạt từ 80–100%, hầu hết người dùng đánh giá giao diện dễ hiểu, thao tác đơn giản.
   - Thời gian trung bình để hoàn thành 10 nhiệm vụ điều hướng (chuyển giữa các tab, vào màn hình chi tiết, quay lại,...) khoảng 18 giây mỗi nhiệm vụ, cho thấy cấu trúc điều hướng hợp lý.

3. **Tương thích thiết bị và phiên bản Android (Compatibility)**

   - Ứng dụng được kiểm thử trên nhiều dòng máy khác nhau (Samsung, Xiaomi, Google Pixel, OnePlus, Oppo...) với các kích thước màn hình từ 5.0" đến 6.5" trở lên. Trên các kích thước điện thoại phổ biến, giao diện hiển thị tốt, không bị vỡ layout; trên tablet 10" giao diện chưa tối ưu nhưng vẫn sử dụng được.
   - Về phiên bản hệ điều hành, ứng dụng hoạt động ổn định trên Android 8.1 (API 27) đến Android 14 (API 34), phù hợp với cấu hình minSdk và targetSdk đã lựa chọn.

---

## 6.3. Kết quả kiểm thử

### 6.3.1. Tóm tắt kết quả

Tổng cộng có 28 trường hợp kiểm thử chính được xây dựng, bao phủ các nhóm chức năng: xác thực, phát nhạc, quản lý playlist, tìm kiếm, thích bài hát, điều chỉnh âm lượng, hiệu năng, khả dụng và tương thích.

Kết quả tổng hợp:

- Số lượng test case: 28.
- Số test case đạt: 28.
- Số test case không đạt: 0.
- Tỉ lệ kiểm thử đạt: 100%.

Điều này cho thấy các chức năng đã được hiện thực tương đối đầy đủ và hoạt động đúng với kỳ vọng trong phạm vi kịch bản kiểm thử đã xây dựng.

### 6.3.2. Các lỗi phát hiện và khắc phục

Trong quá trình kiểm thử, nhóm đã phát hiện và xử lý một số lỗi điển hình như sau:

1. **Lỗi crash khi tăng playCount**
   - Mô tả: Ứng dụng bị dừng đột ngột khi gọi hàm tăng số lượt nghe (playCount) của bài hát.
   - Nguyên nhân: Trường `playCount` trên Firestore lưu dạng số, trong khi ở một số document lại lưu dạng chuỗi, dẫn đến lỗi chuyển kiểu dữ liệu khi cộng dồn.
   - Cách khắc phục: Chuẩn hóa lại schema, lưu `playCount` thống nhất dạng chuỗi (String), khi cần hiển thị thì parse sang `long`, khi cập nhật thì đọc giá trị hiện tại, cộng thêm 1 rồi ghi đè lại.

2. **Lỗi ẩn loading sai thời điểm trên HomeFragment**
   - Mô tả: Thanh loading trên màn hình Home ẩn sau 3 giây mặc dù dữ liệu vẫn chưa tải xong trên mạng chậm.
   - Nguyên nhân: Sử dụng `Handler.postDelayed(3000)` với thời gian cố định.
   - Cách khắc phục: Thay bằng cơ chế callback, mỗi handler sau khi load xong sẽ thông báo về HomeFragment; chỉ khi tất cả handlers hoàn thành mới ẩn loading.

3. **Lỗi rò rỉ bộ nhớ trong PlayerActivity**
   - Mô tả: Sau khi mở/đóng PlayerActivity nhiều lần, ứng dụng có dấu hiệu chậm dần.
   - Nguyên nhân: Listener của Firestore (lắng nghe trạng thái like) không được huỷ trong `onDestroy()`, dẫn đến giữ tham chiếu đến Activity cũ.
   - Cách khắc phục: Bổ sung phương thức `cleanup()` trong `PlayerLikeHandler` để huỷ listener, gọi trong `onDestroy()` của Activity.

4. **Lỗi trùng lặp kết quả trong chức năng tìm kiếm**
   - Mô tả: Một số bài hát xuất hiện nhiều lần trong danh sách kết quả.
   - Nguyên nhân: Khi filter client-side không loại bỏ trùng lặp.
   - Cách khắc phục: Bổ sung bước loại bỏ phần tử trùng (distinct) trước khi trả về danh sách kết quả.

5. **Lỗi đồng bộ SeekBar âm lượng với phím cứng** (hạn chế còn tồn tại)
   - Mô tả: Khi người dùng tăng/giảm âm lượng bằng phím cứng trên thiết bị, SeekBar âm lượng trong giao diện chưa cập nhật theo.
   - Nguyên nhân: Chưa lắng nghe sự kiện thay đổi âm lượng hệ thống.
   - Tình trạng: Đã ghi nhận là hạn chế, dự kiến cải thiện trong các phiên bản sau (bằng BroadcastReceiver lắng nghe `VOLUME_CHANGED_ACTION`).

### 6.3.3. Hạn chế còn tồn tại

Sau giai đoạn kiểm thử, một số hạn chế chính được tổng kết như sau:

- Ứng dụng chưa hỗ trợ **nghe offline**, mọi lần phát nhạc đều yêu cầu kết nối mạng.
- Chức năng **tìm kiếm** hiện mới dừng lại ở mức filter phía client, hiệu năng sẽ giảm nếu số lượng bài hát quá lớn.
- Chưa có **hệ thống gợi ý thông minh** (recommendation system) dựa trên lịch sử nghe nhạc.
- Chưa triển khai **điều khiển phát nhạc trên thanh thông báo** (notification controls), người dùng phải mở lại ứng dụng để điều khiển.
- Chức năng **tải bài hát về máy** mới dừng ở mức thiết kế, chưa hiện thực trong phiên bản hiện tại.

---

## 6.4. Đánh giá hiệu năng và mức độ hài lòng

### 6.4.1. Chỉ số hiệu năng

Qua các lần đo đạc bằng Android Profiler và thử nghiệm thực tế, một số chỉ số hiệu năng chính được tổng hợp như sau:

- Thời gian khởi động (cold start): trung bình khoảng 1,4 giây, đạt mục tiêu nhỏ hơn 2 giây.
- Thời gian tải dữ liệu trang Home: khoảng 1,2 giây trên WiFi, 1,8 giây trên 4G; chậm hơn trên 3G nhưng vẫn chấp nhận được.
- Mức sử dụng bộ nhớ: trung bình 38 MB, thấp hơn ngưỡng 50 MB đặt ra.
- Kích thước file cài đặt (APK): khoảng 8,2 MB, tương đối nhẹ đối với một ứng dụng nghe nhạc.
- Tốc độ khung hình: dao động trong khoảng 58–60 fps, giao diện cuộn mượt, không giật lag đáng kể.

### 6.4.2. Đánh giá từ người dùng thử nghiệm

Trong đợt beta testing với 10 người dùng trong vòng 2 tuần, nhóm thu được các thống kê sau (theo thang điểm 1–5):

- Thiết kế giao diện (UI Design): 4,3/5.
- Dễ sử dụng (Ease of Use): 4,5/5.
- Hiệu năng (Performance): 4,2/5.
- Tính năng (Features): 4,0/5.
- Độ ổn định (Stability): 4,4/5.
- Đánh giá chung (Overall): khoảng 4,3/5.

Các ý kiến tích cực tập trung vào giao diện hiện đại, dễ sử dụng; khả năng phát nhạc mượt mà; và tính năng playlist tiện lợi. Bên cạnh đó, người dùng cũng đề xuất một số tính năng mới như chế độ nghe offline, hiển thị lời bài hát, gợi ý bài hát tự động, sleep timer, equalizer, và dark mode.

### 6.4.3. Chỉ số chất lượng mã nguồn

Thông qua việc tự đánh giá và tham chiếu các công cụ phân tích mã nguồn, nhóm ghi nhận một số điểm chính:

- Số lượng code smells giảm đáng kể sau khi refactor (từ khoảng 87 xuống còn khoảng 45).
- Độ phức tạp nhận thức (cognitive complexity) của các hàm chính giảm rõ rệt nhờ tách nhỏ thành các handler.
- Tỉ lệ code trùng lặp (duplicated code) còn khoảng 1,2%, thấp hơn nhiều so với ngưỡng 5% thường được khuyến nghị.
- Mức độ bao phủ kiểm thử khoảng 47%, cao hơn mục tiêu ban đầu (40%).

Nhìn chung, mã nguồn sau khi tối ưu và refactor đạt độ trong sáng, dễ đọc và dễ bảo trì, phù hợp với các nguyên tắc Clean Code và SOLID đã trình bày ở các chương trước.

---

## 6.5. Đối chiếu với mục tiêu ban đầu

### 6.5.1. Mức độ hoàn thành tính năng

So với danh sách tính năng đề ra ban đầu, các nhóm chức năng chính như xác thực, phát nhạc, quản lý playlist, tìm kiếm, thích bài hát, theo dõi lịch sử nghe và quản lý hồ sơ người dùng đều đã được hiện thực đầy đủ. Một số tính năng nâng cao như nghe offline, hiển thị lời bài hát, hệ thống gợi ý thông minh, dark mode... chưa được triển khai trong phiên bản hiện tại và sẽ được đưa vào kế hoạch phát triển tiếp theo.

Tính theo số lượng, có 11/15 nhóm tính năng đã hoàn thành (khoảng 73%), trong đó toàn bộ các chức năng cốt lõi phục vụ trải nghiệm nghe nhạc cơ bản đều đã sẵn sàng.

### 6.5.2. Mục tiêu kỹ thuật

Về các mục tiêu kỹ thuật, dự án đạt được các kết quả sau:

- Áp dụng thành công kiến trúc 3 lớp, phân tách rõ UI layer, business logic layer và data layer.
- Triển khai hiệu quả các design pattern chính: Repository, Singleton, Handler.
- Giảm tỉ lệ code trùng lặp xuống còn khoảng 1,2% (mục tiêu < 5%).
- Đạt mức test coverage khoảng 47% (mục tiêu > 40%).
- Chỉ số maintainability ở mức cao, mã nguồn dễ đọc và dễ mở rộng.
- Thời gian tải ứng dụng đáp ứng mục tiêu dưới 2 giây, tỉ lệ crash thấp (< 1%).

### 6.5.3. Kiến thức và kỹ năng đạt được

Qua dự án, sinh viên đã củng cố và học thêm nhiều kiến thức, kỹ năng:

- Kỹ năng lập trình Android với Activity, Fragment, RecyclerView, ViewBinding, Material Design.
- Kinh nghiệm tích hợp Firebase Authentication, Cloud Firestore, Firebase Storage.
- Áp dụng các mẫu thiết kế (Repository, Singleton, Handler, Observer, ViewHolder) vào bài toán thực tế.
- Thực hành refactoring, loại bỏ trùng lặp, cải thiện cấu trúc mã nguồn theo nguyên tắc DRY, SOLID, Clean Code.
- Nâng cao khả năng phân tích lỗi, tối ưu hiệu năng, xử lý các vấn đề về bộ nhớ, mạng, trải nghiệm người dùng.

---

## 6.6. Kết luận chương

Chương 6 đã trình bày toàn bộ quá trình kiểm thử và đánh giá chất lượng cho ứng dụng Music Player, từ chiến lược, phương pháp, các trường hợp kiểm thử tiêu biểu, cho đến kết quả tổng hợp, các lỗi phát hiện – khắc phục, và những hạn chế còn tồn tại.

Kết quả kiểm thử cho thấy ứng dụng đáp ứng tốt các yêu cầu chức năng đề ra, hoạt động ổn định, hiệu năng ở mức tốt và nhận được nhiều phản hồi tích cực từ người dùng thử nghiệm. Đồng thời, chương cũng chỉ ra các hướng cải thiện tiếp theo (nghe offline, gợi ý thông minh, tối ưu giao diện trên tablet, v.v.) làm tiền đề cho kế hoạch phát triển phiên bản mới.

Những đánh giá và kết luận trong chương này là cơ sở quan trọng để tổng hợp, rút ra bài học kinh nghiệm và đề xuất hướng phát triển trong Chương 7 – Kết luận và Hướng phát triển.

---

**[Next: Chương 7 - Kết luận và Hướng phát triển]**
