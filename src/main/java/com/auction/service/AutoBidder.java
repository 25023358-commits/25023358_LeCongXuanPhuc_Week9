package src.main.java.com.auction.service;

import java.util.*;
import java.util.concurrent.*;

public class AutoBidder {

    private static class AutoConfig {
        String bidderId;
        String itemId;
        double maxBid;
        double increment;
        long   registeredAt;

        AutoConfig(String bidderId, String itemId,
                   double maxBid, double increment) {
            this.bidderId     = bidderId;
            this.itemId       = itemId;
            this.maxBid       = maxBid;
            this.increment    = increment;
            this.registeredAt = System.currentTimeMillis();
        }
    }

    private final Map<String, List<AutoConfig>> configs
            = new ConcurrentHashMap<>();

    private final AuctionManager manager;

    public AutoBidder(AuctionManager manager) {
        this.manager = manager;
    }

    public synchronized void register(
            String bidderId, String itemId,
            double maxBid, double increment) {

        configs.computeIfAbsent(itemId, k -> new CopyOnWriteArrayList<>())
                .add(new AutoConfig(bidderId, itemId, maxBid, increment));

        System.out.println("🤖 AutoBid registered: " + bidderId
                + " | max=$" + maxBid + " | step=$" + increment);
    }

    public void onNewBid(String itemId, String winnerBidderId, double currentPrice) {
        List<AutoConfig> list = configs.get(itemId);
        if (list == null || list.isEmpty()) return;

        list.sort(Comparator.comparingLong(c -> c.registeredAt));

        for (AutoConfig cfg : list) {
            if (cfg.bidderId.equals(winnerBidderId)) continue;

            double nextBid = currentPrice + cfg.increment;

            if (nextBid <= cfg.maxBid) {
                System.out.println("🤖 AutoBid firing: " + cfg.bidderId
                        + " → $" + nextBid);
                boolean ok = manager.placeBid(itemId, cfg.bidderId, nextBid);
                if (ok) break;
            } else {
                System.out.println("🤖 AutoBid STOPPED for " + cfg.bidderId
                        + ": maxBid $" + cfg.maxBid + " reached");
            }
        }
    }

    public void unregister(String itemId) {
        configs.remove(itemId);
    }
}
