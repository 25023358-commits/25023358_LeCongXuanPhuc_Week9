package com.auction.dao;

import com.auction.entity.bid.BidTransaction;
import com.auction.util.DBHelper;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BidTransactionDAO {

    public void saveBid(BidTransaction bid) {
        String sql = "INSERT INTO bids (id, item_id, bidder_id, bid_amount, timestamp, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBHelper.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, bid.getTransactionId());
            pstmt.setString(2, bid.getItemId());
            pstmt.setString(3, bid.getBidderId());
            pstmt.setDouble(4, bid.getBidAmount());
            pstmt.setLong(5, bid.getTimestamp().toEpochSecond(java.time.ZoneOffset.UTC));
            pstmt.setString(6, bid.getStatus());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<BidTransaction> getBidsByItem(String itemId) {
        List<BidTransaction> bids = new ArrayList<>();
        String sql = "SELECT * FROM bids WHERE item_id = ? ORDER BY timestamp DESC";
        try (Connection conn = DBHelper.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String id = rs.getString("id");
                String bidderId = rs.getString("bidder_id");
                double amount = rs.getDouble("bid_amount");
                long timestamp = rs.getLong("timestamp");
                String status = rs.getString("status");
                BidTransaction bid = new BidTransaction(itemId, bidderId, amount);
                bid.setStatus(status); // Assume we add setter
                bids.add(bid);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bids;
    }
    public void markAllLost(String itemId) {
        String sql = "UPDATE bids SET status = 'LOST' WHERE item_id = ? AND status = 'WINNING'";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // Lấy bid cao nhất — xác định winner khi phiên kết thúc
    public BidTransaction getHighestBid(String itemId) {
        String sql = "SELECT * FROM bids WHERE item_id = ? ORDER BY bid_amount DESC LIMIT 1";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                BidTransaction bid = new BidTransaction(
                        itemId,
                        rs.getString("bidder_id"),
                        rs.getDouble("bid_amount")
                );
                bid.setStatus(rs.getString("status"));
                return bid;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Đếm số bid — dùng cho BidAnalytics
    public int countBids(String itemId) {
        String sql = "SELECT COUNT(*) FROM bids WHERE item_id = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}