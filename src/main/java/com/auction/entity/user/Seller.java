package com.auction.entity.user;

public class Seller extends User {

    // Constructor không tham số cho Jackson
    public Seller() {
        super();
    }

    public Seller(String id, String username) {
        super(id, username, "SELLER");
    }

    @Override
    public void displayProfile() {
        System.out.println("--- Seller Profile ---");
        System.out.println("Username: " + username);
        System.out.println("Role: " + role);
        System.out.println("Chức năng: Đăng sản phẩm, Quản lý kho hàng.");
    }

    public void createItem() {
        System.out.println("Đang thực hiện CRUD: Thêm sản phẩm mới...");
    }
}
