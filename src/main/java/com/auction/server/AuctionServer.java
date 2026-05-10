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

    public void start() {
        DBHelper.initializeDatabase();
        // Load data from DB if needed

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
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
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleMessage(Message msg) throws Exception {
            AuthService authService = new AuthService();
            ItemDAO itemDAO = new ItemDAO();
            BidTransactionDAO bidDAO = new BidTransactionDAO();

            switch (msg.getType()) {
                case "LOGIN":
                    // data: {"username":"user","password":"pass"}
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
                    break;
                case "LIST_ITEMS":
                    try {
                        var items = itemDAO.getAllItems();
                        out.println(objectMapper.writeValueAsString(new Message("ITEMS", objectMapper.writeValueAsString(items))));
                    } catch (Exception e) {
                        out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage())));
                    }
                    break;
                case "BID":
                    // data: {"itemId":"I1","bidderId":"B1","amount":1100.0}
                    try {
                        BidRequest req = objectMapper.readValue(msg.getData(), BidRequest.class);
                        boolean success = auctionManager.placeBid(req.getItemId(), req.getBidderId(), req.getAmount());
                        out.println(objectMapper.writeValueAsString(new Message("BID_RESULT", String.valueOf(success))));
                    } catch (Exception e) {
                        out.println(objectMapper.writeValueAsString(new Message("ERROR", e.getMessage())));
                    }
                    break;
                // Add more cases
            }
        }
    }

    public static void main(String[] args) {
        new AuctionServer().start();
    }
}