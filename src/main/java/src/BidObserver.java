package com.auction;

public interface BidObserver {
    void update(String itemId, double newBid);
}
