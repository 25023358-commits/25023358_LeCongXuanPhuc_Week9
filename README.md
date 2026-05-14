# Auction-System-Group7
# Bài tập lớn: Hệ thống đấu giá - Nhóm 7
## Thành viên
1. Lê Công Xuân Phúc - 25023358 (Nhóm trưởng)
2. Hồ Anh Tuấn - 25023384
3. Nguyễn Anh Tùng - 25023389
4. Trần Thanh Nhật - 25023348


Refactor : Chuẩn hóa OOP, chuyển tất cả thuộc tính từ Protected sang Privated để đảm bảo tính đóng gói, đồng thời fixed lỗi Sigleton cho class AuctionManager ( Thêm các hàm getter,setter) on 4/6/2026 at 6:20 AM
## Hướng dẫn chạy:
Hệ thống Đấu giá Real-time bằng JavaFX
Đây là một ứng dụng đấu giá desktop được xây dựng bằng Java, JavaFX, Maven và SQLite, cho phép nhiều người dùng tham gia vào các phiên đấu giá trong thời gian thực.
Tính năng chính
•Giao diện người dùng được xây dựng bằng JavaFX và FXML.
•Giao tiếp Client-Server qua socket TCP/IP.
•Quản lý cơ sở dữ liệu với SQLite và JDBC.
•Tối ưu hiệu năng database với Connection Pool (HikariCP).
•Mã hóa mật khẩu an toàn bằng BCrypt.
•Tải dữ liệu hiệu quả với chức năng Phân trang (Pagination).
•Hỗ trợ đấu giá tự động (Auto-bidding).
Yêu cầu hệ thống
Để cài đặt và chạy dự án, bạn cần đảm bảo đã cài đặt các phần mềm sau:
•Java Development Kit (JDK): Phiên bản 11 trở lên.(các thành viên đang sử dụng jdk 25)
•Apache Maven: Để quản lý thư viện và xây dựng dự án.
•Git: Để sao chép mã nguồn từ repository.
Hướng dẫn Cài đặt & Chạy
1. Tải mã nguồn
Mở terminal (hoặc Command Prompt/PowerShell trên Windows) và chạy lệnh sau để sao chép dự án về máy của bạn:
Shell Script
git clone <URL_REPOSITORY_GITHUB_CUA_BAN>
cd Auction-System-Group7
2. Xây dựng dự án
Sử dụng Maven để tự động tải các thư viện cần thiết (như JavaFX, HikariCP, jBCrypt,...) và biên dịch mã nguồn.
Shell Script
mvn clean install
Lệnh này sẽ tạo một file .jar thực thi trong thư mục target. File này sẽ có tên tương tự như Auction-System-Group7-1.0-SNAPSHOT.jar.
3. Chạy chương trình
Ứng dụng bao gồm hai thành phần: Server (máy chủ) và Client (máy khách). Bạn phải chạy Server trước, sau đó mới chạy các Client.
Bước 3.1: Chạy Server
Mở một cửa sổ terminal, di chuyển đến thư mục gốc của dự án và chạy lệnh sau:
Shell Script
java -jar target/Auction-System-Group7-1.0-SNAPSHOT.jar server
Lưu ý: Tên file .jar có thể khác một chút tùy theo cấu hình trong file pom.xml của bạn. Hãy kiểm tra tên chính xác trong thư mục target sau khi xây dựng dự án.
Khi bạn thấy thông báo như Server is listening on port [số port], có nghĩa là máy chủ đã sẵn sàng và đang chờ các máy khách kết nối. Hãy giữ cửa sổ terminal này mở.
Bước 3.2: Chạy Client
Mỗi người dùng sẽ sử dụng một ứng dụng Client để đăng nhập và tham gia đấu giá.
Mở một cửa sổ terminal mới (không đóng cửa sổ của server) và chạy lệnh sau:
Shell Script
java -jar target/Auction-System-Group7-1.0-SNAPSHOT.jar
Ngay sau đó, cửa sổ đăng nhập của ứng dụng sẽ hiện lên.
Bạn có thể mở nhiều cửa sổ terminal và chạy lệnh này nhiều lần để giả lập nhiều người dùng khác nhau cùng tham gia vào hệ thống đấu giá.

