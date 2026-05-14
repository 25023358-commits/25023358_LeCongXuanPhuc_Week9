package com.auction.service.strategy;

public class DefaultBiddingStrategy implements BiddingStrategy {
    @Override
    public boolean isValidBid(double currentHighest, double newBid) {
        return newBid > currentHighest;
    }
}
