package com.auction.entity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "role", // Đây là tên trường trong JSON sẽ quyết định tạo lớp con nào
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Bidder.class, name = "BIDDER"),
    @JsonSubTypes.Type(value = Seller.class, name = "SELLER"),
    @JsonSubTypes.Type(value = Admin.class, name = "ADMIN")
})
public abstract class User extends Entity {
    protected String username;
    protected String role;

    // Jackson cần một constructor không tham số để hoạt động
    public User() {
        super(null);
    }

    public User(String id, String username, String role) {
        super(id);
        this.username = username;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getRole() { return role; }

    // Các setter cũng cần thiết cho Jackson
    public void setUsername(String username) {
        this.username = username;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public abstract void displayProfile();
}
