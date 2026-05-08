package src;

import java.util.concurrent.*;

public class BidTask implements Runnable {
    private String itemId;
    private String user;
    private double amount;

    public BidTask(String itemId, String user, double amount) {
        this.itemId = itemId;
        this.user = user;
        this.amount = amount;
    }

    @Override
    public void run() {
        AuctionManager.getInstance().placeBid(itemId, user, amount);
    }
}



