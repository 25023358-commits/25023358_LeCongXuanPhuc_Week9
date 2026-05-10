# 📌 HƯỚNG DẪN SỬ DỤNG HỆ THỐNG ĐẤU GIÁ (Phiên bản Đơn giản)

## 🎯 Các chức năng bắt buộc đã implement

✅ **Quản lý người dùng**
- Bidder (người đấu giá) với balance
- Seller (người bán)
- Admin (quản trị viên)
- Đăng ký/Đăng nhập

✅ **Quản lý sản phẩm**
- Thêm sản phẩm (Electronics, Art, Vehicle)
- Lấy thông tin sản phẩm
- Hiển thị danh sách

✅ **Tham gia đấu giá**
- Đặt giá hợp lệ
- Cập nhật người trả giá cao nhất
- Kiểm tra giá hợp lệ

✅ **Xử lý lỗi**
- Item không tồn tại
- Giá thấp hơn giá hiện tại
- Bid không hợp lệ

✅ **Observer Pattern**
- Thông báo tự động cho người theo dõi

## 🚀 Cách chạy

### Chạy demo đơn giản:
```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.auction.app.MainSimplified"
```

### Chạy tests:
```bash
mvn test
```

### Chạy server (tùy chọn):
```bash
mvn exec:java -Dexec.mainClass="com.auction.server.AuctionServer"
```

## 📝 Ví dụ sử dụng (Code)

```java
// Khởi tạo
AuctionManager manager = new AuctionManager();

// Tạo sản phẩm
Item laptop = ItemFactory.createItem("electronics", "L01", "Laptop", 1000.0, 12);
manager.addItem(laptop);

// Tạo người dùng
Bidder alice = new Bidder("B1", "Alice", 5000.0);
manager.addBidder(alice);
manager.addObserver(alice);

// Đặt giá
manager.placeBid("L01", "Alice", 1100.0);  // ✓ Thành công
manager.placeBid("L01", "Bob", 1050.0);    // ❌ Thất bại (thấp hơn)

// Xem lịch sử
manager.printTransactionHistory("L01");
```

## 🎓 Design Patterns sử dụng

1. **Singleton**: AuthServiceSimplified (instance duy nhất)
2. **Factory**: ItemFactory (tạo các loại Item khác nhau)
3. **Observer**: Bidder implements BidObserver (thông báo tự động)
4. **Template Method**: Item.updateHighestBid()

## 💡 Hướng mở rộng (Advanced)

- **Auto-bidding**: Thêm AutoBidder class
- **Anti-sniping**: Gia hạn thời gian trước deadline
- **Realtime charts**: JavaFX LineChart
- **Database**: SQLite integration
- **Networking**: Client-Server socket

## ✅ Yêu cầu bài tập lớn

- [x] OOP (Inheritance, Polymorphism, Encapsulation, Abstraction)
- [x] Design Patterns (Singleton, Factory, Observer)
- [x] Concurrent (Thread-safe collections)
- [x] Exception Handling
- [x] Unit Tests (JUnit)
- [x] Maven build
- [x] Git version control
_______Bố cục triển khai của các package______
|-com.auction.app: Chứa lớp chính của ứng dụng (Main.java) để khởi động hệ thống.
|-com.auction.client: Xử lý logic phía client, bao gồm kết nối và giao tiếp với server (AuctionClient.java, ClientConnection.java).
|-com.auction.config: Cấu hình hệ thống, như kết nối cơ sở dữ liệu (DatabaseConfig.java).
|-com.auction.controller: Các controller cho giao diện người dùng (UI), xử lý logic điều khiển (BiddingController.java, LoginController.java).
|-com.auction.dao: Data Access Objects (DAO) để tương tác với cơ sở dữ liệu (BidTransactionDAO.java, ItemDAO.java, UserDAO.java).
|-com.auction.entity: Các lớp thực thể (entities) đại diện cho dữ liệu miền, như User, Item, BidTransaction, v.v. (Admin.java, Art.java, Bidder.java, Item.java, Seller.java, v.v.).
|-com.auction.exception: Các ngoại lệ tùy chỉnh (AuctionException.java).
|-com.auction.server: Logic phía server để quản lý đấu giá (AuctionServer.java).
|-com.auction.service: Các dịch vụ nghiệp vụ, bao gồm quản lý đấu giá, xác thực, đấu giá tự động, thông báo, v.v. (AuctionManager.java, AuthService.java, BiddingService.java, NotificationService.java, v.v.).
|-com.auction.util: Các tiện ích hỗ trợ, như trợ giúp cơ sở dữ liệu (DBHelper.java).
!-Cấu trúc Thêm:
src/test/java/com/auction/service/: Chứa các lớp kiểm thử (tests) cho các dịch vụ (AuctionManagerTest.java, AuthServiceTest.java, DefaultBiddingStrategyTest.java).
src/main/resources/: Tài nguyên như file FXML cho giao diện (bidding.fxml, login.fxml).
