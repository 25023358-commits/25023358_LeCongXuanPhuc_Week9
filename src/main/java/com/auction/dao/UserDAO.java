package com.auction.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.auction.entity.*;
import com.auction.util.DBHelper;
import com.auction.entity.User;

public class UserDAO {
    public void saveUser(User user) {
        String sql = "INSERT OR REPLACE INTO users (id, username, password, role, balance) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBHelper.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, "password"); // Placeholder, in real app hash password
            pstmt.setString(4, user.getRole());
            if (user instanceof Bidder) {
                pstmt.setDouble(5, ((Bidder) user).getBalance());
            } else {
                pstmt.setDouble(5, 0.0);
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DBHelper.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String id = rs.getString("id");
                String role = rs.getString("role");
                double balance = rs.getDouble("balance");
                switch (role) {
                    case "BIDDER": return new Bidder(id, username, balance);
                    case "SELLER": return new Seller(id, username);
                    case "ADMIN": return new Admin(id, username);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = DBHelper.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String id = rs.getString("id");
                String username = rs.getString("username");
                String role = rs.getString("role");
                double balance = rs.getDouble("balance");
                switch (role) {
                    case "BIDDER": users.add(new Bidder(id, username, balance)); break;
                    case "SELLER": users.add(new Seller(id, username)); break;
                    case "ADMIN": users.add(new Admin(id, username)); break;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
}