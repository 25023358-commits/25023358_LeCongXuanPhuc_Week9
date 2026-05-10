package src.main.java.com.auction.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBHelper {
    private static final String DB_URL = "jdbc:sqlite:auction.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Create tables
            String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                    "id TEXT PRIMARY KEY," +
                    "username TEXT UNIQUE," +
                    "password TEXT," +
                    "role TEXT," +
                    "balance REAL DEFAULT 0.0" +
                    ")";
            stmt.execute(createUsersTable);

            String createItemsTable = "CREATE TABLE IF NOT EXISTS items (" +
                    "id TEXT PRIMARY KEY," +
                    "name TEXT," +
                    "starting_price REAL," +
                    "current_highest_bid REAL," +
                    "highest_bidder_id TEXT," +
                    "type TEXT," +
                    "seller_id TEXT," +
                    "status TEXT DEFAULT 'OPEN'," +
                    "start_time INTEGER," +
                    "end_time INTEGER," +
                    "FOREIGN KEY (seller_id) REFERENCES users(id)" +
                    ")";
            stmt.execute(createItemsTable);

            String createBidsTable = "CREATE TABLE IF NOT EXISTS bids (" +
                    "id TEXT PRIMARY KEY," +
                    "item_id TEXT," +
                    "bidder_id TEXT," +
                    "bid_amount REAL," +
                    "timestamp INTEGER," +
                    "status TEXT," +
                    "FOREIGN KEY (item_id) REFERENCES items(id)," +
                    "FOREIGN KEY (bidder_id) REFERENCES users(id)" +
                    ")";
            stmt.execute(createBidsTable);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
