package com.auction.service.strategy;

public interface BiddingStrategy {
    boolean isValidBid(double currentHighest, double newBid);
}
