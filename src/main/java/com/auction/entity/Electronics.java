package com.auction.entity;

import java.time.LocalDateTime;

public class Electronics extends Item {
    private int warrantyMonths;

    public Electronics(String id, String name, String description, double startingPrice,
                       LocalDateTime startTime, LocalDateTime endTime, int warrantyMonths) {
        super(id, name, description, startingPrice, startTime, endTime);
        this.warrantyMonths = warrantyMonths;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    @Override
    public void printInfo() {
        System.out.println("[Electronics] " + getName() + " | Start: " + getStartingPrice() + " | Warranty: " + warrantyMonths + " months");
    }
}
