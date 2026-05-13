package com.auction.dao;

import com.auction.util.DBHelper;
import java.sql.*;
import java.util.*;

public class UserDAO {

    // =========================================================
    // Record class — ánh xạ 1-1 với cột trong bảng users
    // Không phải User.java domain class — chỉ chứa dữ liệu thô
    // =========================================================
    public static class UserRecord {
        public String id;
        public String username;
        public String email;
        public String hashedPassword; // không bao giờ trả về plain text
        public String role;
        public double balance;
        public String createdAt;
    }

    // =========================================================
    // CREATE — thêm user mới (dùng khi đăng ký)
    // =========================================================
    public void insert(String id, String username, String email,
            String hashedPassword, String role, double balance)
            throws SQLException {

        String sql = """
                    INSERT INTO users (id, username, email, password, role, balance)
                    VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement stmt = DBHelper.getConnection()
                .prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, username);
            stmt.setString(3, email);
            stmt.setString(4, hashedPassword);
            stmt.setString(5, role);
            stmt.setDouble(6, balance);
            stmt.executeUpdate();
        }
        // try-with-resources tự đóng stmt — không cần stmt.close()
    }

    // =========================================================
    // READ — tìm theo username (dùng khi đăng nhập)
    // Trả về null nếu không tìm thấy
    // =========================================================
    public UserRecord findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (PreparedStatement stmt = DBHelper.getConnection()
                .prepareStatement(sql)) {
            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return mapRow(rs);
            }
        }
        return null;
    }

    // =========================================================
    // READ — tìm theo id
    // =========================================================
    public UserRecord findById(String id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (PreparedStatement stmt = DBHelper.getConnection()
                .prepareStatement(sql)) {
            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return mapRow(rs);
            }
        }
        return null;
    }

    // =========================================================
    // READ — lấy tất cả theo role (BIDDER | SELLER | ADMIN)
    // =========================================================
    public List<UserRecord> findByRole(String role) throws SQLException {
        String sql = "SELECT * FROM users WHERE role = ? ORDER BY created_at DESC";
        List<UserRecord> list = new ArrayList<>();

        try (PreparedStatement stmt = DBHelper.getConnection()
                .prepareStatement(sql)) {
            stmt.setString(1, role);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // =========================================================
    // READ — lấy tất cả người dùng (Admin dùng)
    // =========================================================
    public List<UserRecord> findAll() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        List<UserRecord> list = new ArrayList<>();

        try (PreparedStatement stmt = DBHelper.getConnection().prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    // =========================================================
    // UPDATE — cập nhật balance của Bidder
    // Dùng khi bidder thắng phiên (trừ tiền) hoặc nạp tiền
    // =========================================================
    public void updateBalance(String userId, double newBalance) throws SQLException {
        String sql = "UPDATE users SET balance = ? WHERE id = ?";

        try (PreparedStatement stmt = DBHelper.getConnection()
                .prepareStatement(sql)) {
            stmt.setDouble(1, newBalance);
            stmt.setString(2, userId);
            stmt.executeUpdate();
        }
    }

    // =========================================================
    // DELETE — xóa user (Admin dùng)
    // =========================================================
    public void delete(String userId) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";

        try (PreparedStatement stmt = DBHelper.getConnection()
                .prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.executeUpdate();
        }
    }

    // =========================================================
    // mapRow — tái sử dụng ở mọi method, đổi cột chỉ sửa 1 chỗ
    // =========================================================
    private UserRecord mapRow(ResultSet rs) throws SQLException {
        UserRecord r = new UserRecord();
        r.id = rs.getString("id");
        r.username = rs.getString("username");
        r.email = rs.getString("email");
        r.hashedPassword = rs.getString("password");
        r.role = rs.getString("role");
        r.balance = rs.getDouble("balance");
        r.createdAt = rs.getString("created_at");
        return r;
    }
}
