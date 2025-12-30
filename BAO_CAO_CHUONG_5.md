# CHƯƠNG 5: TỐI ƯU HÓA VÀ REFACTORING

## 5.1. Phân tích vấn đề ban đầu

Sau khi hoàn thành các chức năng cơ bản, mã nguồn của ứng dụng được rà soát lại theo góc nhìn chất lượng phần mềm. Kết quả cho thấy nhiều hạn chế về trùng lặp mã, lớp quá lớn và cách tổ chức logic, nếu không xử lý sớm sẽ gây khó khăn cho việc bảo trì và mở rộng trong tương lai.

### 5.1.1. Trùng lặp mã nguồn

Trong quá trình phân tích, nhiều đoạn mã giống nhau được lặp lại ở nhiều lớp khác nhau, vi phạm nguyên tắc **DRY (Don't Repeat Yourself)**. Các nhóm trùng lặp chính gồm:

- Thông báo `Toast`: xuất hiện hơn 50 lần với cách hiển thị gần giống nhau.
- Gọi thư viện `Glide` để tải ảnh: khoảng gần 20 vị trí với cấu hình tương tự.
- Đoạn kiểm tra kết nối mạng bằng `ConnectivityManager`: lặp lại hơn 10 lần.
- Định dạng thời gian phát nhạc (phút:giây) và định dạng số lượt nghe: xuất hiện ở nhiều adapter và activity.

Hậu quả là mã nguồn dài dòng, khó đọc và khó thay đổi đồng bộ. Khi cần đổi cách hiển thị thông báo hoặc thay đổi placeholder ảnh, lập trình viên phải sửa tay tại rất nhiều vị trí, dễ gây sai sót.

### 5.1.2. Các lớp quá lớn (God Object)

Hai lớp giao diện chính là `PlayerActivity` và `HomeFragment` có kích thước quá lớn so với chức năng của một lớp thông thường:

- `PlayerActivity` chứa hơn 500 dòng mã, kết hợp đồng thời điều khiển phát nhạc, thanh tiến trình, trạng thái thích, âm lượng, ảnh bìa, playlist và chia sẻ.
- `HomeFragment` dài khoảng 400 dòng, vừa tải dữ liệu cho nhiều danh sách, vừa cấu hình nhiều `RecyclerView`, vừa xử lý toàn bộ sự kiện nhấn.

Các lớp "God Object" này vi phạm nguyên tắc trách nhiệm đơn lẻ, khó đọc, khó kiểm thử đơn vị và rất dễ phát sinh lỗi khi chỉnh sửa.

### 5.1.3. Hạn chế trong cơ chế tải dữ liệu

Ở phiên bản ban đầu, `HomeFragment` sử dụng một khoảng thời gian chờ cố định (khoảng 3 giây) để ẩn giao diện loading, bất kể dữ liệu đã tải xong hay chưa. Cách làm này dẫn đến hai tình huống xấu:

- Nếu dữ liệu về sớm, người dùng vẫn phải chờ đủ thời gian timeout.
- Nếu mạng chậm hoặc một nguồn dữ liệu bị lỗi, loading đã ẩn nhưng giao diện vẫn trống hoặc thiếu dữ liệu.

Điều này ảnh hưởng trực tiếp đến trải nghiệm sử dụng và gây khó cho việc xử lý lỗi rõ ràng.

### 5.1.4. Thiếu các lớp tiện ích chung

Trước khi refactor, ứng dụng chưa có các lớp tiện ích dùng chung. Mỗi khi cần hiển thị thông báo, kiểm tra mạng, tải ảnh hoặc định dạng thời gian, lập trình viên phải viết lại toàn bộ đoạn mã. Điều này làm tăng số dòng code, giảm tính nhất quán và khiến việc bảo trì trở nên phức tạp.

## 5.2. Giải pháp refactoring

Để khắc phục các vấn đề trên, luận văn tiến hành refactor mã nguồn theo ba hướng chính: xây dựng các lớp tiện ích dùng chung, áp dụng mô hình handler cho các màn hình phức tạp và cải tiến cơ chế tải dữ liệu. Các thay đổi được thực hiện từng bước, bảo đảm không làm thay đổi hành vi chức năng của ứng dụng.

### 5.2.1. Xây dựng các lớp tiện ích dùng chung

Nhằm giảm trùng lặp và chuẩn hóa cách xử lý những logic phổ biến, hệ thống bổ sung một nhóm lớp tiện ích:

- **ToastUtils**: gói gọn việc hiển thị thông báo thành các phương thức tĩnh như `showSuccess`, `showError`, `showInfo`. Mỗi phương thức tự cấu hình thời lượng, màu nền và biểu tượng phù hợp. Khi cần hiển thị thông báo, lập trình viên chỉ cần gọi:

  ```java
  ToastUtils.showSuccess(context, "Đăng nhập thành công");
  ```

- **ImageLoader**: bao bọc thư viện Glide, cung cấp các phương thức `load`, `loadRounded`, `loadCircle` để tải ảnh thường, ảnh bo góc và ảnh tròn. Nhờ đó, cấu hình placeholder, ảnh lỗi và chiến lược cache được quản lý tập trung.

- **NetworkUtils**: cung cấp các hàm kiểm tra kết nối mạng và một phương thức tiện lợi `checkAndExecute`, vừa kiểm tra mạng, vừa hiển thị lỗi thông qua `ToastUtils` nếu không có kết nối.

- **TimeFormatter**: chuẩn hóa việc định dạng thời lượng bài hát dạng mm:ss, rút gọn lượt nghe (K, M) và hiển thị thời điểm tải lên dưới dạng "x phút trước" hoặc "x ngày trước".

Nhờ nhóm lớp tiện ích này, nhiều đoạn mã lặp được rút gọn thành một dòng gọi hàm, đồng thời giao diện trở nên nhất quán hơn.

### 5.2.2. Áp dụng Handler Pattern cho các màn hình phức tạp

Đối với các màn hình có nhiều chức năng, luận văn áp dụng **Handler Pattern**: tách mỗi nhóm chức năng thành một lớp handler riêng, còn activity/fragment chỉ giữ vai trò điều phối.

- Với `PlayerActivity`, mã nguồn được tách thành bảy handler chuyên biệt như `PlayerControlHandler`, `PlayerSeekBarHandler`, `PlayerLikeHandler`, `PlayerVolumeHandler`, `PlayerImageHandler`, `PlayerPlaylistHandler` và `PlayerShareHandler`. Mỗi handler chịu trách nhiệm cho một phần giao diện và logic cụ thể (phát/tạm dừng, thanh tiến trình, thích bài hát, âm lượng, ảnh bìa, thêm vào playlist, chia sẻ).

- Với `HomeFragment`, dữ liệu trang chủ được chia cho các handler như `HomeAlbumsHandler`, `HomeArtistsHandler`, `HomePopularSongsHandler`, `HomeNewSongsHandler` và `HomeSliderHandler`. Mỗi handler tự tải dữ liệu, cấu hình `RecyclerView` hoặc `ViewPager` tương ứng và báo kết quả về fragment thông qua một callback chung.

Cách tổ chức này giúp giảm đáng kể số dòng mã trong mỗi lớp, nâng cao khả năng đọc hiểu và cho phép kiểm thử riêng từng thành phần.

### 5.2.3. Cải tiến cơ chế tải dữ liệu bằng callback

Để thay thế timeout cố định, `HomeFragment` sử dụng cơ chế **đếm số nguồn dữ liệu đã tải xong**. Ý tưởng là:

- Đặt một hằng số `TOTAL_HANDLERS` biểu thị số lượng handler cần tải dữ liệu.
- Khởi tạo một biến đếm `loadedHandlers` về 0 trước khi bắt đầu tải.
- Mỗi handler, sau khi tải thành công hoặc lỗi, đều gọi `onLoadComplete()` để tăng biến đếm.
- Khi `loadedHandlers` đạt tới `TOTAL_HANDLERS`, fragment ẩn giao diện loading.

Mô hình này đảm bảo loading chỉ bị ẩn khi tất cả nguồn dữ liệu đã phản hồi, kể cả trong trường hợp có lỗi mạng. Trải nghiệm người dùng do đó chính xác và ổn định hơn so với việc dùng thời gian chờ cố định.

## 5.3. Kết quả tối ưu

### 5.3.1. Giảm số dòng mã và tỷ lệ trùng lặp

Sau khi refactor, số dòng mã và tỷ lệ trùng lặp trong các thành phần chính giảm đáng kể:

- `PlayerActivity` giảm từ khoảng 500 dòng xuống còn khoảng 150 dòng nhờ tách thành các handler.
- `HomeFragment` giảm từ khoảng 400 dòng xuống còn khoảng 150 dòng.
- Các đoạn gọi `Toast`, Glide, kiểm tra mạng và định dạng thời gian được gom lại qua các lớp tiện ích, giúp loại bỏ hàng trăm dòng mã lặp.
- Tỷ lệ mã trùng lặp toàn dự án giảm từ khoảng 8–9% xuống còn gần 1–2%, nằm trong ngưỡng tốt theo khuyến nghị thực hành.

Việc giảm số dòng mã không làm mất chức năng mà giúp dự án gọn gàng và dễ nắm bắt hơn.

### 5.3.2. Cải thiện khả năng bảo trì và kiểm thử

Các chỉ số về độ phức tạp và khả năng bảo trì được cải thiện rõ rệt:

- Độ phức tạp chu kỳ (cyclomatic complexity) của `PlayerActivity` và `HomeFragment` giảm mạnh do logic được chia nhỏ và tách khỏi các cấu trúc điều kiện lồng nhau.
- Chỉ số khả năng bảo trì (maintainability index) tăng lên mức cao nhờ số dòng giảm, hàm ngắn hơn và ít nhánh rẽ.
- Việc xuất hiện các lớp tiện ích và handler độc lập giúp dễ viết kiểm thử đơn vị hơn; tỷ lệ mã được kiểm thử tăng đáng kể so với ban đầu gần như không có test.

Nhìn chung, sau khi tối ưu, việc đọc hiểu, chỉnh sửa và mở rộng mã nguồn trở nên nhẹ nhàng hơn rất nhiều.

### 5.3.3. Nâng cao hiệu năng và trải nghiệm người dùng

Refactoring không chỉ cải thiện chất lượng mã mà còn mang lại một số lợi ích về hiệu năng và trải nghiệm:

- Thời gian hiển thị dữ liệu trang chủ giảm do loại bỏ timeout cố định; loading được ẩn ngay khi dữ liệu đã sẵn sàng.
- Việc quản lý tài nguyên tốt hơn (dọn dẹp `MediaPlayer`, hủy listener khi không cần thiết) góp phần giảm sử dụng bộ nhớ và hạn chế rò rỉ.
- Cơ chế xử lý lỗi mạng tập trung giúp giao diện phản hồi rõ ràng hơn khi xảy ra sự cố kết nối.

Những cải thiện này giúp ứng dụng hoạt động mượt mà và thân thiện hơn với người dùng cuối.

## 5.4. Các nguyên tắc và thực hành tốt được áp dụng

Quá trình tối ưu hóa và refactoring được định hướng bởi các nguyên tắc thiết kế và thực hành tốt trong phát triển phần mềm:

- **SOLID**: 
  - Trách nhiệm đơn lẻ (SRP) được thể hiện qua các handler và lớp tiện ích chỉ làm một việc rõ ràng.
  - Mở rộng mà không sửa đổi (OCP) khi có thể bổ sung handler hoặc phương thức tiện ích mới mà ít ảnh hưởng tới mã hiện tại.

- **Clean Code**:
  - Hạn chế trùng lặp (DRY) nhờ gom logic chung vào các lớp tiện ích.
  - Giữ cho hàm và lớp ngắn gọn, tên gọi rõ nghĩa, phù hợp với chức năng.
  - Tách biệt mối quan tâm (Separation of Concerns) giữa giao diện, nghiệp vụ và truy cập dữ liệu.

- **Best practices Android**:
  - Sử dụng ViewBinding để thao tác giao diện an toàn hơn so với `findViewById`.
  - Áp dụng Repository Pattern khi làm việc với Firestore.
  - Giải phóng tài nguyên trong các hàm vòng đời như `onDestroy` để tránh rò rỉ bộ nhớ.

Nhờ tuân thủ các nguyên tắc này, mã nguồn sau refactor có cấu trúc rõ ràng hơn, dễ mở rộng và dễ chuyển giao cho các nhóm phát triển khác.

## 5.5. Tóm tắt chương

Chương 5 đã trình bày quá trình tối ưu hóa và refactoring mã nguồn của ứng dụng nghe nhạc. Từ việc nhận diện các vấn đề ban đầu như trùng lặp mã, lớp quá lớn, timeout cố định và thiếu lớp tiện ích, luận văn đã đề xuất và hiện thực các giải pháp dựa trên lớp tiện ích dùng chung, Handler Pattern và cơ chế tải dữ liệu dựa trên callback.

Kết quả cho thấy số dòng mã được giảm đáng kể, mức độ trùng lặp hạ xuống, khả năng bảo trì và kiểm thử được cải thiện, đồng thời hiệu năng và trải nghiệm người dùng cũng tốt hơn. Những thay đổi này tạo nền tảng vững chắc cho việc tiếp tục mở rộng chức năng, nâng cấp giao diện và đảm bảo chất lượng dự án trong các giai đoạn phát triển tiếp theo.
