package com.auction.entity;

import java.time.LocalDateTime;

public class Art extends Item {
    private String artistName;

    // Constructor không tham số cho Jackson
    public Art() {
        super();
        this.setType("ART");
    }

    public Art(String id, String name, String description, double startingPrice,
               LocalDateTime startTime, LocalDateTime endTime, String sellerId, String artistName) {
        super(id, name, description, startingPrice, startTime, endTime, sellerId);
        this.artistName = artistName;
        this.setType("ART");
    }

    public Art(String id, String name, String description, double startingPrice,
               LocalDateTime startTime, LocalDateTime endTime, String artistName) {
        this(id, name, description, startingPrice, startTime, endTime, null, artistName);
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    @Override
    public void printInfo() {
        System.out.println("[Art] " + getName() + " by " + artistName + " | Start: $" + getStartingPrice());
    }
}
