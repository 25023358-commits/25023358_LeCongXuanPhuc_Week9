package com.auction.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBHelper {
    private static final String DB_URL = "jdbc:sqlite:auction.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initializeDatabase() {
        System.out.println("Database path: " + new java.io.File("auction.db").getAbsolutePath());
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Create tables
            String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                    "id TEXT PRIMARY KEY," +
                    "username TEXT UNIQUE NOT NULL," +
                    "email TEXT UNIQUE," +
                    "password TEXT NOT NULL," +
                    "role TEXT NOT NULL," +
                    "balance REAL DEFAULT 0.0," +
                    "created_at TEXT DEFAULT (datetime('now'))" +
                    ")";
            stmt.execute(createUsersTable);
            
            // Schema đã ổn định - KHÔNG drop table để giữ dữ liệu

            String createItemsTable = "CREATE TABLE IF NOT EXISTS items (" +
                    "id TEXT PRIMARY KEY," +
                    "name TEXT," +
                    "description TEXT," +                    // ← THÊM
                    "starting_price REAL," +
                    "current_bid REAL," +                    // ← ĐỔI TÊN
                    "highest_bidder_id TEXT," +
                    "type TEXT," +
                    "seller_id TEXT," +
                    "status TEXT DEFAULT 'OPEN'," +
                    "start_time TEXT," +                     // ← ĐỔI KIỂU THÀNH TEXT
                    "end_time TEXT," +                       // ← ĐỔI KIỂU THÀNH TEXT
                    "warranty_months INTEGER," +             // ← THÊM
                    "artist_name TEXT," +                    // ← THÊM
                    "created_at INTEGER DEFAULT (strftime('%s','now'))," +  // ← THÊM
                    "FOREIGN KEY (seller_id) REFERENCES users(id)" +
                    ")";
            stmt.execute(createItemsTable);

            String createBidsTable = "CREATE TABLE IF NOT EXISTS bids (" +
                    "id TEXT PRIMARY KEY," +
                    "item_id TEXT NOT NULL," +
                    "bidder_id TEXT NOT NULL," +
                    "bid_amount REAL NOT NULL," +
                    "timestamp INTEGER NOT NULL," +
                    "status TEXT DEFAULT 'WINNING'," +
                    "FOREIGN KEY (item_id) REFERENCES items(id)," +
                    "FOREIGN KEY (bidder_id) REFERENCES users(id)" +
                    ")";
            stmt.execute(createBidsTable);

            String createAuctionsTable = "CREATE TABLE IF NOT EXISTS auctions (" +
                    "id TEXT PRIMARY KEY," +
                    "item_id TEXT UNIQUE," +
                    "status TEXT DEFAULT 'OPEN'," +
                    "start_time INTEGER," +
                    "end_time INTEGER," +
                    "winner_id TEXT," +
                    "final_price REAL," +
                    "FOREIGN KEY (item_id) REFERENCES items(id)," +
                    "FOREIGN KEY (winner_id) REFERENCES users(id)" +
                    ")";
            stmt.execute(createAuctionsTable);
            
            // ĐẢM BẢO TÀI KHOẢN ADMIN LUÔN SẴN SÀNG (Mật khẩu: admin123)
            String checkAdmin = "SELECT id FROM users WHERE username = 'admin'";
            try (ResultSet rs = stmt.executeQuery(checkAdmin)) {
                String hashedPw = "$2a$10$8.UnVuG9HHgffUDAlk8qfOuYJd9nJui7H.vY8uD1tP2M7F8f69K3O"; // admin123
                if (rs.next()) {
                    // Nếu đã có user 'admin', ép lại password và role để đảm bảo đăng nhập được
                    String updateAdmin = "UPDATE users SET password = '" + hashedPw + "', role = 'ADMIN' WHERE username = 'admin'";
                    stmt.execute(updateAdmin);
                } else {
                    // Nếu chưa có, tạo mới
                    String insertAdmin = "INSERT INTO users (id, username, email, password, role, balance) " +
                                       "VALUES ('USR_ADMIN01', 'admin', 'admin@auex.com', '" + hashedPw + "', 'ADMIN', 0.0)";
                    stmt.execute(insertAdmin);
                    System.out.println("✅ Default Admin account initialized (admin / admin123)");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}