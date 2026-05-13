package com.auction.controller;

import com.auction.client.ClientConnection;
import com.auction.entity.AutoBidRequest;
import com.auction.entity.Item;
import com.auction.entity.Message;
import com.auction.entity.User;
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
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionController {

    @FXML private Button createNewItemButton;
    @FXML private Button myItemsButton;
    @FXML private TableView<Item> itemTable;
    @FXML private TableColumn<Item, String> nameColumn;
    @FXML private TableColumn<Item, String> descriptionColumn;
    @FXML private TableColumn<Item, String> priceColumn;
    @FXML private TableColumn<Item, String> statusColumn;
    @FXML private TableColumn<Item, String> endTimeColumn;
    @FXML private TableColumn<Item, String> myStatusColumn;
    @FXML private Text selectedItemText;
    @FXML private TextField bidAmountField;
    @FXML private Label bidMessageLabel;
    @FXML private TextField maxBidField;
    @FXML private TextField incrementField;
    @FXML private ListView<String> notificationList;
    @FXML private ListView<String> bidHistoryList;
    @FXML private LineChart<String, Number> priceChart;
    @FXML private Button viewDetailsButton;
    @FXML private Label userBalanceLabel;
    @FXML private Button topUpButton;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Label autoBidStatusLabel;

    private ObservableList<Item> masterData = FXCollections.observableArrayList();

    private XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();
    private ClientConnection connection;
    private User currentUser;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService timerExecutor = Executors.newSingleThreadScheduledExecutor();
    private MainLayoutController mainLayoutController;
    private AdminDashboardController adminDashboardController;

    public void setMainLayoutController(MainLayoutController mlc) {
        this.mainLayoutController = mlc;
    }

    public void setAdminDashboardController(AdminDashboardController adc) {
        this.adminDashboardController = adc;
    }

    public AuctionController() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getName()));
        descriptionColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDescription()));
        priceColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("$%.2f", cellData.getValue().getCurrentHighestBid())));
        statusColumn.setCellValueFactory(cellData -> {
            var item = cellData.getValue();
            return new SimpleStringProperty(item.getStatus() != null ? item.getStatus().name() : "N/A");
        });

        myStatusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Item auctionItem = getTableView().getItems().get(getIndex());
                    if (currentUser == null || auctionItem.getHighestBidderId() == null) {
                        setText("-");
                        setStyle("-fx-text-fill: #95a5a6;");
                    } else if (currentUser.getId().equals(auctionItem.getHighestBidderId())) {
                        setText("👑 Winning");
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else {
                        setText("Outbid");
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                }
            }
        });

        endTimeColumn.setCellValueFactory(cellData -> {
            Item item = cellData.getValue();
            LocalDateTime end = item.getEndTime();
            if (end == null) return new SimpleStringProperty("N/A");
            if (end.isBefore(LocalDateTime.now())) return new SimpleStringProperty("Ended");
            Duration duration = Duration.between(LocalDateTime.now(), end);
            return new SimpleStringProperty(String.format("%02dh:%02dm:%02ds", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart()));
        });

        endTimeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    if (item.startsWith("00h:00m") || item.equals("Ended")) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        itemTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedItemText.setText(newSel.getName() + " [" + newSel.getStatus() + "]");
                updateChart(newSel);
                fetchBidHistory(newSel.getId());
                if (viewDetailsButton != null) viewDetailsButton.setDisable(false);
            } else {
                selectedItemText.setText("No item selected");
                priceChart.getData().clear();
                bidHistoryList.getItems().clear();
                if (autoBidStatusLabel != null) autoBidStatusLabel.setText("");
                if (viewDetailsButton != null) viewDetailsButton.setDisable(true);
            }
        });

        priceChart.getData().add(priceSeries);

        // Search & Filter setup
        if (statusFilter != null) {
            statusFilter.getItems().addAll("ALL", "OPEN", "RUNNING", "FINISHED");
            statusFilter.setValue("ALL");
            statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterItems());
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> filterItems());
        }

        // Disable details button until item is selected
        if (viewDetailsButton != null) viewDetailsButton.setDisable(true);

        // Refresh bộ đếm thời gian mỗi giây
        timerExecutor.scheduleAtFixedRate(
                () -> Platform.runLater(() -> itemTable.refresh()),
                1, 1, TimeUnit.SECONDS);
    }

    private void updateChart(Item item) {
        priceSeries.getData().clear();
        priceSeries.setName(item.getName());
        priceSeries.getData().add(new XYChart.Data<>("Start", item.getStartingPrice()));
        priceSeries.getData().add(new XYChart.Data<>("Now", item.getCurrentHighestBid()));
    }

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

                    case "BID_UPDATE": {
                        fetchItemsFromServer();
                        JsonNode node = objectMapper.readTree(msg.getData());
                        String itemName = node.has("itemName") ? node.get("itemName").asText() : "Unknown item";
                        String bidderName = node.has("bidderName") ? node.get("bidderName").asText() : "Unknown";
                        double amount = node.has("amount") ? node.get("amount").asDouble() : 0;

                        String log = String.format("[BID] %s placed $%.2f on \"%s\"",
                                bidderName, amount, itemName);
                        addNotification(log);
                        if (adminDashboardController != null) adminDashboardController.addSystemLog(log);
                        if (mainLayoutController != null && mainLayoutController.getMyItemsController() != null) {
                            mainLayoutController.getMyItemsController().addSalesLog(log);
                        }

                        // Cập nhật biểu đồ nếu item đang được chọn
                        Item selected = itemTable.getSelectionModel().getSelectedItem();
                        if (selected != null && node.has("itemId")
                                && selected.getId().equals(node.get("itemId").asText())) {
                            String timeLabel = LocalDateTime.now()
                                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                            priceSeries.getData().add(new XYChart.Data<>(timeLabel, amount));
                            // Refresh bid history
                            fetchBidHistory(selected.getId());
                        }
                        break;
                    }

                    case "BID_RESULT": {
                        boolean success = Boolean.parseBoolean(msg.getData());
                        if (success) {
                            bidMessageLabel.setTextFill(javafx.scene.paint.Color.GREEN);
                            bidMessageLabel.setText("Bid placed successfully!");
                            bidAmountField.clear();
                        } else {
                            bidMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
                            bidMessageLabel.setText("Bid failed — check balance or amount.");
                        }
                        break;
                    }

                    case "ANTI_SNIPING_TRIGGERED": {
                        fetchItemsFromServer();
                        JsonNode node = objectMapper.readTree(msg.getData());
                        String itemId = node.get("itemId").asText();
                        long rem = node.get("remainingSeconds").asLong();
                        String log = String.format("🛡️ [Anti-Sniping] Auction for \"%s\" extended! New time: %ds", itemId, rem);
                        addNotification(log);
                        if (adminDashboardController != null) adminDashboardController.addSystemLog(log);
                        if (mainLayoutController != null && mainLayoutController.getMyItemsController() != null) {
                            mainLayoutController.getMyItemsController().addSalesLog(log);
                        }
                        break;
                    }

                    case "ITEM_STATUS_CHANGED": {
                        fetchItemsFromServer();
                        try {
                            Item changedItem = objectMapper.readValue(msg.getData(), Item.class);
                            String winner = changedItem.getHighestBidderId();
                            String notif;
                            if (changedItem.getStatus() == Item.Status.FINISHED) {
                                notif = String.format("[CLOSED] \"%s\" auction ended! Winner: %s @ $%.2f",
                                        changedItem.getName(),
                                        winner != null ? winner : "No bids",
                                        changedItem.getCurrentHighestBid());
                            } else {
                                notif = String.format("[STATUS] \"%s\" is now %s",
                                        changedItem.getName(), changedItem.getStatus());
                            }
                            addNotification(notif);
                        } catch (Exception e) {
                            addNotification("[SYSTEM] Item status updated.");
                        }
                        break;
                    }

                    case "NEW_ITEM_ADDED": {
                        fetchItemsFromServer();
                        try {
                            Item newItem = objectMapper.readValue(msg.getData(), Item.class);
                            addNotification("[NEW] \"" + newItem.getName() + "\" is now available for auction!");
                        } catch (Exception e) {
                            addNotification("[NEW] A new item is available for auction!");
                        }
                        break;
                    }

                    case "NOTIFY":
                        addNotification("[INFO] " + msg.getData());
                        break;

                    case "ITEM_LIST": {
                        List<Item> items = objectMapper.readValue(msg.getData(),
                                new TypeReference<List<Item>>() {});
                        loadItems(FXCollections.observableArrayList(items));
                        if (adminDashboardController != null) adminDashboardController.loadItems(msg.getData());
                        break;
                    }

                    case "SELLER_ITEMS": {
                        List<Item> sellerItems = objectMapper.readValue(msg.getData(),
                                new TypeReference<List<Item>>() {});
                        if (mainLayoutController != null && mainLayoutController.getMyItemsController() != null) {
                            mainLayoutController.getMyItemsController().loadItems(sellerItems);
                        } else {
                            loadItems(FXCollections.observableArrayList(sellerItems));
                        }
                        addNotification("[DASHBOARD] Showing your " + sellerItems.size() + " item(s).");
                        break;
                    }

                    case "ALL_USERS":
                        if (adminDashboardController != null) {
                            adminDashboardController.loadUsers(msg.getData());
                        }
                        break;

                    case "DELETE_USER_SUCCESS":
                        Platform.runLater(() -> {
                            addNotification("[ADMIN] Deleted user successfully: " + msg.getData());
                             if (adminDashboardController != null) {
                                 adminDashboardController.handleRefreshAll();
                             }
                        });
                        break;

                    case "BID_HISTORY": {
                        // Server trả về list Map có bidderName
                        List<Map<String, Object>> bids = objectMapper.readValue(msg.getData(),
                                new TypeReference<List<Map<String, Object>>>() {});
                        updateBidHistoryList(bids);
                        break;
                    }

                    case "ITEM_DETAILS": {
                        // Mở cửa sổ Item Details
                        openItemDetailsWindow(msg.getData());
                        break;
                    }

                    case "AUTO_BID_REGISTERED": {
                        JsonNode node = objectMapper.readTree(msg.getData());
                        String itemId = node.get("itemId").asText();
                        double max = node.get("maxBid").asDouble();
                        String log = String.format("🤖 [AUTO-BID] Active for %s (Max: $%.2f)", itemId, max);
                        
                        if (autoBidStatusLabel != null) {
                            autoBidStatusLabel.setText(String.format("ACTIVE: Max $%.2f | Step $%.2f", 
                                max, node.get("increment").asDouble()));
                        }
                        
                        bidMessageLabel.setTextFill(javafx.scene.paint.Color.GREEN);
                        bidMessageLabel.setText("Auto-Bid activated!");
                        addNotification(log);
                        if (adminDashboardController != null) adminDashboardController.addSystemLog(log);
                        break;
                    }

                    case "TOP_UP_SUCCESS": {
                        JsonNode node = objectMapper.readTree(msg.getData());
                        double newBalance = node.get("newBalance").asDouble();
                        addNotification(String.format("[TOP-UP] Added funds successfully. New Balance: $%.2f", newBalance));
                        if (currentUser instanceof com.auction.entity.Bidder) {
                            ((com.auction.entity.Bidder) currentUser).setBalance(newBalance);
                            userBalanceLabel.setText(String.format("Balance: $%.2f", newBalance));
                        }
                        break;
                    }

                    case "ITEM_REMOVED": {
                        fetchItemsFromServer();
                        addNotification("[REMOVED] An item has been deleted.");
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                addNotification("[ERROR] " + e.getMessage());
            }
        });
    }

    private void addNotification(String text) {
        notificationList.getItems().add(0, text);
        // Giới hạn tối đa 100 thông báo
        if (notificationList.getItems().size() > 100) {
            notificationList.getItems().remove(100, notificationList.getItems().size());
        }
    }

    public void setConnection(ClientConnection connection) {
        this.connection = connection;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            if ("SELLER".equals(user.getRole())) {
                if (createNewItemButton != null) {
                    createNewItemButton.setVisible(true);
                    createNewItemButton.setManaged(true);
                }
                if (myItemsButton != null) {
                    myItemsButton.setVisible(true);
                    myItemsButton.setManaged(true);
                }
                if (topUpButton != null) {
                    topUpButton.setVisible(false);
                    topUpButton.setManaged(false);
                }
            }
            if (userBalanceLabel != null && "BIDDER".equals(user.getRole())) {
                com.auction.entity.Bidder bidder = (com.auction.entity.Bidder) user;
                userBalanceLabel.setText(String.format("Balance: $%.2f", bidder.getBalance()));
                if (topUpButton != null) {
                    topUpButton.setVisible(true);
                    topUpButton.setManaged(true);
                }
            }
        }
    }

    public void loadItems(ObservableList<Item> items) {
        Platform.runLater(() -> {
            masterData.setAll(items);
            filterItems();
        });
    }

    private void filterItems() {
        if (searchField == null || statusFilter == null) return;
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String status = statusFilter.getValue() == null ? "ALL" : statusFilter.getValue();

        ObservableList<Item> filtered = FXCollections.observableArrayList();
        for (Item item : masterData) {
            boolean matchesSearch = item.getName().toLowerCase().contains(searchText);
            boolean matchesStatus = status.equals("ALL") || (item.getStatus() != null && item.getStatus().name().equals(status));
            if (matchesSearch && matchesStatus) {
                filtered.add(item);
            }
        }
        itemTable.setItems(filtered);
    }

    private void fetchBidHistory(String itemId) {
        try {
            connection.sendMessage(new Message("GET_BID_HISTORY", itemId));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Hiển thị bid history với username từ server */
    private void updateBidHistoryList(List<Map<String, Object>> bids) {
        Platform.runLater(() -> {
            bidHistoryList.getItems().clear();
            for (Map<String, Object> bid : bids) {
                String name = bid.getOrDefault("bidderName", bid.get("bidderId")).toString();
                double amount = ((Number) bid.get("amount")).doubleValue();
                String status = bid.getOrDefault("status", "").toString();
                bidHistoryList.getItems().add(String.format("$%.2f — %s [%s]", amount, name, status));
            }
        });
    }

    @FXML
    private void handleCreateNewItem() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/create-item.fxml"));
            Parent root = loader.load();
            CreateItemController controller = loader.getController();
            controller.setConnection(this.connection);
            controller.setCurrentSeller(this.currentUser);

            Stage stage = new Stage();
            stage.setTitle("Create New Auction Item");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setOnHidden(event -> fetchItemsFromServer());
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fetchItemsFromServer() {
        try {
            connection.sendMessage(new Message("GET_ITEMS", ""));
        } catch (Exception e) {
            System.err.println("Could not request items: " + e.getMessage());
        }
    }

    @FXML
    private void handlePlaceBid() {
        Item selectedItem = itemTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            bidMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
            bidMessageLabel.setText("Please select an item to bid.");
            return;
        }
        if (selectedItem.getStatus() != Item.Status.RUNNING) {
            bidMessageLabel.setTextFill(javafx.scene.paint.Color.ORANGE);
            bidMessageLabel.setText("Auction is not active (status: " + selectedItem.getStatus() + ").");
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
            Message msg = new Message("BID", objectMapper.writeValueAsString(req));
            connection.sendMessage(msg);
            bidMessageLabel.setTextFill(javafx.scene.paint.Color.GRAY);
            bidMessageLabel.setText("Sending bid...");
        } catch (NumberFormatException e) {
            bidMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
            bidMessageLabel.setText("Invalid amount format.");
        } catch (Exception e) {
            e.printStackTrace();
            bidMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
            bidMessageLabel.setText("Network error: " + e.getMessage());
        }
    }

    /** Auto Bidding — gửi REGISTER_AUTO_BID lên server đầy đủ */
    @FXML
    private void handleAutoBid() {
        Item selectedItem = itemTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            bidMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
            bidMessageLabel.setText("Please select an item for Auto-Bid.");
            return;
        }
        if (currentUser == null || !"BIDDER".equals(currentUser.getRole())) {
            bidMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
            bidMessageLabel.setText("Only bidders can use Auto-Bid.");
            return;
        }
        String maxText = maxBidField.getText().trim();
        String stepText = incrementField.getText().trim();
        if (maxText.isEmpty() || stepText.isEmpty()) {
            bidMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
            bidMessageLabel.setText("Enter Max Bid and Step for Auto-Bid.");
            return;
        }
        try {
            double maxBid = Double.parseDouble(maxText);
            double increment = Double.parseDouble(stepText);
            if (maxBid <= selectedItem.getCurrentHighestBid()) {
                bidMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
                bidMessageLabel.setText("Max bid must be higher than current price.");
                return;
            }
            AutoBidRequest req = new AutoBidRequest(
                    selectedItem.getId(), currentUser.getId(), maxBid, increment);
            connection.sendMessage(new Message("REGISTER_AUTO_BID",
                    objectMapper.writeValueAsString(req)));
            bidMessageLabel.setTextFill(javafx.scene.paint.Color.GRAY);
            bidMessageLabel.setText("Registering auto-bid...");
        } catch (NumberFormatException e) {
            bidMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
            bidMessageLabel.setText("Invalid Auto-Bid parameters.");
        } catch (Exception e) {
            e.printStackTrace();
            bidMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
            bidMessageLabel.setText("Network error: " + e.getMessage());
        }
    }

    /** View Item Details */
    @FXML
    private void handleViewDetails() {
        Item selectedItem = itemTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) return;
        try {
            connection.sendMessage(new Message("GET_ITEM_DETAILS", selectedItem.getId()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openItemDetailsWindow(String detailsJson) {
        try {
            java.net.URL fxmlUrl = getClass().getResource("/item-detail.fxml");
            if (fxmlUrl == null) {
                addNotification("[ERROR] item-detail.fxml not found.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            ItemDetailController controller = loader.getController();
            controller.setConnectionAndUser(this.connection, this.currentUser);
            controller.loadDetails(detailsJson);

            Stage stage = new Stage();
            stage.setTitle("Item Details");
            stage.setScene(new Scene(root, 480, 420));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            addNotification("[ERROR] Cannot open details: " + e.getMessage());
        }
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

    @FXML
    private void handleShowMyItems() {
        try {
            connection.sendMessage(new Message("GET_SELLER_ITEMS", currentUser.getId()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleTopUp() {
        TextInputDialog dialog = new TextInputDialog("100");
        dialog.setTitle("Top Up Balance");
        dialog.setHeaderText("Add funds to your bidding account");
        dialog.setContentText("Enter amount ($):");

        java.util.Optional<String> result = dialog.showAndWait();
        result.ifPresent(amountStr -> {
            try {
                double amount = Double.parseDouble(amountStr);
                if (amount > 0) {
                    Map<String, Object> req = new java.util.HashMap<>();
                    req.put("userId", currentUser.getId());
                    req.put("amount", amount);
                    connection.sendMessage(new Message("TOP_UP", objectMapper.writeValueAsString(req)));
                } else {
                    addNotification("[ERROR] Top-up amount must be positive.");
                }
            } catch (NumberFormatException e) {
                addNotification("[ERROR] Invalid amount format.");
            } catch (Exception e) {
                addNotification("[ERROR] Could not send top-up request.");
            }
        });
    }
}