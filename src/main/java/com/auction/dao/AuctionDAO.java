package com.auction.dao;

import com.auction.util.DBHelper;
import java.sql.*;
import java.util.*;

public class AuctionDAO {

    // =========================================================
    // Record class — ánh xạ bảng auctions
    // =========================================================
    public static class AuctionRecord {
        public String id;
        public String itemId;
        public String status;      // OPEN|RUNNING|FINISHED|PAID|CANCELED
        public String startTime;
        public String endTime;     // thay đổi khi AntiSniping gia hạn
        public String winnerId;    // null cho đến khi kết thúc
        public Double finalPrice;  // null cho đến khi kết thúc
    }

    // =========================================================
    // CREATE — tạo phiên đấu giá mới
    // Gọi trong AuctionManager.startCountdown()
    // =========================================================
    public void insert(String auctionId, String itemId, int durationSeconds)
            throws SQLException {

        String sql = """
            INSERT INTO auctions (id, item_id, status, start_time, end_time)
            VALUES (?, ?, 'RUNNING',
                    datetime('now'),
                    datetime('now', '+' || ? || ' seconds'))
        """;

        try (PreparedStatement stmt = DBHelper.getConnection()
                .prepareStatement(sql)) {
            stmt.setString(1, auctionId);
            stmt.setString(2, itemId);
            stmt.setInt(3, durationSeconds);
            stmt.executeUpdate();
        }
    }

    // =========================================================
    // READ — tìm phiên theo id
    // =========================================================
    public AuctionRecord findById(String auctionId) throws SQLException {
        String sql = "SELECT * FROM auctions WHERE id = ?";

        try (PreparedStatement stmt = DBHelper.getConnection()
                .prepareStatement(sql)) {
            stmt.setString(1, auctionId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    // =========================================================
    // READ — tìm phiên đang active của một item
    // =========================================================
    public AuctionRecord findActiveByItem(String itemId) throws SQLException {
        String sql = """
            SELECT * FROM auctions
            WHERE item_id = ? AND status = 'RUNNING'
            LIMIT 1
        """;

        try (PreparedStatement stmt = DBHelper.getConnection()
                .prepareStatement(sql)) {
            stmt.setString(1, itemId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    // =========================================================
    // READ — tất cả phiên đang RUNNING (load lại khi khởi động app)
    // =========================================================
    public List<AuctionRecord> findAllActive() throws SQLException {
        String sql = "SELECT * FROM auctions WHERE status = 'RUNNING'";
        List<AuctionRecord> list = new ArrayList<>();

        try (PreparedStatement stmt = DBHelper.getConnection()
                .prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // =========================================================
    // UPDATE — gia hạn thời gian kết thúc (AntiSniping gọi)
    // =========================================================
    public void updateEndTime(String auctionId, int extraSeconds)
            throws SQLException {

        String sql = """
            UPDATE auctions
            SET end_time = datetime('now', '+' || ? || ' seconds')
            WHERE id = ?
        """;

        try (PreparedStatement stmt = DBHelper.getConnection()
                .prepareStatement(sql)) {
            stmt.setInt(1, extraSeconds);
            stmt.setString(2, auctionId);
            stmt.executeUpdate();
        }
    }

    // =========================================================
    // UPDATE — đổi status phiên
    // RUNNING → FINISHED khi hết giờ
    // =========================================================
    public void updateStatus(String auctionId, String status) throws SQLException {
        String sql = "UPDATE auctions SET status = ? WHERE id = ?";

        try (PreparedStatement stmt = DBHelper.getConnection()
                .prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, auctionId);
            stmt.executeUpdate();
        }
    }

    // =========================================================
    // UPDATE — ghi nhận người thắng khi phiên kết thúc
    // =========================================================
    public void setWinner(String auctionId, String winnerId, double finalPrice)
            throws SQLException {

        String sql = """
            UPDATE auctions
            SET status = 'FINISHED', winner_id = ?, final_price = ?
            WHERE id = ?
        """;

        try (PreparedStatement stmt = DBHelper.getConnection()
                .prepareStatement(sql)) {
            stmt.setString(1, winnerId);
            stmt.setDouble(2, finalPrice);
            stmt.setString(3, auctionId);
            stmt.executeUpdate();
        }
    }

    // =========================================================
    // mapRow
    // =========================================================
    private AuctionRecord mapRow(ResultSet rs) throws SQLException {
        AuctionRecord r = new AuctionRecord();
        r.id         = rs.getString("id");
        r.itemId     = rs.getString("item_id");
        r.status     = rs.getString("status");
        r.startTime  = rs.getString("start_time");
        r.endTime    = rs.getString("end_time");
        r.winnerId   = rs.getString("winner_id");

        double fp = rs.getDouble("final_price");
        r.finalPrice = rs.wasNull() ? null : fp;
        return r;
    }
}
