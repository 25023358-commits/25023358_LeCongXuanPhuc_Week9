package com.auction.dao;

import com.auction.entity.Art;
import com.auction.entity.Electronics;
import com.auction.entity.Vehicle;
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
                    warranty_months = ?, artist_name = ?, engine_cc = ?
                WHERE id = ?
            """;
        } else {
            // Câu lệnh INSERT
            sql = """
                INSERT INTO items (name, description, starting_price, current_bid,
                                   highest_bidder_id, start_time, end_time, status,
                                   warranty_months, artist_name, engine_cc, id, type, seller_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                stmt.setNull(11, Types.INTEGER);
                if (!exists) {
                    stmt.setString(13, "ELECTRONICS");
                }
            } else if (item instanceof Art) {
                stmt.setNull(9, Types.INTEGER);
                stmt.setString(10, ((Art) item).getArtistName());
                stmt.setNull(11, Types.INTEGER);
                if (!exists) {
                    stmt.setString(13, "ART");
                }
            } else if (item instanceof Vehicle) {
                stmt.setNull(9, Types.INTEGER);
                stmt.setNull(10, Types.VARCHAR);
                stmt.setInt(11, ((Vehicle) item).getEngineCC());
                if (!exists) {
                    stmt.setString(13, "VEHICLE");
                }
            }

            if (exists) {
                stmt.setString(12, item.getId());
            } else {
                stmt.setString(12, item.getId());
                stmt.setString(14, item.getSellerId());
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

    public List<Item> findBySeller(String sellerId) throws SQLException {
        String sql = "SELECT * FROM items WHERE seller_id = ? ORDER BY created_at DESC";
        List<Item> list = new ArrayList<>();

        try (PreparedStatement stmt = DBHelper.getConnection().prepareStatement(sql)) {
            stmt.setString(1, sellerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToItem(rs));
                }
            }
        }
        return list;
    }
    // =========================================================
    // UPDATE — Cập nhật giá cao nhất và người đặt giá
    // =========================================================
    public void updateCurrentBid(String itemId, double bidAmount) throws SQLException {
        String sql = "UPDATE items SET current_bid = ? WHERE id = ?";

        try (PreparedStatement stmt = DBHelper.getConnection().prepareStatement(sql)) {
            stmt.setDouble(1, bidAmount);
            stmt.setString(2, itemId);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Updating item failed, no rows affected.");
            }
        }
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
        String sellerId = rs.getString("seller_id");
        if ("ELECTRONICS".equals(type)) {
            int warranty = rs.getInt("warranty_months");
            item = new Electronics(id, name, description, startingPrice, startTime, endTime, sellerId, warranty);
        } else if ("ART".equals(type)) {
            String artist = rs.getString("artist_name");
            item = new Art(id, name, description, startingPrice, startTime, endTime, sellerId, artist);
        } else {
            int engineCC = rs.getInt("engine_cc");
            item = new Vehicle(id, name, description, startingPrice, startTime, endTime, sellerId, engineCC);
        }

        item.setCurrentHighestBid(rs.getDouble("current_bid"));
        item.setHighestBidderId(rs.getString("highest_bidder_id"));
        item.setStatus(Item.Status.valueOf(rs.getString("status")));

        return item;
    }
    
    public void delete(String id) throws SQLException {
        String sql = "DELETE FROM items WHERE id = ?";
        try (PreparedStatement stmt = DBHelper.getConnection().prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.executeUpdate();
        }
    }
    
    // Các hàm update khác có thể tích hợp vào hàm save()
}
