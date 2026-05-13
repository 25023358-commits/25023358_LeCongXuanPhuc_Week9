package com.auction.entity;

public class Bidder extends User implements BidObserver {
    private double balance;

    // Constructor không tham số cho Jackson
    public Bidder() {
        super();
    }

    public Bidder(String id, String username, double balance) {
        super(id, username, "BIDDER");
        this.balance = balance;
    }

    @Override
    public void displayProfile() {
        System.out.println("[Bidder] " + getUsername() + " | Balance: $" + balance);
    }

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
