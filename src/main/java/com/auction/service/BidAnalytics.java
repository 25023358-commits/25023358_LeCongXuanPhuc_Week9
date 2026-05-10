package src.main.java.com.auction.service;

import java.util.*;
import java.util.concurrent.*;

// Advanced: Phân tích đấu giá
public class BidAnalytics {
    private static BidAnalytics instance;
    private Map<String, List<Double>> bidHistory = new ConcurrentHashMap<>();
    private Map<String, Integer> bidCount = new ConcurrentHashMap<>();

    private BidAnalytics() {}

    public static BidAnalytics getInstance() {
        if (instance == null) {
            synchronized (BidAnalytics.class) {
                if (instance == null) instance = new BidAnalytics();
            }
        }
        return instance;
    }

    public void recordBid(String itemId, double amount) {
        bidHistory.computeIfAbsent(itemId, k -> new CopyOnWriteArrayList<>()).add(amount);
        bidCount.merge(itemId, 1, Integer::sum);
    }

    public void printStats(String itemId) {
        List<Double> history = bidHistory.get(itemId);
        if (history == null || history.isEmpty()) {
            System.out.println("📊 No bids yet for " + itemId);
            return;
        }

        double avg = history.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double max = history.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double min = history.stream().mapToDouble(Double::doubleValue).min().orElse(0);

        System.out.println("📊 Analytics for " + itemId + ":");
        System.out.println("   - Total bids: " + history.size());
        System.out.println("   - Avg bid: $" + String.format("%.2f", avg));
        System.out.println("   - Highest: $" + String.format("%.2f", max));
        System.out.println("   - Lowest: $" + String.format("%.2f", min));
        System.out.println("   - Price increase: +$" + String.format("%.2f", max - min));
    }
}
