package com.auction.entity;

public class Message {
    private String type; // e.g., "LOGIN", "BID", "LIST_ITEMS"
    private String data; // JSON string

    public Message() {}

    public Message(String type, String data) {
        this.type = type;
        this.data = data;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
}
