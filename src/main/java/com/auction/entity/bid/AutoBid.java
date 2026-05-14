package com.auction.entity.bid;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AutoBid {
    private String id;
    private String itemId;
    private String userId;
    private BigDecimal maxAmount;
    private boolean isActive;
    private LocalDateTime createdAt;

    // Constructors
    public AutoBid() {}

    public AutoBid(String id, String itemId, String userId, BigDecimal maxAmount) {
        this.id = id;
        this.itemId = itemId;
        this.userId = userId;
        this.maxAmount = maxAmount;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
