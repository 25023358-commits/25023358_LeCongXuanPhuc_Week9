package src.main.java.com.auction.service;

public class DefaultBiddingStrategy implements BiddingStrategy {
    @Override
    public boolean isValidBid(double currentHighest, double newBid) {
        return newBid > currentHighest;
    }
}
