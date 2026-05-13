package com.auction.service;

import com.auction.dao.ItemDAO;
import com.auction.entity.Item;
import com.auction.entity.Message;
import com.auction.server.AuctionServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionScheduler {
    private final ItemDAO itemDAO = new ItemDAO();
    private final AuctionManager auctionManager;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuctionScheduler(AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::checkAuctions, 0, 5, TimeUnit.SECONDS);
    }

    private void checkAuctions() {
        try {
            List<Item> allItems = itemDAO.findAll();
            LocalDateTime now = LocalDateTime.now();

            for (Item item : allItems) {
                boolean changed = false;

                // 1. Chuyển OPEN -> RUNNING
                if (item.getStatus() == Item.Status.OPEN && now.isAfter(item.getStartTime())) {
                    item.setStatus(Item.Status.RUNNING);
                    changed = true;
                    System.out.println("🚀 Auction STARTED: " + item.getName());
                }

                // 2. Chuyển RUNNING -> FINISHED
                if (item.getStatus() == Item.Status.RUNNING && now.isAfter(item.getEndTime())) {
                    item.setStatus(Item.Status.FINISHED);
                    changed = true;
                    System.out.println("🏁 Auction FINISHED: " + item.getName() + 
                                       " | Winner: " + item.getHighestBidderId());
                }

                if (changed) {
                    // Cập nhật Database
                    itemDAO.save(item);
                    // Cập nhật Memory Manager
                    auctionManager.addItem(item);
                    // Broadcast cho Client
                    AuctionServer.broadcast(new Message("ITEM_STATUS_CHANGED", objectMapper.writeValueAsString(item)));
                }
            }
        } catch (Exception e) {
            System.err.println("Error in AuctionScheduler: " + e.getMessage());
        }
    }

    public void stop() {
        scheduler.shutdown();
    }
}
