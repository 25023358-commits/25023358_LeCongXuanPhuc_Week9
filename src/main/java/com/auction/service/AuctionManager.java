package com.auction.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.atomic.AtomicBoolean;
import com.auction.entity.*;
import com.auction.service.*;

public class AuctionManager {
    // Sử dụng ConcurrentHashMap để nhiều luồng có thể truy cập các phiên đấu giá khác nhau cùng lúc
    private Map<String, Item> activeAuctions;

    // Sử dụng CopyOnWriteArrayList để an toàn khi vừa duyệt vừa thêm/xóa Observer
    private List<BidObserver> observers;
    private BiddingStrategy strategy;

    // === Các component mới ===
    private AntiSniping antiSniping;
    private List<BidTransaction> transactionHistory;
    private AutoBidder autoBidder;
    private Map<String, Bidder> bidders;

    // Services
    private NotificationService notificationService;
    private AnalyticsService analyticsService;
    private BiddingService biddingService;

    public AuctionManager() {
        activeAuctions = new ConcurrentHashMap<>();
        observers = new CopyOnWriteArrayList<>();
        strategy = new DefaultBiddingStrategy();

        // Khởi tạo các component mới
        antiSniping = AntiSniping.getInstance();
        transactionHistory = new CopyOnWriteArrayList<>();
        bidders = new ConcurrentHashMap<>();

        // Initialize services
        notificationService = new NotificationService();
        analyticsService = new AnalyticsService();
        biddingService = new BiddingService(activeAuctions, bidders, strategy, antiSniping,
                analyticsService, notificationService, autoBidder, transactionHistory);
    }

    public void setAutoBidder(AutoBidder autoBidder) {
        this.autoBidder = autoBidder;
        // Update biddingService if needed, but for simplicity, assume set before use
        biddingService = new BiddingService(activeAuctions, bidders, strategy, antiSniping,
                analyticsService, notificationService, autoBidder, transactionHistory);
    }

    public void addBidder(Bidder bidder) {
        bidders.put(bidder.getId(), bidder);
    }

    public Bidder getBidder(String bidderId) {
        return bidders.get(bidderId);
    }

    public void addObserver(BidObserver observer) {
        notificationService.addObserver(observer);
    }

    public void addItem(Item item) {
        activeAuctions.put(item.getId(), item);
    }

    public Item getItem(String itemId) {
        return activeAuctions.get(itemId);
    }

    // === Advanced: Đăng ký theo dõi item realtime ===
    public void watchItem(String itemId, Bidder bidder) {
        notificationService.watchItem(itemId, bidder);
    }

    // === Advanced: Bắt đầu đếm ngược ===
    public void startCountdown(String itemId, int seconds) {
        notificationService.startCountdown(itemId, seconds, () -> {
            System.out.println("🏆 AUCTION CLOSED for " + itemId);
            Item item = activeAuctions.get(itemId);
            if (item != null) {
                System.out.println("Winner: " + item.getHighestBidderId() + " | $" + item.getCurrentHighestBid());
            }
        });
    }

    // === CORE METHOD: Xử lý đấu giá ===
    public boolean placeBid(String itemId, String bidderId, double bidAmount) {
        return biddingService.placeBid(itemId, bidderId, bidAmount);

    }

    // === Advanced: Thống kê ===
    public void showAnalytics(String itemId) {
        analyticsService.printStats(itemId);
    }

    // === Advanced: Lấy thời gian còn lại ===
    public long getRemainingTime(String itemId) {
        return antiSniping.getRemainingSeconds(itemId);
    }

    // === Advanced: Xem lịch sử giao dịch ===
    public void printTransactionHistory(String itemId) {
        biddingService.printTransactionHistory(itemId);
    }

    // === Advanced: Xem tất cả lịch sử ===
    public void printAllTransactions() {
        biddingService.printAllTransactions();
    }

    // === Helper: Kiểm tra item còn đấu giá không ===
    public boolean isAuctionActive(String itemId) {
        return antiSniping.isAuctionActive(itemId);
    }
}
