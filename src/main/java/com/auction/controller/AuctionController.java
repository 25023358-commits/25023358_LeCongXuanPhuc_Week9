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

    private XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();
    private ClientConnection connection;
    private User currentUser;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService timerExecutor = Executors.newSingleThreadScheduledExecutor();

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
        endTimeColumn.setCellValueFactory(cellData -> {
            Item item = cellData.getValue();
            LocalDateTime end = item.getEndTime();
            if (end == null) return new SimpleStringProperty("N/A");
            String formatted;
            if (end.isBefore(LocalDateTime.now())) {
                formatted = "Finished";
            } else {
                Duration duration = Duration.between(LocalDateTime.now(), end);
                long hours = duration.toHours();
                long minutes = duration.toMinutesPart();
                formatted = String.format("%dh %dm", hours, minutes);
            }
            return new SimpleStringProperty(formatted);
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
                if (viewDetailsButton != null) viewDetailsButton.setDisable(true);
            }
        });

        priceChart.getData().add(priceSeries);

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

                        addNotification(String.format("[BID] %s placed $%.2f on \"%s\"",
                                bidderName, amount, itemName));

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
                        break;
                    }

                    case "SELLER_ITEMS": {
                        List<Item> sellerItems = objectMapper.readValue(msg.getData(),
                                new TypeReference<List<Item>>() {});
                        loadItems(FXCollections.observableArrayList(sellerItems));
                        addNotification("[DASHBOARD] Showing your " + sellerItems.size() + " item(s).");
                        break;
                    }

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
                        bidMessageLabel.setTextFill(javafx.scene.paint.Color.BLUE);
                        bidMessageLabel.setText(String.format(
                                "Auto-Bid ON: max=$%.2f step=$%.2f",
                                node.get("maxBid").asDouble(),
                                node.get("increment").asDouble()));
                        addNotification(String.format("[AUTO-BID] Registered for max=$%.2f step=$%.2f",
                                node.get("maxBid").asDouble(), node.get("increment").asDouble()));
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
                createNewItemButton.setVisible(true);
                createNewItemButton.setManaged(true);
                myItemsButton.setVisible(true);
                myItemsButton.setManaged(true);
            }
            if (userBalanceLabel != null && "BIDDER".equals(user.getRole())) {
                com.auction.entity.Bidder bidder = (com.auction.entity.Bidder) user;
                userBalanceLabel.setText(String.format("Balance: $%.2f", bidder.getBalance()));
            }
        }
    }

    public void loadItems(ObservableList<Item> items) {
        Platform.runLater(() -> itemTable.setItems(items));
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
}