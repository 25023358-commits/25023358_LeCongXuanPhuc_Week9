package com.auction.entity;

import java.time.LocalDateTime;

public class Electronics extends Item {
    private int warrantyMonths;

    // Constructor không tham số cho Jackson
    public Electronics() {
        super();
        this.setType("ELECTRONICS");
    }

    public Electronics(String id, String name, String description, double startingPrice,
                       LocalDateTime startTime, LocalDateTime endTime, String sellerId, int warrantyMonths) {
        super(id, name, description, startingPrice, startTime, endTime, sellerId);
        this.warrantyMonths = warrantyMonths;
        this.setType("ELECTRONICS");
    }

    public Electronics(String id, String name, String description, double startingPrice,
                       LocalDateTime startTime, LocalDateTime endTime, int warrantyMonths) {
        this(id, name, description, startingPrice, startTime, endTime, null, warrantyMonths);
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public void printInfo() {
        System.out.println("[Electronics] " + getName() + " | Start: " + getStartingPrice() + " | Warranty: " + warrantyMonths + " months");
    }
}
