package com.auction.entity.observer;

public interface BidObserver {
    void update(String itemId, double newBid, String bidderId);
}
