package com.auction.entity;

import java.time.LocalDateTime;

public class Art extends Item {
    private String artistName;

    public Art(String id, String name, String description, double startingPrice,
               LocalDateTime startTime, LocalDateTime endTime, String artistName) {
        super(id, name, description, startingPrice, startTime, endTime);
        this.artistName = artistName;
    }

    public String getArtistName() {
        return artistName;
    }

    @Override
    public void printInfo() {
        System.out.println("[Art] " + getName() + " by " + artistName + " | Start: $" + getStartingPrice());
    }
}
