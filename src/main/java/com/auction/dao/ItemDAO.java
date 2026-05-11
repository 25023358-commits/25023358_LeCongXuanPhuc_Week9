package com.auction.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.auction.entity.*;
import com.auction.util.DBHelper;

public class ItemDAO {
    public void saveItem(Item item, String sellerId) {
        String sql = "INSERT OR REPLACE INTO items (id, name, starting_price, current_highest_bid, highest_bidder_id, type, seller_id, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBHelper.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, item.getId());
            pstmt.setString(2, item.getName());
            pstmt.setDouble(3, item.getStartingPrice());
            pstmt.setDouble(4, item.getCurrentHighestBid());
            pstmt.setString(5, item.getHighestBidderId());
            if (item instanceof Electronics) {
                pstmt.setString(6, "ELECTRONICS");
            } else if (item instanceof Art) {
                pstmt.setString(6, "ART");
            } else {
                pstmt.setString(6, "ITEM");
            }
            pstmt.setString(7, sellerId);
            pstmt.setString(8, "OPEN"); // Default status
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Item getItemById(String id) {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (Connection conn = DBHelper.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String name = rs.getString("name");
                double startingPrice = rs.getDouble("starting_price");
                double currentBid = rs.getDouble("current_highest_bid");
                String highestBidder = rs.getString("highest_bidder_id");
                String type = rs.getString("type");
                Item item;
                if ("ELECTRONICS".equals(type)) {
                    item = new Electronics(id, name, startingPrice, 12); // Placeholder warranty
                } else if ("ART".equals(type)) {
                    item = new Art(id, name, startingPrice, "Unknown"); // Placeholder artist
                } else {
                    item = new Electronics(id, name, startingPrice, 0); // Fallback
                }
                // Update current bid
                if (currentBid > startingPrice) {
                    item.updateHighestBid(currentBid, highestBidder);
                }
                return item;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Item> getAllItems() {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items";
        try (Connection conn = DBHelper.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String id = rs.getString("id");
                items.add(getItemById(id));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }
}