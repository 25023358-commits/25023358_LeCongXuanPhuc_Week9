package src.main.java.com.auction.entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BidTransaction {
    private String transactionId;
    private String itemId;
    private String bidderId;
    private double bidAmount;
    private LocalDateTime timestamp;
    private String status; // "WINNING", "LOST", "PENDING"

    private static int counter = 0;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public BidTransaction(String itemId, String bidderId, double bidAmount) {
        this.transactionId = generateId();
        this.itemId = itemId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.timestamp = LocalDateTime.now();
        this.status = "PENDING";
    }

    private synchronized String generateId() {
        return "TXN_" + System.currentTimeMillis() + "_" + (++counter);
    }

    public void markAsWinning() {
        this.status = "WINNING";
    }

    public void markAsLost() {
        this.status = "LOST";
    }

    public boolean isWinning() {
        return "WINNING".equals(status);
    }

    public void printInfo() {
        String statusIcon;
        switch (status) {
            case "WINNING": statusIcon = "🏆 WINNING"; break;
            case "LOST": statusIcon = "❌ LOST"; break;
            default: statusIcon = "⏳ PENDING";
        }
        System.out.printf("   [%s] %s | %s bid $%.2f at %s%n",
                transactionId, statusIcon, bidderId, bidAmount,
                timestamp.format(TIME_FORMATTER));
    }

    // Getters
    public String getTransactionId() { return transactionId; }
    public String getItemId() { return itemId; }
    public String getBidderId() { return bidderId; }
    public double getBidAmount() { return bidAmount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getStatus() { return status; }

    // Setter
    public void setStatus(String status) {
        this.status = status;
    }
}
