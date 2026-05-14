package com.auction.dao;

import com.auction.entity.bid.AutoBid;
import com.auction.util.DBHelper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AutoBidDAO {

    /**
     * Saves or updates an auto-bid setting.
     * If an entry for the same user and item exists, it updates it. Otherwise, it inserts a new one.
     */
    public void save(AutoBid autoBid) throws SQLException {
        Optional<AutoBid> existing = findByUserAndItem(autoBid.getUserId(), autoBid.getItemId());

        String sql;
        if (existing.isPresent()) {
            sql = "UPDATE autobids SET max_amount = ?, is_active = ? WHERE id = ?";
        } else {
            sql = "INSERT INTO autobids (id, item_id, user_id, max_amount, is_active) VALUES (?, ?, ?, ?, ?)";
        }

        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (existing.isPresent()) {
                pstmt.setBigDecimal(1, autoBid.getMaxAmount());
                pstmt.setBoolean(2, autoBid.isActive());
                pstmt.setString(3, existing.get().getId());
            } else {
                pstmt.setString(1, autoBid.getId());
                pstmt.setString(2, autoBid.getItemId());
                pstmt.setString(3, autoBid.getUserId());
                pstmt.setBigDecimal(4, autoBid.getMaxAmount());
                pstmt.setBoolean(5, autoBid.isActive());
            }
            pstmt.executeUpdate();
        }
    }

    /**
     * Finds an active auto-bid setting for a specific user and item.
     */
    public Optional<AutoBid> findByUserAndItem(String userId, String itemId) throws SQLException {
        String sql = "SELECT * FROM autobids WHERE user_id = ? AND item_id = ? AND is_active = TRUE";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToAutoBid(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Finds all active auto-bid settings for a given item, excluding a specific user (usually the current bidder).
     * Ordered by max_amount descending, so the highest bidder is first.
     */
    public List<AutoBid> findActiveAutoBidsForItem(String itemId, String excludeUserId) throws SQLException {
        List<AutoBid> autoBids = new ArrayList<>();
        String sql = "SELECT * FROM autobids WHERE item_id = ? AND is_active = TRUE AND user_id != ? ORDER BY max_amount DESC";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemId);
            pstmt.setString(2, excludeUserId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    autoBids.add(mapRowToAutoBid(rs));
                }
            }
        }
        return autoBids;
    }

    /**
     * Deactivates an auto-bid setting for a user and item.
     */
    public void deactivate(String userId, String itemId) throws SQLException {
        String sql = "UPDATE autobids SET is_active = FALSE WHERE user_id = ? AND item_id = ?";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, itemId);
            pstmt.executeUpdate();
        }
    }

    private AutoBid mapRowToAutoBid(ResultSet rs) throws SQLException {
        AutoBid autoBid = new AutoBid();
        autoBid.setId(rs.getString("id"));
        autoBid.setItemId(rs.getString("item_id"));
        autoBid.setUserId(rs.getString("user_id"));
        autoBid.setMaxAmount(rs.getBigDecimal("max_amount"));
        autoBid.setActive(rs.getBoolean("is_active"));
        autoBid.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return autoBid;
    }
}
