package com.auction.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.auction.service.AuctionManager;
import com.auction.util.DBHelper;
import com.auction.service.AuthService;
import com.auction.dao.ItemDAO;
import com.auction.dao.BidTransactionDAO;
import com.auction.entity.Message;
import com.auction.entity.LoginRequest;
import com.auction.entity.User;
import com.auction.entity.BidRequest;
import com.auction.entity.RegisterRequest;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuctionServer {
    private static final int PORT = 12345;
    private ExecutorService executor = Executors.newCachedThreadPool();
    private AuctionManager auctionManager = new AuctionManager();
    private ObjectMapper objectMapper = new ObjectMapper();
    private AuthService authService = new AuthService(); // Khởi tạo 1 lần, tái sử dụng

    public void start() {
        DBHelper.initializeDatabase();
        System.out.println("Server started on port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
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
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleMessage(Message msg) throws Exception {
            switch (msg.getType()) {
                case "LOGIN":
                    handleLogin(msg);
                    break;
                case "REGISTER":
                    handleRegister(msg);
                    break;
                case "GET_ITEMS":
                case "LIST_ITEMS":
                    handleListItems();
                    break;
                case "BID":
                    handleBid(msg);
                    break;
                // Add more cases
            }
        }

        private void handleLogin(Message msg) throws IOException {
            try {
                LoginRequest req = objectMapper.readValue(msg.getData(), LoginRequest.class);
                User user = authService.login(req.getUsername(), req.getPassword());
                if (user != null) {
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

        private void handleListItems() throws IOException {
            ItemDAO itemDAO = new ItemDAO();
            try {
                System.out.println("Fetching items from database...");
                var items = itemDAO.findAll();
                System.out.println("Items fetched successfully: " + items.size()); // In số lượng items
                out.println(objectMapper.writeValueAsString(new Message("ITEM_LIST", objectMapper.writeValueAsString(items))));
            } catch (Exception e) {
                System.out.println("ERROR in handleListItems: " + e.getMessage());
                e.printStackTrace(); // In full stack trace
                out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage())));
            }
        }
        private void handleBid(Message msg) throws IOException {
            try {
                BidRequest req = objectMapper.readValue(msg.getData(), BidRequest.class);
                boolean success = auctionManager.placeBid(req.getItemId(), req.getBidderId(), req.getAmount());
                out.println(objectMapper.writeValueAsString(new Message("BID_RESULT", String.valueOf(success))));
            } catch (Exception e) {
                out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage())));
            }
        }
    }

    public static void main(String[] args) {
        new AuctionServer().start();
    }
}