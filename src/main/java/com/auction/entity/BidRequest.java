package src.main.java.com.auction.entity;

public class BidRequest {
    private String itemId;
    private String bidderId;
    private double amount;

    public BidRequest() {}

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getBidderId() { return bidderId; }
    public void setBidderId(String bidderId) { this.bidderId = bidderId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}
