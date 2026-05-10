package com.auction.entity;
import com.auction.entity.BidObserver;  // ← Thêm dòng này


/**
 * Bản hợp nhất: Giữ Profile/Balance của Nhật và tính năng Nhận thông báo của Tùng
 */
public class Bidder extends User implements BidObserver {
    private double balance;

    // constructor của Nhật (có ID và Username từ lớp User)
    public Bidder(String id, String username, double balance) {
        super(id, username, "BIDDER");
        this.balance = balance;
    }

    // Logic hiển thị Profile của Nhật
    @Override
    public void displayProfile() {
        System.out.println("[Bidder] " + getUsername() + " | Balance: $" + balance);
    }

    // Tính năng Observer của Tùng: Nhận thông báo tự động khi giá thay đổi
    @Override
    public void update(String itemId, double newBid, String bidderId) {
        System.out.println("🔔 [Thông báo tới " + getUsername() + "]: Vật phẩm " + itemId + " vừa được trả giá mới: $" + newBid + " bởi " + bidderId);

        if (newBid > balance) {
            System.out.println("⚠️ Cảnh báo: Số dư của bạn ($" + balance + ") không còn đủ để theo thầu!");
        }
    }

    public double getBalance() { return balance; }

    public void setBalance(double balance) { this.balance = balance; }
}
