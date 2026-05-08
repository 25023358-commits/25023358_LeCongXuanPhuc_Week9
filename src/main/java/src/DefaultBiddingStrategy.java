package com.auction;

public class DefaultBiddingStrategy implements BiddingStrategy {
    @Override
    public boolean isValidBid(double newBid, double currentBid) {
        return newBid > currentBid;
    }
}
