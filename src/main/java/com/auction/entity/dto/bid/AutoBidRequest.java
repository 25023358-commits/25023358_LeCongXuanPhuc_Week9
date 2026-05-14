package com.auction.entity.dto.bid;

/**
 * AutoBidRequest — Gửi từ Client lên Server để đăng ký Auto Bidding.
 */
public class AutoBidRequest {
    private String itemId;
    private String bidderId;
    private double maxBid;
    private double increment;

    public AutoBidRequest() {}

    public AutoBidRequest(String itemId, String bidderId, double maxBid, double increment) {
        this.itemId = itemId;
        this.bidderId = bidderId;
        this.maxBid = maxBid;
        this.increment = increment;
    }

    public String getItemId()      { return itemId; }
    public String getBidderId()    { return bidderId; }
    public double getMaxBid()      { return maxBid; }
    public double getIncrement()   { return increment; }

    public void setItemId(String itemId)         { this.itemId = itemId; }
    public void setBidderId(String bidderId)     { this.bidderId = bidderId; }
    public void setMaxBid(double maxBid)         { this.maxBid = maxBid; }
    public void setIncrement(double increment)   { this.increment = increment; }
}
