package com.auction.entity;

public interface BidObserver {
    void update(String itemId, double newBid, String bidderId);
}
