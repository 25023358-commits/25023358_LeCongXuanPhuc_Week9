package com.auction.server;

import com.auction.entity.bid.BidTransaction;
import com.auction.entity.dto.auth.LoginRequest;
import com.auction.entity.dto.auth.RegisterRequest;
import com.auction.entity.dto.bid.AutoBidRequest;
import com.auction.entity.dto.bid.BidRequest;
import com.auction.entity.items.Item;
import com.auction.entity.message.Message;
import com.auction.entity.user.Bidder;
import com.auction.entity.user.User;
import com.auction.service.scheduler.AuctionScheduler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.auction.service.auction.AuctionManager;
import com.auction.service.bidding.AutoBidder;
import com.auction.util.DBHelper;
import com.auction.dao.BidTransactionDAO;
import com.auction.dao.ItemDAO;
import com.auction.dao.UserDAO;
import com.auction.service.auth.AuthService;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
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
    private final AuctionScheduler auctionScheduler;

    public AuctionServer() {
        objectMapper.registerModule(new JavaTimeModule());
        // Khởi tạo AutoBidder và gán vào AuctionManager
        autoBidder = new AutoBidder(auctionManager);
        auctionManager.setAutoBidder(autoBidder);
        auctionScheduler = new AuctionScheduler(auctionManager);
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
        private String currentUserId; // track user for cleanup

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
                try { socket.close(); } catch (IOException e) { e.printStackTrace(); }
            }
        }

        private void handleMessage(Message msg) throws Exception {
            switch (msg.getType()) {
                case "LOGIN":              handleLogin(msg); break;
                case "REGISTER":           handleRegister(msg); break;
                case "GET_ITEMS":          handleListItems(); break;
                case "CREATE_ITEM":        handleCreateItem(msg); break;
                case "UPDATE_ITEM":        handleUpdateItem(msg); break;
                case "BID":                handleBid(msg); break;
                case "DELETE_ITEM":        handleDeleteItem(msg); break;
                case "REGISTER_AUTO_BID":  handleRegisterAutoBid(msg); break;
                case "GET_BID_HISTORY":    handleGetBidHistory(msg); break;
                case "GET_ANALYTICS":      handleGetAnalytics(msg); break;
                case "GET_SELLER_ITEMS":   handleGetSellerItems(msg); break;
                case "GET_ITEM_DETAILS":   handleGetItemDetails(msg); break;
                case "TOP_UP":             handleTopUp(msg); break;
                case "PAY_ITEM":           handlePayItem(msg); break;
                case "GET_ALL_USERS":      handleGetAllUsers(); break;
                case "DELETE_USER":        handleDeleteUser(msg); break;
                case "LOGOUT":             handleLogout(); break;
            }
        }

        private void handleLogin(Message msg) throws IOException {
            try {
                LoginRequest req = objectMapper.readValue(msg.getData(), LoginRequest.class);
                User user = authService.login(req.getUsername(), req.getPassword());
                if (user != null) {
                    currentUserId = user.getId();
                    if (user instanceof Bidder) {
                        auctionManager.addBidder((Bidder) user);
                    }
                    out.println(objectMapper.writeValueAsString(
                            new Message("LOGIN_SUCCESS", objectMapper.writeValueAsString(user))));
                } else {
                    out.println(objectMapper.writeValueAsString(
                            new Message("LOGIN_FAILED", "Invalid credentials")));
                }
            } catch (Exception e) {
                out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage())));
            }
        }

        private void handleRegister(Message msg) throws IOException {
            try {
                RegisterRequest req = objectMapper.readValue(msg.getData(), RegisterRequest.class);
                boolean success = authService.register(req.getUsername(), req.getEmail(),
                        req.getPassword(), req.getRole());
                if (success) {
                    out.println(objectMapper.writeValueAsString(
                            new Message("REGISTER_SUCCESS", "Account created successfully.")));
                } else {
                    out.println(objectMapper.writeValueAsString(
                            new Message("REGISTER_FAILED", "Username already exists.")));
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
                out.println(objectMapper.writeValueAsString(
                        new Message("CREATE_ITEM_SUCCESS", "Item created.")));
                broadcast(new Message("NEW_ITEM_ADDED", objectMapper.writeValueAsString(newItem)));
            } catch (Exception e) {
                out.println(objectMapper.writeValueAsString(
                        new Message("CREATE_ITEM_FAILED", e.getMessage())));
            }
        }

        private void handleUpdateItem(Message msg) throws IOException {
            try {
                Item updatedItem = objectMapper.readValue(msg.getData(), Item.class);
                itemDAO.save(updatedItem); // save handles update if ID exists
                auctionManager.addItem(updatedItem); // Overwrite in activeAuctions
                out.println(objectMapper.writeValueAsString(
                        new Message("UPDATE_ITEM_SUCCESS", "Item updated.")));
                broadcast(new Message("ITEM_STATUS_CHANGED", objectMapper.writeValueAsString(updatedItem)));
            } catch (Exception e) {
                out.println(objectMapper.writeValueAsString(
                        new Message("UPDATE_ITEM_FAILED", e.getMessage())));
            }
        }

        private void handleListItems() throws IOException {
            try {
                List<Item> items = itemDAO.findAll();
                out.println(objectMapper.writeValueAsString(
                        new Message("ITEM_LIST", objectMapper.writeValueAsString(items))));
            } catch (Exception e) {
                out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage())));
            }
        }

        private void handleBid(Message msg) throws IOException {
            try {
                BidRequest req = objectMapper.readValue(msg.getData(), BidRequest.class);
                boolean success = auctionManager.placeBid(req.getItemId(), req.getBidderId(), req.getAmount());
                out.println(objectMapper.writeValueAsString(
                        new Message("BID_RESULT", String.valueOf(success))));
                if (success) {
                    // Lấy tên item để hiển thị trong notification
                    Item item = auctionManager.getItem(req.getItemId());
                    String itemName = (item != null) ? item.getName() : req.getItemId();
                    // Lấy username của bidder để hiển thị
                    String bidderName = req.getBidderId();
                    try {
                        UserDAO.UserRecord rec = userDAO.findById(req.getBidderId());
                        if (rec != null) bidderName = rec.username;
                    } catch (Exception ignored) {}

                    // Tạo notification JSON chi tiết hơn
                    Map<String, Object> bidUpdate = new HashMap<>();
                    bidUpdate.put("itemId", req.getItemId());
                    bidUpdate.put("itemName", itemName);
                    bidUpdate.put("amount", req.getAmount());
                    bidUpdate.put("bidderId", req.getBidderId());
                    bidUpdate.put("bidderName", bidderName);
                    broadcast(new Message("BID_UPDATE", objectMapper.writeValueAsString(bidUpdate)));
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
                out.println(objectMapper.writeValueAsString(
                        new Message("DELETE_SUCCESS", itemId)));
            } catch (Exception e) {
                try { out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage()))); }
                catch (Exception ignored) {}
            }
        }

        private void handleGetAllUsers() {
            try {
                List<UserDAO.UserRecord> users = userDAO.findAll();
                out.println(objectMapper.writeValueAsString(
                        new Message("ALL_USERS", objectMapper.writeValueAsString(users))));
            } catch (Exception e) {
                try { out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage()))); }
                catch (Exception ignored) {}
            }
        }

        private void handleDeleteUser(Message msg) {
            try {
                String userId = msg.getData();
                userDAO.delete(userId);
                // Broadcast if necessary, or just respond
                out.println(objectMapper.writeValueAsString(
                        new Message("DELETE_USER_SUCCESS", userId)));
            } catch (Exception e) {
                try { out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage()))); }
                catch (Exception ignored) {}
            }
        }

        // ===== AUTO BIDDING — tích hợp đầy đủ =====
        private void handleRegisterAutoBid(Message msg) {
            try {
                AutoBidRequest req = objectMapper.readValue(msg.getData(), AutoBidRequest.class);
                autoBidder.register(req.getBidderId(), req.getItemId(), req.getMaxBid(), req.getIncrement());

                Map<String, Object> resp = new HashMap<>();
                resp.put("itemId", req.getItemId());
                resp.put("maxBid", req.getMaxBid());
                resp.put("increment", req.getIncrement());
                out.println(objectMapper.writeValueAsString(
                        new Message("AUTO_BID_REGISTERED", objectMapper.writeValueAsString(resp))));
                System.out.println("AutoBid registered: bidder=" + req.getBidderId()
                        + " item=" + req.getItemId()
                        + " max=$" + req.getMaxBid() + " step=$" + req.getIncrement());
            } catch (Exception e) {
                try { out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage()))); }
                catch (Exception ignored) {}
            }
        }

        private void handleLogout() {
            clients.remove(this);
            System.out.println("Client logged out: " + currentUserId);
        }

        // ===== BID HISTORY — kèm username =====
        private void handleGetBidHistory(Message msg) {
            try {
                String itemId = msg.getData();
                List<BidTransaction> bids = new BidTransactionDAO().getBidsByItem(itemId);

                // Tạo list có kèm username
                List<Map<String, Object>> richBids = new java.util.ArrayList<>();
                for (BidTransaction bid : bids) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("bidderId", bid.getBidderId());
                    entry.put("amount", bid.getBidAmount());
                    entry.put("status", bid.getStatus());
                    entry.put("timestamp", bid.getTimestamp() != null
                            ? bid.getTimestamp().toString() : "");
                    // Resolve username
                    String username = bid.getBidderId();
                    try {
                        UserDAO.UserRecord rec = userDAO.findById(bid.getBidderId());
                        if (rec != null) username = rec.username;
                    } catch (Exception ignored) {}
                    entry.put("bidderName", username);
                    richBids.add(entry);
                }
                out.println(objectMapper.writeValueAsString(
                        new Message("BID_HISTORY", objectMapper.writeValueAsString(richBids))));
            } catch (Exception e) {
                try { out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage()))); }
                catch (Exception ignored) {}
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
                out.println(objectMapper.writeValueAsString(
                        new Message("ANALYTICS", objectMapper.writeValueAsString(stats))));
            } catch (Exception e) {
                try { out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage()))); }
                catch (Exception ignored) {}
            }
        }

        private void handleGetSellerItems(Message msg) {
            try {
                String sellerId = msg.getData();
                List<Item> items = new ItemDAO().findBySeller(sellerId);
                out.println(objectMapper.writeValueAsString(
                        new Message("SELLER_ITEMS", objectMapper.writeValueAsString(items))));
            } catch (Exception e) {
                try { out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage()))); }
                catch (Exception ignored) {}
            }
        }

        // ===== ITEM DETAILS — trả về chi tiết item + seller name + bid count =====
        private void handleGetItemDetails(Message msg) {
            try {
                String itemId = msg.getData();
                Item item = itemDAO.findById(itemId);
                if (item == null) {
                    out.println(objectMapper.writeValueAsString(
                            new Message("ERROR", "Item not found: " + itemId)));
                    return;
                }
                BidTransactionDAO bidDAO = new BidTransactionDAO();
                int bidCount = bidDAO.countBids(itemId);
                BidTransaction highestBid = bidDAO.getHighestBid(itemId);

                // Lấy tên seller
                String sellerName = item.getSellerId();
                try {
                    UserDAO.UserRecord sellerRec = userDAO.findById(item.getSellerId());
                    if (sellerRec != null) sellerName = sellerRec.username;
                } catch (Exception ignored) {}

                // Lấy tên winner (nếu có)
                String winnerName = null;
                if (item.getHighestBidderId() != null) {
                    try {
                        UserDAO.UserRecord winnerRec = userDAO.findById(item.getHighestBidderId());
                        if (winnerRec != null) winnerName = winnerRec.username;
                    } catch (Exception ignored) {}
                }

                Map<String, Object> details = new HashMap<>();
                details.put("item", item);
                details.put("sellerName", sellerName);
                details.put("bidCount", bidCount);
                details.put("highestBidAmount", highestBid != null ? highestBid.getBidAmount() : 0.0);
                details.put("winnerName", winnerName != null ? winnerName : "N/A");

                out.println(objectMapper.writeValueAsString(
                        new Message("ITEM_DETAILS", objectMapper.writeValueAsString(details))));
            } catch (Exception e) {
                try { out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage()))); }
                catch (Exception ignored) {}
            }
        }

        // ===== NẠP TIỀN (TOP UP) =====
        private void handleTopUp(Message msg) {
            try {
                Map<String, Object> req = objectMapper.readValue(msg.getData(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                String userId = (String) req.get("userId");
                double amount = ((Number) req.get("amount")).doubleValue();

                UserDAO.UserRecord rec = userDAO.findById(userId);
                if (rec != null) {
                    double newBalance = rec.balance + amount;
                    userDAO.updateBalance(userId, newBalance);
                    
                    // Cập nhật Cache trên Server để Bid không bị lỗi Insufficient Balance
                    Bidder cachedBidder = auctionManager.getBidder(userId);
                    if (cachedBidder != null) {
                        cachedBidder.setBalance(newBalance);
                    }
                    
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("newBalance", newBalance);
                    out.println(objectMapper.writeValueAsString(new Message("TOP_UP_SUCCESS", objectMapper.writeValueAsString(resp))));
                } else {
                    out.println(objectMapper.writeValueAsString(new Message("ERROR", "User not found for top-up")));
                }
            } catch (Exception e) {
                try { out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage()))); }
                catch (Exception ignored) {}
            }
        }
        
        // ===== THANH TOÁN (PAY_ITEM) =====
        private void handlePayItem(Message msg) {
            try {
                String itemId = msg.getData();
                Item item = itemDAO.findById(itemId);
                
                if (item == null || item.getStatus() != Item.Status.FINISHED) {
                    out.println(objectMapper.writeValueAsString(new Message("ERROR", "Invalid item or not finished yet.")));
                    return;
                }
                
                String winnerId = item.getHighestBidderId();
                if (winnerId == null || !winnerId.equals(currentUserId)) {
                    out.println(objectMapper.writeValueAsString(new Message("ERROR", "You are not the winner.")));
                    return;
                }

                UserDAO.UserRecord winner = userDAO.findById(winnerId);
                double amountToPay = item.getCurrentHighestBid();
                
                if (winner.balance >= amountToPay) {
                    // Trừ tiền
                    double newBalance = winner.balance - amountToPay;
                    userDAO.updateBalance(winnerId, newBalance);
                    
                    // Cập nhật RAM
                    Bidder cachedBidder = auctionManager.getBidder(winnerId);
                    if (cachedBidder != null) {
                        cachedBidder.setBalance(newBalance);
                    }
                    
                    // Chuyển trạng thái Item thành PAID
                    item.setStatus(Item.Status.PAID);
                    itemDAO.save(item);
                    auctionManager.addItem(item);
                    
                    // Báo cho user số dư mới thông qua TOP_UP_SUCCESS (tái sử dụng UI cập nhật)
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("newBalance", newBalance);
                    out.println(objectMapper.writeValueAsString(new Message("TOP_UP_SUCCESS", objectMapper.writeValueAsString(resp))));
                    
                    // Broadcast cập nhật Item
                    broadcast(new Message("ITEM_STATUS_CHANGED", objectMapper.writeValueAsString(item)));
                    out.println(objectMapper.writeValueAsString(new Message("NOTIFY", "Payment successful! Item is now PAID.")));
                } else {
                    out.println(objectMapper.writeValueAsString(new Message("ERROR", "Insufficient balance to pay for this item!")));
                    // Nếu muốn CANCELED luôn:
                    // item.setStatus(Item.Status.CANCELED);
                    // itemDAO.save(item);
                    // broadcast(new Message("ITEM_STATUS_CHANGED", objectMapper.writeValueAsString(item)));
                }
            } catch (Exception e) {
                try { out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage()))); }
                catch (Exception ignored) {}
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