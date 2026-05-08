package com.auction.entity;

public interface AuctionObserver {
    void update(String message);
    void onAuctionEnd(String itemId, String winnerId, double finalPrice);
}
