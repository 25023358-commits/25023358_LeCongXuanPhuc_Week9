package com.auction.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public abstract class Item extends Entity {

    public enum Status {
        OPEN, RUNNING, FINISHED, PAID, CANCELED
    }

    private String name;
    private String description; // Thêm trường mô tả
    private double startingPrice;
    private double currentHighestBid;
    private String highestBidderId;
    private LocalDateTime startTime; // Thêm thời gian bắt đầu đấu giá
    private LocalDateTime endTime;   // Thêm thời gian kết thúc đấu giá
    private Status status;           // Thêm trạng thái của phiên đấu giá

    private transient List<AuctionObserver> observers = new ArrayList<>();

    // Constructor mới với các trường bổ sung
    public Item(String id, String name, String description, double startingPrice,
                LocalDateTime startTime, LocalDateTime endTime) {
        super(id);
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentHighestBid = startingPrice; // Giá cao nhất ban đầu bằng giá khởi điểm
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = Status.OPEN; // Mặc định khi tạo là OPEN
    }

    // --- GETTER/SETTER ---
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getStartingPrice() { return startingPrice; }
    public double getCurrentHighestBid() { return currentHighestBid; }
    public String getHighestBidderId() { return highestBidderId; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public Status getStatus() { return status; }

    // Setter cho status (cần thiết để AuctionManager thay đổi trạng thái)
    public void setStatus(Status status) {
        this.status = status;
        notifyObservers("Item " + name + " status changed to: " + status);
    }

    // Setter cho highestBidderId (cần thiết khi load từ DB)
    public void setHighestBidderId(String highestBidderId) {
        this.highestBidderId = highestBidderId;
    }

    // Setter cho currentHighestBid (cần thiết khi load từ DB)
    public void setCurrentHighestBid(double currentHighestBid) {
        this.currentHighestBid = currentHighestBid;
    }

    public abstract void printInfo();

    // --- LOGIC OBSERVER ---
    public void addObserver(AuctionObserver observer) {
        if (observers == null) observers = new ArrayList<>();
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    protected void notifyObservers(String message) {
        if (observers != null) {
            for (AuctionObserver obs : observers) {
                obs.update(message);
            }
        }
    }

    public synchronized boolean updateHighestBid(double newBid, String bidderId) {
        // Chỉ cho phép đặt giá nếu phiên đang RUNNING
        if (this.status != Status.RUNNING) {
            notifyObservers("Bid rejected for " + name + ": Auction is not RUNNING.");
            return false;
        }

        if (newBid > currentHighestBid) {
            this.currentHighestBid = newBid;
            this.highestBidderId = bidderId;

            notifyObservers("New highest bid for " + name + ": $" + newBid + " by " + bidderId);
            return true;
        }
        return false;
    }
}
