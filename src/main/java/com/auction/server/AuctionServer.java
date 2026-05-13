package com.auction.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.auction.service.AuctionManager;
import com.auction.util.DBHelper;
import com.auction.dao.BidTransactionDAO;
import com.auction.dao.ItemDAO;
import com.auction.service.AuthService;
import com.auction.dao.ItemDAO;
import com.auction.entity.*;
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
    private final com.auction.service.AuctionScheduler auctionScheduler;

    public AuctionServer() {
        objectMapper.registerModule(new JavaTimeModule());
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
                case "LOGIN": handleLogin(msg); break;
                case "REGISTER": handleRegister(msg); break;
                case "GET_ITEMS": handleListItems(); break;
                case "CREATE_ITEM": handleCreateItem(msg); break;
                case "BID": handleBid(msg); break;
                case "DELETE_ITEM": handleDeleteItem(msg); break;
                case "REGISTER_AUTO_BID": handleRegisterAutoBid(msg); break;
                case "GET_BID_HISTORY": handleGetBidHistory(msg); break;
                case "GET_ANALYTICS": handleGetAnalytics(msg); break;
                case "GET_SELLER_ITEMS": handleGetSellerItems(msg); break;
                case "LOGOUT": handleLogout(); break;
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

        private void handleRegisterAutoBid(Message msg) {
            try {
                out.println(objectMapper.writeValueAsString(new Message("NOTIFY", "Auto-Bid Registered.")));
            } catch (Exception e) {
                 try { out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage()))); } catch (Exception ignored) {}
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