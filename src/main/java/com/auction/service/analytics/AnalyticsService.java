package com.auction.service.analytics;

import com.auction.service.bidding.BidAnalytics;

public class AnalyticsService {
    private BidAnalytics analytics;

    public AnalyticsService() {
        analytics = BidAnalytics.getInstance(); // Keep singleton for now
    }

    public void recordBid(String itemId, double bidAmount) {
        analytics.recordBid(itemId, bidAmount);
    }

    public void printStats(String itemId) {
        analytics.printStats(itemId);
    }
}
