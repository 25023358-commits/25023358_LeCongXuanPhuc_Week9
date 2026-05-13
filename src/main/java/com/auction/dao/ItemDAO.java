package com.auction.dao;

import com.auction.entity.Art;
import com.auction.entity.Electronics;
import com.auction.entity.Item;
import com.auction.util.DBHelper;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ItemDAO {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // =========================================================
    // CREATE / UPDATE — Thêm hoặc cập nhật item
    // =========================================================
    public void save(Item item) throws SQLException {
        // Kiểm tra xem item đã tồn tại chưa
        boolean exists = findById(item.getId()) != null;

        String sql;
        if (exists) {
            // Câu lệnh UPDATE
            sql = """
                UPDATE items SET
                    name = ?, description = ?, starting_price = ?, current_bid = ?,
                    highest_bidder_id = ?, start_time = ?, end_time = ?, status = ?,
                    warranty_months = ?, artist_name = ?
                WHERE id = ?
            """;
        } else {
            // Câu lệnh INSERT
            sql = """
                INSERT INTO items (name, description, starting_price, current_bid,
                                   highest_bidder_id, start_time, end_time, status,
                                   warranty_months, artist_name, id, type, seller_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        }

        try (PreparedStatement stmt = DBHelper.getConnection().prepareStatement(sql)) {
            stmt.setString(1, item.getName());
            stmt.setString(2, item.getDescription());
            stmt.setDouble(3, item.getStartingPrice());
            stmt.setDouble(4, item.getCurrentHighestBid());
            stmt.setString(5, item.getHighestBidderId());
            stmt.setString(6, item.getStartTime().format(formatter));
            stmt.setString(7, item.getEndTime().format(formatter));
            stmt.setString(8, item.getStatus().name());

            if (item instanceof Electronics) {
                stmt.setInt(9, ((Electronics) item).getWarrantyMonths());
                stmt.setNull(10, Types.VARCHAR);
                if (!exists) {
                    stmt.setString(12, "ELECTRONICS");
                }
            } else if (item instanceof Art) {
                stmt.setNull(9, Types.INTEGER);
                stmt.setString(10, ((Art) item).getArtistName());
                if (!exists) {
                    stmt.setString(12, "ART");
                }
            }

            if (exists) {
                stmt.setString(11, item.getId());
            } else {
                stmt.setString(11, item.getId());
                // Giả sử Item có sellerId, cần thêm getter
                // stmt.setString(13, item.getSellerId());
            }

            stmt.executeUpdate();
        }
    }

    // =========================================================
    // READ — tìm theo id
    // =========================================================
    public Item findById(String id) throws SQLException {
        String sql = "SELECT * FROM items WHERE id = ?";

        try (PreparedStatement stmt = DBHelper.getConnection().prepareStatement(sql)) {
            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRowToItem(rs);
            }
        }
        return null;
    }

    // =========================================================
    // READ — lấy tất cả items
    // =========================================================
    public List<Item> findAll() throws SQLException {
        String sql = "SELECT * FROM items ORDER BY created_at DESC";
        List<Item> list = new ArrayList<>();

        try (Statement stmt = DBHelper.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRowToItem(rs));
            }
        }
        return list;
    }
    
    // =========================================================
    // mapRowToItem — chuyển ResultSet → Item object
    // =========================================================
    private Item mapRowToItem(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String name = rs.getString("name");
        String description = rs.getString("description");
        double startingPrice = rs.getDouble("starting_price");
        LocalDateTime startTime = LocalDateTime.parse(rs.getString("start_time"), formatter);
        LocalDateTime endTime = LocalDateTime.parse(rs.getString("end_time"), formatter);
        String type = rs.getString("type");

        Item item;
        if ("ELECTRONICS".equals(type)) {
            int warranty = rs.getInt("warranty_months");
            item = new Electronics(id, name, description, startingPrice, startTime, endTime, warranty);
        } else {
            String artist = rs.getString("artist_name");
            item = new Art(id, name, description, startingPrice, startTime, endTime, artist);
        }

        item.setCurrentHighestBid(rs.getDouble("current_bid"));
        item.setHighestBidderId(rs.getString("highest_bidder_id"));
        item.setStatus(Item.Status.valueOf(rs.getString("status")));

        return item;
    }
    
    // Các hàm update và delete khác có thể giữ nguyên hoặc tích hợp vào hàm save()
}
