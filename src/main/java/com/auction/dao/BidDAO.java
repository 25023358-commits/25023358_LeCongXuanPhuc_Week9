package com.auction.dao;

import com.auction.util.DBHelper;
import java.sql.*;
import java.util.*;

public class BidDAO {

    // =========================================================
    // Record class — ánh xạ bảng bids
    // Tương đương BidTransaction.java nhưng từ DB
    // =========================================================
    public static class BidRecord {
        public int    id;           // AUTOINCREMENT
        public String auctionId;
        public String bidderId;
        public double amount;
        public String status;       // WINNING | LOST | PENDING
        public String bidTime;
    }

    // =========================================================
    // CREATE — ghi một bid mới xuống DB
    // Gọi trong AuctionManager.placeBid() sau khi bid thành công
    // =========================================================
    public void insert(String auctionId, String bidderId, double amount)
            throws SQLException {

        String sql = """
            INSERT INTO bids (auction_id, bidder_id, amount, status)
            VALUES (?, ?, ?, 'WINNING')
        """;
        // Mỗi bid mới vào là WINNING — sẽ mark LOST ngay sau nếu có bid cao hơn

        try (PreparedStatement stmt = DBHelper.getConnection()
                .prepareStatement(sql)) {
            stmt.setString(1, auctionId);
            stmt.setString(2, bidderId);
            stmt.setDouble(3, amount);
            stmt.executeUpdate();
        }
    }

    // =========================================================
    // UPDATE — mark tất cả bid cũ của phiên này là LOST
    // Gọi TRƯỚC insert() — giống fix bug markPreviousTransactionsAsLost
    // =========================================================
    public void markAllLost(String auctionId) throws SQLException {
        String sql = """
            UPDATE bids SET status = 'LOST'
            WHERE auction_id = ? AND status = 'WINNING'
        """;

        try (PreparedStatement stmt = DBHelper.getConnection()
                .prepareStatement(sql)) {
            stmt.setString(1, auctionId);
            stmt.executeUpdate();
        }
    }

    // =========================================================
    // READ — lấy bid cao nhất của một phiên (xác định winner)
    // =========================================================
    public BidRecord findHighest(String auctionId) throws SQLException {
        String sql = """
            SELECT * FROM bids
            WHERE auction_id = ?
            ORDER BY amount DESC
            LIMIT 1
        """;

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
    // READ — toàn bộ lịch sử bid của một phiên (cho price chart)
    // Sắp theo thời gian tăng dần để vẽ đường chart
    // =========================================================
    public List<BidRecord> findByAuction(String auctionId) throws SQLException {
        String sql = """
            SELECT * FROM bids
            WHERE auction_id = ?
            ORDER BY bid_time ASC
        """;
        List<BidRecord> list = new ArrayList<>();

        try (PreparedStatement stmt = DBHelper.getConnection()
                .prepareStatement(sql)) {
            stmt.setString(1, auctionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // =========================================================
    // READ — tất cả bid của một bidder (lịch sử cá nhân)
    // =========================================================
    public List<BidRecord> findByBidder(String bidderId) throws SQLException {
        String sql = """
            SELECT * FROM bids
            WHERE bidder_id = ?
            ORDER BY bid_time DESC
        """;
        List<BidRecord> list = new ArrayList<>();

        try (PreparedStatement stmt = DBHelper.getConnection()
                .prepareStatement(sql)) {
            stmt.setString(1, bidderId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // =========================================================
    // READ — đếm số lần bid của một phiên (cho BidAnalytics)
    // =========================================================
    public int countByAuction(String auctionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM bids WHERE auction_id = ?";

        try (PreparedStatement stmt = DBHelper.getConnection()
                .prepareStatement(sql)) {
            stmt.setString(1, auctionId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    // =========================================================
    // mapRow
    // =========================================================
    private BidRecord mapRow(ResultSet rs) throws SQLException {
        BidRecord r  = new BidRecord();
        r.id         = rs.getInt("id");
        r.auctionId  = rs.getString("auction_id");
        r.bidderId   = rs.getString("bidder_id");
        r.amount     = rs.getDouble("amount");
        r.status     = rs.getString("status");
        r.bidTime    = rs.getString("bid_time");
        return r;
    }
}
