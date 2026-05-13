package com.auction.controller;

import com.auction.client.ClientConnection;
import com.auction.entity.Item;
import com.auction.entity.Message;
import com.auction.entity.User;
import com.auction.entity.Admin;
import com.auction.entity.Bidder;
import com.auction.entity.Seller;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionController {

    // ── Header ──────────────────────────────────────────────────
    @FXML private Button createNewItemButton;
    @FXML private Button myItemsButton;
    @FXML private Button adminButton;
    @FXML private Label  balanceDisplayLabel;   // Chức năng 5

    // ── Table ──────────────────────────────────────────────────
    @FXML private TableView<Item>              itemTable;
    @FXML private TableColumn<Item, String>    nameColumn;
    @FXML private TableColumn<Item, String>    descriptionColumn;
    @FXML private TableColumn<Item, String>    priceColumn;
    @FXML private TableColumn<Item, String>    statusColumn;
    @FXML private TableColumn<Item, String>    endTimeColumn;
    @FXML private TableColumn<Item, String>    winnerColumn;   // Chức năng 9

    // ── Right panel ─────────────────────────────────────────────
    @FXML private LineChart<String, Number>    priceChart;
    @FXML private ListView<String>             bidHistoryList;

    // ── Bottom ─────────────────────────────────────────────────
    @FXML private Text                         selectedItemText;
    @FXML private Button                       payButton;       // Chức năng 9
    @FXML private TextField                    bidAmountField;
    @FXML private Label                        bidMessageLabel;
    @FXML private TextField                    maxBidField;
    @FXML private TextField                    incrementField;
    @FXML private ListView<String>             notificationList;

    // ── State ───────────────────────────────────────────────────
    private ClientConnection connection;
    private User currentUser;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService timerExecutor =
            Executors.newSingleThreadScheduledExecutor();
    private final XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();

    // Giữ reference đến Profile/Admin controller để cập nhật live
    private ProfileController activeProfileController = null;
    private AdminController   activeAdminController   = null;

    public AuctionController() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @FXML
    public void initialize() {
        // ── Column factories ────────────────────────────────────
        nameColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getName()));
        descriptionColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getDescription()));
        priceColumn.setCellValueFactory(c ->
                new SimpleStringProperty(String.format("$%.2f",
                        c.getValue().getCurrentHighestBid())));
        statusColumn.setCellValueFactory(c -> {
            Item.Status s = c.getValue().getStatus();
            return new SimpleStringProperty(s != null ? s.name() : "N/A");
        });
        endTimeColumn.setCellValueFactory(c -> {
            Item item = c.getValue();
            LocalDateTime end = item.getEndTime();
            if (end == null) return new SimpleStringProperty("N/A");
            if (end.isBefore(LocalDateTime.now())) return new SimpleStringProperty("Finished");
            Duration dur = Duration.between(LocalDateTime.now(), end);
            return new SimpleStringProperty(
                    String.format("%dh %dm", dur.toHours(), dur.toMinutesPart()));
        });
        // Chức năng 9: cột Winner
        winnerColumn.setCellValueFactory(c -> {
            String winner = c.getValue().getHighestBidderId();
            Item.Status st = c.getValue().getStatus();
            if ((st == Item.Status.FINISHED || st == Item.Status.PAID) && winner != null) {
                return new SimpleStringProperty("🏆 " + winner.substring(0, Math.min(8, winner.length())));
            }
            return new SimpleStringProperty("");
        });

        // ── Selection listener ──────────────────────────────────
        itemTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSel, newSel) -> {
                    if (newSel != null) {
                        selectedItemText.setText(newSel.getName() +
                                "  [" + newSel.getId() + "]");
                        updateChart(newSel);
                        fetchBidHistory(newSel.getId());
                        updatePayButton(newSel);  // Chức năng 9
                    } else {
                        selectedItemText.setText("No item selected");
                        priceChart.getData().clear();
                        bidHistoryList.getItems().clear();
                        payButton.setVisible(false);
                        payButton.setManaged(false);
                    }
                });

        priceChart.getData().add(priceSeries);

        // ── Timer: refresh countdown mỗi giây ──────────────────
        timerExecutor.scheduleAtFixedRate(
                () -> Platform.runLater(() -> itemTable.refresh()),
                1, 1, TimeUnit.SECONDS);
    }

    // ════════════════════════════════════════════════════════════
    // Chức năng 9: hiện/ẩn nút Pay Now
    // ════════════════════════════════════════════════════════════
    private void updatePayButton(Item item) {
        boolean isWinner = currentUser != null
                && currentUser.getId().equals(item.getHighestBidderId());
        boolean isFinished = item.getStatus() == Item.Status.FINISHED;
        boolean show = isFinished && isWinner;
        payButton.setVisible(show);
        payButton.setManaged(show);
    }

    // ════════════════════════════════════════════════════════════
    // Chức năng 7: Price chart
    // ════════════════════════════════════════════════════════════
    private void updateChart(Item item) {
        priceSeries.getData().clear();
        priceSeries.setName(item.getName());
        priceSeries.getData().add(new XYChart.Data<>("Start", item.getStartingPrice()));
        priceSeries.getData().add(new XYChart.Data<>("Current", item.getCurrentHighestBid()));
    }

    // ════════════════════════════════════════════════════════════
    // Message listener (background thread)
    // ════════════════════════════════════════════════════════════
    public void startMessageListener() {
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    Message msg = connection.receiveMessage();
                    handleIncomingMessage(msg);
                }
            } catch (Exception e) {
                System.out.println("Message listener stopped: " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void handleIncomingMessage(Message msg) {
        Platform.runLater(() -> {
            try {
                switch (msg.getType()) {

                    // ── Items ─────────────────────────────────────
                    case "ITEM_LIST": {
                        List<Item> items = objectMapper.readValue(msg.getData(),
                                new TypeReference<List<Item>>() {});
                        ObservableList<Item> obs = FXCollections.observableArrayList(items);
                        itemTable.setItems(obs);
                        // Nếu Admin panel đang mở, cập nhật luôn
                        if (activeAdminController != null) {
                            activeAdminController.applyItems(items);
                        }
                        break;
                    }
                    case "NEW_ITEM_ADDED":
                        fetchItemsFromServer();
                        notify("[NEW] A new item is available!");
                        break;

                    case "ITEM_REMOVED":
                        fetchItemsFromServer();
                        notify("[REMOVED] An item was removed.");
                        break;

                    case "ITEM_STATUS_CHANGED": {
                        fetchItemsFromServer();
                        Item changed = objectMapper.readValue(msg.getData(), Item.class);
                        notify("[STATUS] " + changed.getName() + " → " + changed.getStatus());
                        // Nếu user là winner và item FINISHED → gợi ý thanh toán
                        if (changed.getStatus() == Item.Status.FINISHED
                                && currentUser.getId().equals(changed.getHighestBidderId())) {
                            notify("🏆 Congratulations! You won \"" + changed.getName()
                                    + "\". Select it and click Pay Now.");
                        }
                        break;
                    }

                    // ── Bidding ───────────────────────────────────
                    case "BID_UPDATE":
                        fetchItemsFromServer();
                        notify("[BID] New bid received!");
                        // Update chart if current item
                        Item sel = itemTable.getSelectionModel().getSelectedItem();
                        if (sel != null) {
                            JsonNode n = objectMapper.readTree(msg.getData());
                            if (sel.getId().equals(n.get("itemId").asText())) {
                                double np = n.get("amount").asDouble();
                                priceSeries.getData().add(new XYChart.Data<>(
                                        LocalDateTime.now().format(
                                                java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")),
                                        np));
                            }
                        }
                        break;

                    case "BID_RESULT": {
                        boolean ok = Boolean.parseBoolean(msg.getData());
                        if (ok) {
                            bidMessageLabel.setTextFill(javafx.scene.paint.Color.GREEN);
                            bidMessageLabel.setText("✔ Bid placed!");
                            bidAmountField.clear();
                            refreshBalance();   // Chức năng 5
                        } else {
                            bidMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
                            bidMessageLabel.setText("✗ Bid failed (balance/price).");
                        }
                        break;
                    }

                    // ── Chức năng 3: Auto-bid ─────────────────────
                    case "AUTO_BID_SUCCESS":
                        bidMessageLabel.setTextFill(javafx.scene.paint.Color.BLUE);
                        bidMessageLabel.setText("🤖 " + msg.getData());
                        notify("[AUTO-BID] Registered successfully.");
                        maxBidField.clear();
                        incrementField.clear();
                        break;

                    case "AUTO_BID_FAILED":
                        bidMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
                        bidMessageLabel.setText("Auto-Bid failed: " + msg.getData());
                        break;

                    // ── Chức năng 4: Anti-sniping notification ────
                    case "ANTI_SNIPING":
                        notify("⏰ [Anti-Sniping] " + msg.getData());
                        break;

                    // ── Chức năng 5+8: Balance & Profile ──────────
                    case "BALANCE_INFO": {
                        JsonNode node = objectMapper.readTree(msg.getData());
                        double balance = node.path("balance").asDouble();
                        updateBalanceDisplay(balance);
                        if (activeProfileController != null) {
                            activeProfileController.applyBalance(balance);
                        }
                        break;
                    }
                    case "TOP_UP_SUCCESS": {
                        JsonNode node = objectMapper.readTree(msg.getData());
                        double nb = node.path("newBalance").asDouble();
                        updateBalanceDisplay(nb);
                        if (activeProfileController != null) {
                            activeProfileController.applyBalance(nb);
                        }
                        notify("[TOP-UP] Balance updated to $" + String.format("%.2f", nb));
                        if (activeProfileController != null) {
                            ((javafx.scene.control.Label) activeProfileController
                                    .getClass().getDeclaredField("topUpMessage")
                                    .get(activeProfileController))
                                    .setText("✔ Top-up successful!");
                        }
                        break;
                    }
                    case "TOP_UP_FAILED":
                        notify("[TOP-UP FAILED] " + msg.getData());
                        break;

                    case "PROFILE_INFO": {
                        JsonNode node = objectMapper.readTree(msg.getData());
                        if (activeProfileController != null) {
                            activeProfileController.applyProfileInfo(node);
                        }
                        break;
                    }

                    // ── Chức năng 6: Admin ────────────────────────
                    case "ALL_USERS":
                        if (activeAdminController != null) {
                            activeAdminController.applyAllUsers(msg.getData());
                        }
                        break;

                    case "DELETE_USER_SUCCESS":
                        notify("[ADMIN] User deleted: " + msg.getData());
                        break;

                    case "DELETE_SUCCESS":
                        notify("[ADMIN] Item deleted: " + msg.getData());
                        break;

                    // ── Chức năng 7: Bid history & analytics ──────
                    case "BID_HISTORY": {
                        List<com.auction.entity.BidTransaction> bids = objectMapper.readValue(
                                msg.getData(),
                                new TypeReference<List<com.auction.entity.BidTransaction>>() {});
                        bidHistoryList.getItems().clear();
                        for (com.auction.entity.BidTransaction b : bids) {
                            bidHistoryList.getItems().add(
                                    String.format("$%.2f — %s [%s]",
                                            b.getBidAmount(),
                                            b.getBidderId().substring(0, Math.min(8, b.getBidderId().length())),
                                            b.getStatus()));
                        }
                        break;
                    }
                    case "ANALYTICS": {
                        JsonNode node = objectMapper.readTree(msg.getData());
                        notify(String.format("[STATS] Total bids: %d | Highest: $%.2f",
                                node.path("totalBids").asInt(),
                                node.path("highestBid").asDouble()));
                        break;
                    }

                    // ── Chức năng 9: Thanh toán ───────────────────
                    case "PAYMENT_SUCCESS": {
                        JsonNode node = objectMapper.readTree(msg.getData());
                        double newBalance = node.path("newBalance").asDouble();
                        updateBalanceDisplay(newBalance);
                        if (activeProfileController != null) {
                            activeProfileController.applyBalance(newBalance);
                        }
                        fetchItemsFromServer();
                        notify("💰 Payment successful! New balance: $"
                                + String.format("%.2f", newBalance));
                        new Alert(Alert.AlertType.INFORMATION,
                                "Payment of $" + String.format("%.2f", node.path("amount").asDouble())
                                + " completed!\nNew balance: $" + String.format("%.2f", newBalance))
                                .showAndWait();
                        break;
                    }
                    case "PAYMENT_FAILED":
                        notify("[PAYMENT FAILED] " + msg.getData());
                        new Alert(Alert.AlertType.ERROR,
                                "Payment failed: " + msg.getData()).showAndWait();
                        break;

                    // ── Seller items ──────────────────────────────
                    case "SELLER_ITEMS": {
                        List<Item> sellerItems = objectMapper.readValue(msg.getData(),
                                new TypeReference<List<Item>>() {});
                        itemTable.setItems(FXCollections.observableArrayList(sellerItems));
                        notify("[DASHBOARD] Showing your items (" + sellerItems.size() + ").");
                        break;
                    }

                    case "NOTIFY":
                        notify(msg.getData());
                        break;

                    case "ERROR":
                        notify("[ERROR] " + msg.getData());
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                notify("[ERROR] " + e.getMessage());
            }
        });
    }

    // ════════════════════════════════════════════════════════════
    // Setters (called from LoginController)
    // ════════════════════════════════════════════════════════════
    public void setConnection(ClientConnection connection) {
        this.connection = connection;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user == null) return;
        
        // Sử dụng instanceof để an toàn hơn so với getRole()
        if (user instanceof Seller) {
            createNewItemButton.setVisible(true);
            createNewItemButton.setManaged(true);
            myItemsButton.setVisible(true);
            myItemsButton.setManaged(true);
        }
        
        if (user instanceof Admin) {
            adminButton.setVisible(true);
            adminButton.setManaged(true);
        }
        
        // Chức năng 5: load balance ban đầu (chỉ Bidder có balance)
        if (user instanceof Bidder) {
            refreshBalance();
        } else {
            balanceDisplayLabel.setText(user.getRole());
        }
    }

    public void loadItems(ObservableList<Item> items) {
        Platform.runLater(() -> itemTable.setItems(items));
    }

    // ════════════════════════════════════════════════════════════
    // Helper: balance
    // ════════════════════════════════════════════════════════════
    private void refreshBalance() {
        Thread t = new Thread(() -> {
            try {
                connection.sendMessage(new Message("GET_BALANCE", currentUser.getId()));
            } catch (Exception ignored) {}
        });
        t.setDaemon(true);
        t.start();
    }

    private void updateBalanceDisplay(double balance) {
        balanceDisplayLabel.setText(String.format("Balance: $%.2f", balance));
    }

    private void notify(String text) {
        notificationList.getItems().add(0, text);
        // Giới hạn 50 thông báo
        if (notificationList.getItems().size() > 50) {
            notificationList.getItems().remove(50, notificationList.getItems().size());
        }
    }

    // ════════════════════════════════════════════════════════════
    // Fetch helpers
    // ════════════════════════════════════════════════════════════
    private void fetchItemsFromServer() {
        Thread t = new Thread(() -> {
            try {
                connection.sendMessage(new Message("GET_ITEMS", ""));
            } catch (Exception e) {
                System.err.println("Could not request items: " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void fetchBidHistory(String itemId) {
        Thread t = new Thread(() -> {
            try {
                connection.sendMessage(new Message("GET_BID_HISTORY", itemId));
            } catch (Exception e) { e.printStackTrace(); }
        });
        t.setDaemon(true);
        t.start();
    }

    // ════════════════════════════════════════════════════════════
    // FXML Handlers
    // ════════════════════════════════════════════════════════════

    /** Chức năng 1: Place Bid */
    @FXML
    private void handlePlaceBid() {
        Item selectedItem = itemTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            bidMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
            bidMessageLabel.setText("Please select an item.");
            return;
        }
        String amountText = bidAmountField.getText().trim();
        if (amountText.isEmpty()) {
            bidMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
            bidMessageLabel.setText("Please enter a bid amount.");
            return;
        }
        try {
            double amount = Double.parseDouble(amountText);
            com.auction.entity.BidRequest req = new com.auction.entity.BidRequest();
            req.setItemId(selectedItem.getId());
            req.setBidderId(currentUser.getId());
            req.setAmount(amount);
            Thread t = new Thread(() -> {
                try {
                    connection.sendMessage(new Message("BID",
                            objectMapper.writeValueAsString(req)));
                } catch (Exception e) {
                    Platform.runLater(() -> bidMessageLabel.setText("Error: " + e.getMessage()));
                }
            });
            t.setDaemon(true);
            t.start();
        } catch (NumberFormatException e) {
            bidMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
            bidMessageLabel.setText("Invalid amount.");
        }
    }

    /** Chức năng 3: Auto-bid */
    @FXML
    private void handleAutoBid() {
        Item selectedItem = itemTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            bidMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
            bidMessageLabel.setText("Please select an item.");
            return;
        }
        String maxText = maxBidField.getText().trim();
        String stepText = incrementField.getText().trim();
        if (maxText.isEmpty() || stepText.isEmpty()) {
            bidMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
            bidMessageLabel.setText("Enter Max and Step for Auto-Bid.");
            return;
        }
        try {
            double maxBid    = Double.parseDouble(maxText);
            double increment = Double.parseDouble(stepText);
            Map<String, Object> payload = new HashMap<>();
            payload.put("bidderId",  currentUser.getId());
            payload.put("itemId",    selectedItem.getId());
            payload.put("maxBid",    maxBid);
            payload.put("increment", increment);
            Thread t = new Thread(() -> {
                try {
                    connection.sendMessage(new Message("REGISTER_AUTO_BID",
                            objectMapper.writeValueAsString(payload)));
                } catch (Exception e) {
                    Platform.runLater(() -> bidMessageLabel.setText("Error: " + e.getMessage()));
                }
            });
            t.setDaemon(true);
            t.start();
        } catch (NumberFormatException e) {
            bidMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
            bidMessageLabel.setText("Invalid max/step values.");
        }
    }

    /** Chức năng 9: Thanh toán */
    @FXML
    private void handlePayNow() {
        Item selectedItem = itemTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null || selectedItem.getStatus() != Item.Status.FINISHED) return;
        double amount = selectedItem.getCurrentHighestBid();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Pay $" + String.format("%.2f", amount) + " for \"" + selectedItem.getName() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Payment");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                Thread t = new Thread(() -> {
                    try {
                        Map<String, Object> payload = new HashMap<>();
                        payload.put("itemId",   selectedItem.getId());
                        payload.put("winnerId", currentUser.getId());
                        payload.put("amount",   amount);
                        connection.sendMessage(new Message("FINALIZE_PAYMENT",
                                objectMapper.writeValueAsString(payload)));
                    } catch (Exception e) {
                        Platform.runLater(() ->
                                notify("[PAYMENT ERROR] " + e.getMessage()));
                    }
                });
                t.setDaemon(true);
                t.start();
            }
        });
    }

    /** Chức năng 8: Mở Profile */
    @FXML
    private void handleOpenProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profile.fxml"));
            Parent root = loader.load();
            ProfileController ctrl = loader.getController();
            activeProfileController = ctrl;
            ctrl.init(connection, currentUser);

            Stage stage = new Stage();
            stage.setTitle("My Profile");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setOnHidden(e -> activeProfileController = null);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            notify("[ERROR] Cannot open Profile.");
        }
    }

    /** Chức năng 6: Mở Admin Panel */
    @FXML
    private void handleOpenAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/admin.fxml"));
            Parent root = loader.load();
            AdminController ctrl = loader.getController();
            activeAdminController = ctrl;
            ctrl.init(connection);

            Stage stage = new Stage();
            stage.setTitle("Admin Panel");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setOnHidden(e -> {
                activeAdminController = null;
                fetchItemsFromServer(); // refresh sau khi đóng admin
            });
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            notify("[ERROR] Cannot open Admin Panel.");
        }
    }

    /** Seller: Create new item */
    @FXML
    private void handleCreateNewItem() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/create-item.fxml"));
            Parent root = loader.load();
            CreateItemController ctrl = loader.getController();
            ctrl.setConnection(connection);
            ctrl.setCurrentSeller(currentUser);

            Stage stage = new Stage();
            stage.setTitle("Create New Auction Item");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setOnHidden(e -> fetchItemsFromServer());
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Seller: My items */
    @FXML
    private void handleShowMyItems() {
        Thread t = new Thread(() -> {
            try {
                connection.sendMessage(new Message("GET_SELLER_ITEMS", currentUser.getId()));
            } catch (Exception e) { e.printStackTrace(); }
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleLogout() {
        try {
            if (connection != null) {
                connection.sendMessage(new Message("LOGOUT", ""));
                connection.close();
            }
            timerExecutor.shutdown();
            com.auction.client.AuctionClient.showLogin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}