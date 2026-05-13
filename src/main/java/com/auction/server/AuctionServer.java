package com.auction.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.auction.service.AuctionManager;
import com.auction.service.AutoBidder;
import com.auction.util.DBHelper;
import com.auction.dao.BidTransactionDAO;
import com.auction.dao.ItemDAO;
import com.auction.dao.UserDAO;
import com.auction.service.AuthService;
import com.auction.entity.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuctionServer {
    private static final int PORT = 12345;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final AuctionManager auctionManager = new AuctionManager();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AuthService authService = new AuthService();
    private final ItemDAO itemDAO = new ItemDAO();
    private final UserDAO userDAO = new UserDAO();
    private final AutoBidder autoBidder;
    private final com.auction.service.AuctionScheduler auctionScheduler;

    public AuctionServer() {
        objectMapper.registerModule(new JavaTimeModule());
        autoBidder = new AutoBidder(auctionManager);
        auctionManager.setAutoBidder(autoBidder);
        auctionScheduler = new com.auction.service.AuctionScheduler(auctionManager);
    }

    public void start() {
        DBHelper.initializeDatabase();
        loadItemsIntoManager();
        auctionScheduler.start();
        System.out.println("Server started on port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                clients.add(handler);
                executor.submit(handler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadItemsIntoManager() {
        try {
            List<Item> items = itemDAO.findAll();
            for (Item item : items) {
                auctionManager.addItem(item);
            }
            System.out.println("Loaded " + items.size() + " items into AuctionManager.");
        } catch (Exception e) {
            System.err.println("Failed to load items: " + e.getMessage());
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendMessage(Message msg) {
            try {
                out.println(objectMapper.writeValueAsString(msg));
            } catch (Exception e) {
                System.err.println("Error sending message to client: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    Message msg = objectMapper.readValue(line, Message.class);
                    handleMessage(msg);
                }
            } catch (Exception e) {
                System.out.println("Client disconnected: " + socket.getRemoteSocketAddress());
            } finally {
                clients.remove(this);
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleMessage(Message msg) throws Exception {
            switch (msg.getType()) {
                case "LOGIN":              handleLogin(msg); break;
                case "REGISTER":           handleRegister(msg); break;
                case "GET_ITEMS":          handleListItems(); break;
                case "CREATE_ITEM":        handleCreateItem(msg); break;
                case "BID":                handleBid(msg); break;
                case "DELETE_ITEM":        handleDeleteItem(msg); break;
                case "REGISTER_AUTO_BID": handleRegisterAutoBid(msg); break;
                case "GET_BID_HISTORY":   handleGetBidHistory(msg); break;
                case "GET_ANALYTICS":     handleGetAnalytics(msg); break;
                case "GET_SELLER_ITEMS":  handleGetSellerItems(msg); break;
                // === Chức năng 5+8: Balance & Profile ===
                case "GET_BALANCE":        handleGetBalance(msg); break;
                case "TOP_UP":             handleTopUp(msg); break;
                case "GET_PROFILE":        handleGetProfile(msg); break;
                // === Chức năng 6: Admin ===
                case "GET_ALL_USERS":      handleGetAllUsers(); break;
                case "ADMIN_DELETE_USER":  handleAdminDeleteUser(msg); break;
                case "ADMIN_DELETE_ITEM":  handleAdminDeleteItem(msg); break;
                // === Chức năng 9: Thanh toán ===
                case "FINALIZE_PAYMENT":   handleFinalizePayment(msg); break;
                case "LOGOUT":             handleLogout(); break;
            }
        }

        private void handleLogin(Message msg) throws IOException {
            try {
                LoginRequest req = objectMapper.readValue(msg.getData(), LoginRequest.class);
                User user = authService.login(req.getUsername(), req.getPassword());
                if (user != null) {
                    if (user instanceof Bidder) {
                        auctionManager.addBidder((Bidder) user);
                    }
                    out.println(objectMapper.writeValueAsString(new Message("LOGIN_SUCCESS", objectMapper.writeValueAsString(user))));
                } else {
                    out.println(objectMapper.writeValueAsString(new Message("LOGIN_FAILED", "Invalid credentials")));
                }
            } catch (Exception e) {
                out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage())));
            }
        }

        private void handleRegister(Message msg) throws IOException {
            try {
                RegisterRequest req = objectMapper.readValue(msg.getData(), RegisterRequest.class);
                boolean success = authService.register(req.getUsername(), req.getEmail(), req.getPassword(), req.getRole());
                if (success) {
                    out.println(objectMapper.writeValueAsString(new Message("REGISTER_SUCCESS", "Account created successfully.")));
                } else {
                    out.println(objectMapper.writeValueAsString(new Message("REGISTER_FAILED", "Username already exists.")));
                }
            } catch (Exception e) {
                out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage())));
            }
        }

        private void handleCreateItem(Message msg) throws IOException {
            try {
                Item newItem = objectMapper.readValue(msg.getData(), Item.class);
                itemDAO.save(newItem);
                auctionManager.addItem(newItem);
                out.println(objectMapper.writeValueAsString(new Message("CREATE_ITEM_SUCCESS", "Item created.")));
                broadcast(new Message("NEW_ITEM_ADDED", objectMapper.writeValueAsString(newItem)));
            } catch (Exception e) {
                out.println(objectMapper.writeValueAsString(new Message("CREATE_ITEM_FAILED", e.getMessage())));
            }
        }

        private void handleListItems() throws IOException {
            try {
                List<Item> items = itemDAO.findAll(); 
                out.println(objectMapper.writeValueAsString(new Message("ITEM_LIST", objectMapper.writeValueAsString(items))));
            } catch (Exception e) {
                out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage())));
            }
        }

        private void handleBid(Message msg) throws IOException {
            try {
                BidRequest req = objectMapper.readValue(msg.getData(), BidRequest.class);
                boolean success = auctionManager.placeBid(req.getItemId(), req.getBidderId(), req.getAmount());
                out.println(objectMapper.writeValueAsString(new Message("BID_RESULT", String.valueOf(success))));
                if (success) {
                    broadcast(new Message("BID_UPDATE", objectMapper.writeValueAsString(req)));
                }
            } catch (Exception e) {
                out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage())));
            }
        }

        private void handleDeleteItem(Message msg) {
            try {
                String itemId = msg.getData();
                itemDAO.delete(itemId);
                broadcast(new Message("ITEM_REMOVED", itemId));
                out.println(objectMapper.writeValueAsString(new Message("DELETE_SUCCESS", itemId)));
            } catch (Exception e) {
                 try { out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage()))); } catch (Exception ignored) {}
            }
        }

        // === Chức năng 3: Auto-bid thực sự ===
        private void handleRegisterAutoBid(Message msg) {
            try {
                com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(msg.getData());
                String bidderId  = node.get("bidderId").asText();
                String itemId    = node.get("itemId").asText();
                double maxBid    = node.get("maxBid").asDouble();
                double increment = node.get("increment").asDouble();

                // Kiểm tra bidder và item tồn tại
                Item item = auctionManager.getItem(itemId);
                if (item == null) {
                    out.println(objectMapper.writeValueAsString(new Message("AUTO_BID_FAILED", "Item not found.")));
                    return;
                }
                // Giả sử chúng ta có thể kiểm tra bidder từ UserDAO nếu không có trong memory
                UserDAO.UserRecord bidderRecord = userDAO.findById(bidderId);
                if (bidderRecord == null) {
                    out.println(objectMapper.writeValueAsString(new Message("AUTO_BID_FAILED", "Bidder not found.")));
                    return;
                }
                
                autoBidder.register(bidderId, itemId, maxBid, increment);
                out.println(objectMapper.writeValueAsString(new Message("AUTO_BID_SUCCESS",
                        "Auto-Bid registered: max=$" + maxBid + ", step=$" + increment)));
            } catch (Exception e) {
                try { out.println(objectMapper.writeValueAsString(new Message("AUTO_BID_FAILED", e.getMessage()))); } catch (Exception ignored) {}
            }
        }

        // === Chức năng 5+8: Lấy số dư ===
        private void handleGetBalance(Message msg) {
            try {
                String userId = msg.getData();
                UserDAO.UserRecord record = userDAO.findById(userId);
                if (record != null) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("balance", record.balance);
                    result.put("username", record.username);
                    out.println(objectMapper.writeValueAsString(new Message("BALANCE_INFO", objectMapper.writeValueAsString(result))));
                } else {
                    out.println(objectMapper.writeValueAsString(new Message("ERROR", "User not found.")));
                }
            } catch (Exception e) {
                try { out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage()))); } catch (Exception ignored) {}
            }
        }

        // === Chức năng 8: Nạp tiền ===
        private void handleTopUp(Message msg) {
            try {
                com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(msg.getData());
                String userId = node.get("userId").asText();
                double amount = node.get("amount").asDouble();
                if (amount <= 0) {
                    out.println(objectMapper.writeValueAsString(new Message("TOP_UP_FAILED", "Amount must be positive.")));
                    return;
                }
                UserDAO.UserRecord record = userDAO.findById(userId);
                if (record == null) {
                    out.println(objectMapper.writeValueAsString(new Message("TOP_UP_FAILED", "User not found.")));
                    return;
                }
                double newBalance = record.balance + amount;
                userDAO.updateBalance(userId, newBalance);
                
                // Cập nhật lại bidder trong AuctionManager (Rất quan trọng cho bidding check)
                Bidder updatedBidder = new Bidder(record.id, record.username, newBalance);
                auctionManager.addBidder(updatedBidder);

                Map<String, Object> result = new HashMap<>();
                result.put("newBalance", newBalance);
                out.println(objectMapper.writeValueAsString(new Message("TOP_UP_SUCCESS", objectMapper.writeValueAsString(result))));
            } catch (Exception e) {
                try { out.println(objectMapper.writeValueAsString(new Message("TOP_UP_FAILED", e.getMessage()))); } catch (Exception ignored) {}
            }
        }

        // === Chức năng 8: Lấy profile ===
        private void handleGetProfile(Message msg) {
            try {
                String userId = msg.getData();
                UserDAO.UserRecord record = userDAO.findById(userId);
                if (record != null) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", record.id);
                    result.put("username", record.username);
                    result.put("email", record.email != null ? record.email : "");
                    result.put("role", record.role);
                    result.put("balance", record.balance);
                    result.put("createdAt", record.createdAt != null ? record.createdAt : "");
                    out.println(objectMapper.writeValueAsString(new Message("PROFILE_INFO", objectMapper.writeValueAsString(result))));
                } else {
                    out.println(objectMapper.writeValueAsString(new Message("ERROR", "User not found.")));
                }
            } catch (Exception e) {
                try { out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage()))); } catch (Exception ignored) {}
            }
        }

        // === Chức năng 6: Admin - lấy tất cả users ===
        private void handleGetAllUsers() {
            try {
                java.util.List<UserDAO.UserRecord> records = userDAO.findByRole("BIDDER");
                records.addAll(userDAO.findByRole("SELLER"));
                java.util.List<Map<String, Object>> users = new java.util.ArrayList<>();
                for (UserDAO.UserRecord r : records) {
                    Map<String, Object> u = new HashMap<>();
                    u.put("id", r.id);
                    u.put("username", r.username);
                    u.put("email", r.email != null ? r.email : "");
                    u.put("role", r.role);
                    u.put("balance", r.balance);
                    u.put("createdAt", r.createdAt != null ? r.createdAt : "");
                    users.add(u);
                }
                out.println(objectMapper.writeValueAsString(new Message("ALL_USERS", objectMapper.writeValueAsString(users))));
            } catch (Exception e) {
                try { out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage()))); } catch (Exception ignored) {}
            }
        }

        // === Chức năng 6: Admin - xóa user ===
        private void handleAdminDeleteUser(Message msg) {
            try {
                String userId = msg.getData();
                userDAO.delete(userId);
                broadcast(new Message("USER_DELETED", userId));
                out.println(objectMapper.writeValueAsString(new Message("DELETE_USER_SUCCESS", userId)));
            } catch (Exception e) {
                try { out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage()))); } catch (Exception ignored) {}
            }
        }

        // === Chức năng 6: Admin - xóa item ===
        private void handleAdminDeleteItem(Message msg) {
            try {
                String itemId = msg.getData();
                itemDAO.delete(itemId);
                broadcast(new Message("ITEM_REMOVED", itemId));
                out.println(objectMapper.writeValueAsString(new Message("DELETE_SUCCESS", itemId)));
            } catch (Exception e) {
                try { out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage()))); } catch (Exception ignored) {}
            }
        }

        // === Chức năng 9: Thanh toán khi đấu giá kết thúc ===
        private void handleFinalizePayment(Message msg) {
            try {
                com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(msg.getData());
                String itemId   = node.get("itemId").asText();
                String winnerId = node.get("winnerId").asText();
                double amount   = node.get("amount").asDouble();

                UserDAO.UserRecord winner = userDAO.findById(winnerId);
                if (winner == null) {
                    out.println(objectMapper.writeValueAsString(new Message("PAYMENT_FAILED", "Winner not found.")));
                    return;
                }
                if (winner.balance < amount) {
                    out.println(objectMapper.writeValueAsString(new Message("PAYMENT_FAILED",
                            "Insufficient balance. Need $" + amount + ", have $" + winner.balance)));
                    return;
                }
                // Trừ tiền người thắng trong DB
                double newBalance = winner.balance - amount;
                userDAO.updateBalance(winnerId, newBalance);
                
                // Cập nhật trong AuctionManager memory
                Bidder updatedWinner = new Bidder(winner.id, winner.username, newBalance);
                auctionManager.addBidder(updatedWinner);

                // Cập nhật status item -> PAID
                Item item = auctionManager.getItem(itemId);
                if (item != null) {
                    item.setStatus(Item.Status.PAID);
                    itemDAO.save(item);
                }
                Map<String, Object> result = new HashMap<>();
                result.put("itemId", itemId);
                result.put("winnerId", winnerId);
                result.put("amount", amount);
                result.put("newBalance", newBalance);
                String resultJson = objectMapper.writeValueAsString(result);
                out.println(objectMapper.writeValueAsString(new Message("PAYMENT_SUCCESS", resultJson)));
                broadcast(new Message("ITEM_STATUS_CHANGED", objectMapper.writeValueAsString(item)));
                System.out.println("💰 Payment completed: " + winnerId + " paid $" + amount + " for " + itemId);
            } catch (Exception e) {
                try { out.println(objectMapper.writeValueAsString(new Message("PAYMENT_FAILED", e.getMessage()))); } catch (Exception ignored) {}
            }
        }

        private void handleLogout() {
            clients.remove(this);
            System.out.println("Client logged out.");
        }

        private void handleGetBidHistory(Message msg) {
            try {
                String itemId = msg.getData();
                List<BidTransaction> bids = new BidTransactionDAO().getBidsByItem(itemId);
                out.println(objectMapper.writeValueAsString(new Message("BID_HISTORY", objectMapper.writeValueAsString(bids))));
            } catch (Exception e) {
                try { out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage()))); } catch (Exception ignored) {}
            }
        }

        private void handleGetAnalytics(Message msg) {
            try {
                String itemId = msg.getData();
                BidTransactionDAO dao = new BidTransactionDAO();
                int count = dao.countBids(itemId);
                BidTransaction highest = dao.getHighestBid(itemId);
                Map<String, Object> stats = new HashMap<>();
                stats.put("totalBids", count);
                stats.put("highestBid", highest != null ? highest.getBidAmount() : 0.0);
                out.println(objectMapper.writeValueAsString(new Message("ANALYTICS", objectMapper.writeValueAsString(stats))));
            } catch (Exception e) {
                try { out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage()))); } catch (Exception ignored) {}
            }
        }

        private void handleGetSellerItems(Message msg) {
            try {
                String sellerId = msg.getData();
                List<Item> items = new ItemDAO().findBySeller(sellerId);
                out.println(objectMapper.writeValueAsString(new Message("SELLER_ITEMS", objectMapper.writeValueAsString(items))));
            } catch (Exception e) {
                try { out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage()))); } catch (Exception ignored) {}
            }
        }
    }

    public static void broadcast(Message msg) {
        for (ClientHandler client : clients) {
            client.sendMessage(msg);
        }
    }

    public static void main(String[] args) {
        new AuctionServer().start();
    }
}