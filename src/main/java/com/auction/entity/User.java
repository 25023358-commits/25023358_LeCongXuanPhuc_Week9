package com.auction.entity;
public abstract class User extends Entity {
    protected String username;
    protected String role;

    public User(String id, String username, String role) {
        super(id);
        this.username = username;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getRole() { return role; }

    public abstract void displayProfile();
}
