package src.main.java.com.auction.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import src.main.java.com.auction.entity.*;

public class NotificationService {
    private List<BidObserver> observers;
    private RealtimeNotifier realtime;

    public NotificationService() {
        observers = new CopyOnWriteArrayList<>();
        realtime = RealtimeNotifier.getInstance(); // Keep singleton for now, or refactor later
    }

    public void addObserver(BidObserver observer) {
        observers.add(observer);
    }

    public void notifyObservers(String itemId, double price, String bidderId) {
        for (BidObserver obs : observers) {
            obs.update(itemId, price, bidderId);
        }
    }

    public void watchItem(String itemId, Bidder bidder) {
        realtime.watchItem(itemId, bidder);
    }

    public void startCountdown(String itemId, int seconds, Runnable onEnd) {
        realtime.startCountdown(itemId, seconds, onEnd);
    }

    public void notifyRealtime(String itemId, double bidAmount, String bidderId) {
        realtime.notifyRealtime(itemId, bidAmount, bidderId);
    }
}
