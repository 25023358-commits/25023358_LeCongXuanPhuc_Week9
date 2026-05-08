# Auction-System-Group7
# Bài tập lớn: Hệ thống đấu giá - Nhóm 7
## Thành viên
1. Lê Công Xuân Phúc - 25023358 (Nhóm trưởng)
2. Hồ Anh Tuấn - 25023384
3. Nguyễn Anh Tùng - 25023389
4. Trần Thanh Nhật - 25023348

## Nhật ký tiến độ
Add files via upload on 5/4/2026 at 10:16 PM
Update luồng và disign pattern on 19/4/2026 at 8:50 PM


Refactor : Chuẩn hóa OOP, chuyển tất cả thuộc tính từ Protected sang Privated để đảm bảo tính đóng gói, đồng thời fixed lỗi Sigleton cho class AuctionManager ( Thêm các hàm getter,setter) on 4/6/2026 at 6:20 AM
## Hướng dẫn chạy
- Cần cài đặt Java 17, IntelliJ IDEA...
- Chạy file Main.java để bắt đầu.

Sơ đồ kế thừa tổng thể:
                                    ┌─────────────────────────┐
                                    │                         │
                                    │       <<abstract>>      │
                                    │         Entity          │
                                    │─────────────────────────│
                                    │ - id: String            │
                                    │─────────────────────────│
                                    │ + getId(): String       │
                                    └───────────┬─────────────┘
                                                │
                        ┌───────────────────────┴───────────────────────┐
                        │                                               │
                        ▼                                               ▼
            ┌───────────────────────┐                       ┌───────────────────────┐
            │                       │                       │                       │
            │    <<abstract>>        │                       │    <<abstract>>        │
            │         Item           │                       │         User           │
            │───────────────────────│                       │───────────────────────│
            │ - name: String        │                       │ - username: String    │
            │ - startingPrice: double│                      │ - role: String        │
            │ - currentHighestBid: double│                  │───────────────────────│
            │ - highestBidderId: String│                    │ + getUsername(): String│
            │───────────────────────│                       │ + getRole(): String   │
            │ + getName(): String   │                       │ + displayProfile()    │
            │ + printInfo()         │                       │   (abstract)          │
            └───────────┬───────────┘                       └───────────┬───────────┘
                        │                                               │
        ┌───────────────┼───────────────┐                               │
        │               │               │                    ┌──────────┼──────────┐
        ▼               ▼               ▼                    ▼          ▼          ▼
┌───────────────┐ ┌───────────────┐ ┌───────────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐
│               │ │               │ │               │ │           │ │           │ │           │
│  Electronics  │ │     Art       │ │   Vehicle*    │ │  Bidder   │ │  Seller*  │ │   Admin*  │
│               │ │               │ │   (mở rộng)    │ │           │ │          │ |           |
│───────────────│ │───────────────│ │───────────────│ │───────────│ │───────────│ │───────────│
│ - warranty    │ │ - artistName  │ │ - engineCC    │ │ - balance │ │ - revenue │ │ - permission│
│   Months: int │ │   : String    │ │   : int       │ │   : double│ │   : double│ │   Level    │
│───────────────│ │───────────────│ │───────────────│ │───────────│ │───────────│ │───────────│
│ + printInfo() │ │ + printInfo() │ │ + printInfo() │ │ + display │ │ + display │ │ + display │
│               │ │               │ │               │ │   Profile()│ │   Profile()│ │   Profile()│
└───────────────┘ └───────────────┘ └───────────────┘ └─────┬─────┘ └───────────┘ └───────────┘
                                                              │
                                                              │ implements
                                                              ▼
                                                    ┌───────────────────┐
                                                    │   <<interface>>   │
                                                    │    BidObserver    │
                                                    │───────────────────│
                                                    │ + update(itemId:  │
                                                    │   String,         │
                                                    │   newBid: double) │
                                                    └───────────────────┘
