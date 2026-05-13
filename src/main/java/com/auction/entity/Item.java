package com.auction.entity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Electronics.class, name = "ELECTRONICS"),
    @JsonSubTypes.Type(value = Art.class, name = "ART")
})
public abstract class Item extends Entity {

    public enum Status {
        OPEN, RUNNING, FINISHED, PAID, CANCELED
    }

    private String name;
    private String description;
    private double startingPrice;
    private double currentHighestBid;
    private String highestBidderId;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime startTime;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime endTime;
    
    private Status status;
    private String type; 
    private String sellerId; 

    private transient List<AuctionObserver> observers = new ArrayList<>();

    public Item() {
        super(null);
    }

    public Item(String id, String name, String description, double startingPrice,
                LocalDateTime startTime, LocalDateTime endTime, String sellerId) {
        super(id);
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = Status.OPEN;
        this.currentHighestBid = startingPrice;
        this.sellerId = sellerId;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public double getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(double currentHighestBid) { this.currentHighestBid = currentHighestBid; }

    public String getHighestBidderId() { return highestBidderId; }
    public void setHighestBidderId(String highestBidderId) { this.highestBidderId = highestBidderId; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public void addObserver(AuctionObserver observer) {
        if (observers == null) observers = new ArrayList<>();
        observers.add(observer);
    }

    public boolean updateHighestBid(double amount, String bidderId) {
        if (amount > currentHighestBid) {
            this.currentHighestBid = amount;
            this.highestBidderId = bidderId;
            notifyObservers("New highest bid for " + name + ": $" + amount + " by " + bidderId);
            return true;
        }
        return false;
    }

    public abstract void printInfo();

    protected void notifyObservers(String message) {
        if (observers != null) {
            for (AuctionObserver obs : observers) {
                obs.update(message);
            }
        }
    }

    @Override
    public String toString() {
        return name + " (Current Bid: $" + currentHighestBid + ")";
    }
}
