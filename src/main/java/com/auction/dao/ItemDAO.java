package com.auction.dao;

import com.auction.util.DBHelper;
import java.sql.*;
import java.util.*;

public class ItemDAO {

    // =========================================================
    // Record class — ánh xạ bảng items
    // warranty_months và artist_name có thể null tùy type
    // =========================================================
    public static class ItemRecord {
        public String id;
        public String name;
        public String type;           // "ELECTRONICS" | "ART"
        public double startingPrice;
        public double currentBid;
        public String sellerId;
        public String status;         // OPEN|RUNNING|FINISHED|PAID|CANCELED
        public Integer warrantyMonths;// null nếu là Art
        public String artistName;     // null nếu là Electronics
        public String createdAt;
    }

    // =========================================================
    // CREATE — thêm Electronics
    // =========================================================
    public void insertElectronics(String id, String name, double startingPrice,
                                  String sellerId, int warrantyMonths)
            throws SQLException {

        String sql = """
            INSERT INTO items (id, name, type, starting_price, current_bid,
                               seller_id, warranty_months)
            VALUES (?, ?, 'ELECTRONICS', ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = DBHelper.getConnection()
                .prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, name);
            stmt.setDouble(3, startingPrice);
            stmt.setDouble(4, startingPrice); // current_bid = starting_price ban đầu
            stmt.setString(5, sellerId);
            stmt.setInt(6, warrantyMonths);
            stmt.executeUpdate();
        }
    }

    // =========================================================
    // CREATE — thêm Art
    // =========================================================
    public void insertArt(String id, String name, double startingPrice,
                          String sellerId, String artistName)
            throws SQLException {

        String sql = """
            INSERT INTO items (id, name, type, starting_price, current_bid,
                               seller_id, artist_name)
            VALUES (?, ?, 'ART', ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = DBHelper.getConnection()
                .prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, name);
            stmt.setDouble(3, startingPrice);
            stmt.setDouble(4, startingPrice);
            stmt.setString(5, sellerId);
            stmt.setString(6, artistName);
            stmt.executeUpdate();
        }
    }

    // =========================================================
    // READ — tìm theo id
    // =========================================================
    public ItemRecord findById(String id) throws SQLException {
        String sql = "SELECT * FROM items WHERE id = ?";

        try (PreparedStatement stmt = DBHelper.getConnection()
                .prepareStatement(sql)) {
            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    // =========================================================
    // READ — lấy tất cả items (hiển thị danh sách cho bidder)
    // =========================================================
    public List<ItemRecord> findAll() throws SQLException {
        String sql = "SELECT * FROM items ORDER BY created_at DESC";
        List<ItemRecord> list = new ArrayList<>();

        try (PreparedStatement stmt = DBHelper.getConnection()
                .prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // =========================================================
    // READ — items của một seller cụ thể
    // =========================================================
    public List<ItemRecord> findBySeller(String sellerId) throws SQLException {
        String sql = "SELECT * FROM items WHERE seller_id = ? ORDER BY created_at DESC";
        List<ItemRecord> list = new ArrayList<>();

        try (PreparedStatement stmt = DBHelper.getConnection()
                .prepareStatement(sql)) {
            stmt.setString(1, sellerId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // =========================================================
    // UPDATE — cập nhật giá bid cao nhất
    // Gọi sau mỗi lần bid thành công trong AuctionManager
    // =========================================================
    public void updateCurrentBid(String itemId, double newBid) throws SQLException {
        String sql = "UPDATE items SET current_bid = ? WHERE id = ?";

        try (PreparedStatement stmt = DBHelper.getConnection()
                .prepareStatement(sql)) {
            stmt.setDouble(1, newBid);
            stmt.setString(2, itemId);
            stmt.executeUpdate();
        }
    }

    // =========================================================
    // UPDATE — đổi trạng thái item
    // OPEN → RUNNING → FINISHED → PAID | CANCELED
    // =========================================================
    public void updateStatus(String itemId, String status) throws SQLException {
        String sql = "UPDATE items SET status = ? WHERE id = ?";

        try (PreparedStatement stmt = DBHelper.getConnection()
                .prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, itemId);
            stmt.executeUpdate();
        }
    }

    // =========================================================
    // DELETE — seller xóa item của mình
    // =========================================================
    public void delete(String itemId) throws SQLException {
        String sql = "DELETE FROM items WHERE id = ?";

        try (PreparedStatement stmt = DBHelper.getConnection()
                .prepareStatement(sql)) {
            stmt.setString(1, itemId);
            stmt.executeUpdate();
        }
    }

    // =========================================================
    // mapRow — chuyển ResultSet → ItemRecord
    // =========================================================
    private ItemRecord mapRow(ResultSet rs) throws SQLException {
        ItemRecord r    = new ItemRecord();
        r.id            = rs.getString("id");
        r.name          = rs.getString("name");
        r.type          = rs.getString("type");
        r.startingPrice = rs.getDouble("starting_price");
        r.currentBid    = rs.getDouble("current_bid");
        r.sellerId      = rs.getString("seller_id");
        r.status        = rs.getString("status");
        r.createdAt     = rs.getString("created_at");

        // warrantyMonths: 0 trong SQLite nếu null — cần check bằng wasNull()
        int w = rs.getInt("warranty_months");
        r.warrantyMonths = rs.wasNull() ? null : w;

        r.artistName    = rs.getString("artist_name"); // null nếu không có
        return r;
    }
}
