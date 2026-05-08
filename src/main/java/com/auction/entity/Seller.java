package com.auction.entity;

public class Seller extends User {

    public Seller(String id, String username) {
        // Gán cứng role là "Seller" khi khởi tạo
        super(id, username, "Seller");
    }

    // Thực thi phương thức trừu tượng từ lớp User
    @Override
    public void displayProfile() {
        System.out.println("--- Seller Profile ---");
        System.out.println("ID: " + getUsername()); // Giả định Entity có getId()
        System.out.println("Username: " + username);
        System.out.println("Role: " + role);
        System.out.println("Chức năng: Đăng sản phẩm, Quản lý kho hàng.");
    }

    // Chức năng riêng của Seller
    public void createItem() {
        System.out.println("Đang thực hiện CRUD: Thêm sản phẩm mới...");
    }
}
