package src.main.java.com.auction.service;

public interface BiddingStrategy {
    boolean isValidBid(double currentHighest, double newBid);
}
