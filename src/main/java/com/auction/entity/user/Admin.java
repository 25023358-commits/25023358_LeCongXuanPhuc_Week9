package com.auction.entity.user;

public class Admin extends User {

    // Constructor không tham số cho Jackson
    public Admin() {
        super();
    }

    public Admin(String id, String username) {
        super(id, username, "ADMIN");
    }

    @Override
    public void displayProfile() {
        System.out.println("--- Admin Dashboard ---");
        System.out.println("Username: " + username);
        System.out.println("Quyền hạn: Toàn quyền hệ thống (CRUD User & Item).");
    }

    public void banUser(String userId) {
        System.out.println("Đã khóa người dùng: " + userId);
    }
}
