package com.auction.entity.observer;

public interface AuctionObserver {
    void update(String message);
    void onAuctionEnd(String itemId, String winnerId, double finalPrice);
}
