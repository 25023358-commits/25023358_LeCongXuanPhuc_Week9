package com.auction;

public interface BiddingStrategy {
    boolean isValidBid(double newBid, double currentBid);
}
