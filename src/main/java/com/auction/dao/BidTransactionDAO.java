package src.main.java.com.auction.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import src.main.java.com.auction.entity.*;
import src.main.java.com.auction.util.DBHelper;

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
}
