# 📌 HƯỚNG DẪN SỬ DỤNG HỆ THỐNG ĐẤU GIÁ (Phiên bản Đơn giản)

## 📁 Cấu trúc dự án (Simplified)

```
com.auction/
├── entity/          # Các lớp dữ liệu
│   ├── Entity.java  # Base class
│   ├── User.java    # Base user
│   ├── Bidder.java  # Người đấu giá
│   ├── Seller.java  # Người bán
│   ├── Admin.java   # Quản trị
│   ├── Item.java    # Sản phẩm (abstract)
│   ├── Electronics.java
│   ├── Art.java
│   ├── Vehicle.java
│   ├── BidTransaction.java
│   ├── Message.java
│   └── Observer interfaces
├── service/         # Các service
│   ├── AuctionManagerSimplified.java    # Quản lý phiên đấu giá
│   ├── AuthServiceSimplified.java       # Xác thực người dùng
│   ├── ItemFactorySimplified.java       # Tạo sản phẩm
│   └── BiddingServiceSimplified.java    # Validation bid
├── dao/            # Database access (tùy chọn)
├── client/         # Client (tùy chọn)
├── server/         # Server (tùy chọn)
└── app/
    ├── Main.java          # Phiên bản đầy đủ
    └── MainSimplified.java # Phiên bản đơn giản
```

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

