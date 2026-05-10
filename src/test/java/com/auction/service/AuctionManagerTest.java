package com.auction.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.auction.service.AuctionManager;
import com.auction.entity.Bidder;
import com.auction.entity.Electronics;
import com.auction.entity.Item;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionManagerTest {
    private AuctionManager manager;

    @BeforeEach
    void setUp() {
        manager = new AuctionManager();
    }

    @Test
    void testAddItem() {
        Item item = new Electronics("I1", "Laptop", 1000.0, 12);
        manager.addItem(item);
        assertEquals(item, manager.getItem("I1"));
    }

    @Test
    void testPlaceBid() {
        Item item = new Electronics("I1", "Laptop", 1000.0, 12);
        manager.addItem(item);
        Bidder bidder = new Bidder("B1", "bidder", 2000.0);
        manager.addBidder(bidder);

        manager.startCountdown("I1", 60); // Start auction

        boolean success = manager.placeBid("I1", "B1", 1100.0);
        assertTrue(success);
        assertEquals(1100.0, item.getCurrentHighestBid());
    }
}
