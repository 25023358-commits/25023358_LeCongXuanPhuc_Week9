package com.auction.entity;

public class Admin extends User {

    public Admin(String id, String username) {
        super(id, username, "ADMIN");
    }

    @Override
    public void displayProfile() {
        System.out.println("--- Admin Dashboard ---");
        System.out.println("Username: " + username);
        System.out.println("Quyền hạn: Toàn quyền hệ thống (CRUD User & Item).");
    }

    // Chức năng riêng của Admin
    public void banUser(String userId) {
        System.out.println("Đã khóa người dùng: " + userId);
    }
}
