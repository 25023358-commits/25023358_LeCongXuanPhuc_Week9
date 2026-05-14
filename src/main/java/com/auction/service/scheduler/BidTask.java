package com.auction.service.scheduler;

import com.auction.service.auction.AuctionManager;

public class BidTask implements Runnable {
    private String itemId;
    private String user;
    private double amount;
    private AuctionManager auctionManager;

    public BidTask(String itemId, String user, double amount, AuctionManager auctionManager) {
        this.itemId = itemId;
        this.user = user;
        this.amount = amount;
        this.auctionManager = auctionManager;
    }

    @Override
    public void run() {
        auctionManager.placeBid(itemId, user, amount);
    }
}
