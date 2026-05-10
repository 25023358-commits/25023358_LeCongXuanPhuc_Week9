package src.main.java.com.auction.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import src.main.java.com.auction.entity.*;

public class BiddingService {
    private Map<String, Item> activeAuctions;
    private Map<String, Bidder> bidders;
    private BiddingStrategy strategy;
    private AntiSniping antiSniping;
    private AnalyticsService analyticsService;
    private NotificationService notificationService;
    private AutoBidder autoBidder;
    private List<BidTransaction> transactionHistory;

    public BiddingService(Map<String, Item> activeAuctions, Map<String, Bidder> bidders,
                          BiddingStrategy strategy, AntiSniping antiSniping,
                          AnalyticsService analyticsService, NotificationService notificationService,
                          AutoBidder autoBidder, List<BidTransaction> transactionHistory) {
        this.activeAuctions = activeAuctions;
        this.bidders = bidders;
        this.strategy = strategy;
        this.antiSniping = antiSniping;
        this.analyticsService = analyticsService;
        this.notificationService = notificationService;
        this.autoBidder = autoBidder;
        this.transactionHistory = transactionHistory;
    }

    public boolean placeBid(String itemId, String bidderId, double bidAmount) {
        Item item = activeAuctions.get(itemId);

        if (item == null) {
            System.out.println("Item not found: " + itemId);
            return false;
        }

        // Kiểm tra chống sniping
        if (!antiSniping.checkAndExtend(itemId)) {
            System.out.println("Auction ended for " + itemId + "! Cannot bid.");
            return false;
        }

        synchronized (item) {
            Bidder bidder = bidders.get(bidderId);
            if (bidder == null || bidder.getBalance() < bidAmount) {
                System.out.println("Bid FAILED: Insufficient balance or bidder not found.");
                return false;
            }

            if (strategy.isValidBid(item.getCurrentHighestBid(), bidAmount)) {
                boolean success = item.updateHighestBid(bidAmount, bidderId);
                if (success) {
                    System.out.println("Bid SUCCESS: " + bidderId + " -> $" + bidAmount + " on " + itemId);

                    // Ghi analytics
                    analyticsService.recordBid(itemId, bidAmount);

                    // Gửi realtime notification
                    notificationService.notifyRealtime(itemId, bidAmount, bidderId);

                    // Trigger auto-bid
                    if (autoBidder != null) {
                        autoBidder.onNewBid(itemId, bidderId, bidAmount);
                    }

                    // Đánh dấu các transaction cũ không còn thắng
                    markPreviousTransactionsAsLost(itemId);

                    // Lưu transaction
                    BidTransaction tx = new BidTransaction(itemId, bidderId, bidAmount);
                    tx.markAsWinning();
                    transactionHistory.add(tx);

                    // Notify observers
                    notificationService.notifyObservers(itemId, bidAmount, bidderId);

                    return true;
                }
            }
        }

        System.out.println("Bid FAILED for " + bidderId + ": Amount too low or invalid.");
        return false;
    }

    // Đánh dấu tất cả transaction cũ của item này là không thắng
    private void markPreviousTransactionsAsLost(String itemId) {
        for (BidTransaction tx : transactionHistory) {
            if (tx.getItemId().equals(itemId) && tx.isWinning()) {
                tx.markAsLost();  // Cần thêm method này trong BidTransaction
            }
        }
    }

    public void printTransactionHistory(String itemId) {
        System.out.println("📜 TRANSACTION HISTORY for " + itemId + ":");
        boolean found = false;
        for (BidTransaction tx : transactionHistory) {
            if (tx.getItemId().equals(itemId)) {
                tx.printInfo();
                found = true;
            }
        }
        if (!found) {
            System.out.println("   No transactions yet.");
        }
    }

    public void printAllTransactions() {
        System.out.println("📜 ALL TRANSACTIONS:");
        if (transactionHistory.isEmpty()) {
            System.out.println("   No transactions yet.");
        } else {
            for (BidTransaction tx : transactionHistory) {
                tx.printInfo();
            }
        }
    }
}
