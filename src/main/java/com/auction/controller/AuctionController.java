package com.auction.controller;

import com.auction.client.ClientConnection;
import com.auction.entity.Item;
import com.auction.entity.Message;
import com.auction.entity.User;
import com.fasterxml.jackson.core.type.TypeReference;
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
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionController {

    @FXML
    private Button createNewItemButton;
    @FXML
    private Button myItemsButton;
    @FXML
    private TableView<Item> itemTable;
    @FXML
    private TableColumn<Item, String> nameColumn;
    @FXML
    private TableColumn<Item, String> descriptionColumn;
    @FXML
    private TableColumn<Item, String> priceColumn;
    @FXML
    private TableColumn<Item, String> statusColumn;
    @FXML
    private TableColumn<Item, String> endTimeColumn;
    @FXML
    private Text selectedItemText;
    @FXML
    private TextField bidAmountField;
    @FXML
    private Label bidMessageLabel;
    @FXML
    private TextField maxBidField;
    @FXML
    private TextField incrementField;
    @FXML
    private ListView<String> notificationList;
    @FXML
    private ListView<String> bidHistoryList;
    @FXML
    private LineChart<String, Number> priceChart;

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
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        descriptionColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));
        priceColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("$%.2f", cellData.getValue().getCurrentHighestBid())));
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
                formatted = String.format("%d hours, %d mins", hours, minutes);
            }
            return new SimpleStringProperty(formatted);
        });

        itemTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedItemText.setText(newSelection.getName());
                updateChart(newSelection);
                fetchBidHistory(newSelection.getId());
            } else {
                selectedItemText.setText("No item selected");
                priceChart.getData().clear();
                bidHistoryList.getItems().clear();
            }
        });
        
        priceChart.getData().add(priceSeries);

        // Bắt đầu đếm ngược thời gian mỗi giây
        timerExecutor.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> itemTable.refresh());
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void updateChart(Item item) {
        priceSeries.getData().clear();
        priceSeries.setName(item.getName());
        // Giả sử ta lấy dữ liệu lịch sử từ server hoặc giả lập
        priceSeries.getData().add(new XYChart.Data<>("Start", item.getStartingPrice()));
        priceSeries.getData().add(new XYChart.Data<>("Now", item.getCurrentHighestBid()));
    }

    public void startMessageListener() {
        new Thread(() -> {
            try {
                while (true) {
                    Message msg = connection.receiveMessage();
                    handleIncomingMessage(msg);
                }
            } catch (Exception e) {
                System.out.println("Message listener stopped: " + e.getMessage());
            }
        }).start();
    }

    private void handleIncomingMessage(Message msg) {
        Platform.runLater(() -> {
            try {
                switch (msg.getType()) {
                    case "BID_UPDATE":
                        fetchItemsFromServer();
                        notificationList.getItems().add(0, "[BID] New bid received!");
                        // Cập nhật biểu đồ nếu item đang chọn là item vừa có bid mới
                        Item selected = itemTable.getSelectionModel().getSelectedItem();
                        if (selected != null) {
                            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(msg.getData());
                            if (selected.getId().equals(node.get("itemId").asText())) {
                                double newPrice = node.get("amount").asDouble();
                                priceSeries.getData().add(new XYChart.Data<>(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")), newPrice));
                            }
                        }
                        break;
                    case "BID_RESULT":
                        boolean success = Boolean.parseBoolean(msg.getData());
                        if (success) {
                            bidMessageLabel.setTextFill(javafx.scene.paint.Color.GREEN);
                            bidMessageLabel.setText("Bid placed successfully!");
                            bidAmountField.clear();
                        } else {
                            bidMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
                            bidMessageLabel.setText("Bid failed (Check balance/price).");
                        }
                        break;
                    case "ITEM_STATUS_CHANGED":
                        fetchItemsFromServer();
                        notificationList.getItems().add(0, "[SYSTEM] Item status updated.");
                        break;
                    case "NEW_ITEM_ADDED":
                        fetchItemsFromServer();
                        notificationList.getItems().add(0, "[NEW] A new item is available for auction!");
                        break;
                    case "NOTIFY":
                        notificationList.getItems().add(0, msg.getData());
                        break;
                    case "ITEM_LIST":
                        List<Item> items = objectMapper.readValue(msg.getData(), new TypeReference<List<Item>>(){});
                        loadItems(FXCollections.observableArrayList(items));
                        break;
                    case "SELLER_ITEMS":
                        List<Item> sellerItems = objectMapper.readValue(msg.getData(), new TypeReference<List<Item>>(){});
                        loadItems(FXCollections.observableArrayList(sellerItems));
                        notificationList.getItems().add(0, "[DASHBOARD] Showing your items.");
                        break;
                    case "BID_HISTORY":
                        List<com.auction.entity.BidTransaction> bids = objectMapper.readValue(msg.getData(), new TypeReference<List<com.auction.entity.BidTransaction>>(){});
                        updateBidHistoryList(bids);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> notificationList.getItems().add(0, "[ERROR] " + e.getMessage()));
            }
        });
    }

    public void setConnection(ClientConnection connection) {
        this.connection = connection;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null && "SELLER".equals(user.getRole())) {
            createNewItemButton.setVisible(true);
            createNewItemButton.setManaged(true);
            myItemsButton.setVisible(true);
            myItemsButton.setManaged(true);
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

    private void updateBidHistoryList(List<com.auction.entity.BidTransaction> bids) {
        Platform.runLater(() -> {
            bidHistoryList.getItems().clear();
            for (com.auction.entity.BidTransaction bid : bids) {
                bidHistoryList.getItems().add(String.format("$%.2f - %s", bid.getBidAmount(), bid.getBidderId()));
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
            
            // Lắng nghe sự kiện đóng cửa sổ (bất kể bấm Cancel, X, hay Create thành công)
            stage.setOnHidden(event -> fetchItemsFromServer());
            
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi: Không thể mở cửa sổ tạo sản phẩm.");
        }
    }
    
    private void fetchItemsFromServer() {
         try {
            Message req = new Message("GET_ITEMS", "");
            connection.sendMessage(req);
            // Phản hồi sẽ được xử lý bất đồng bộ bởi Message Listener qua case "ITEM_LIST"
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

        String amountText = bidAmountField.getText();
        if (amountText.isEmpty()) {
            bidMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
            bidMessageLabel.setText("Please enter a bid amount.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            
            // Tạo BidRequest
            com.auction.entity.BidRequest req = new com.auction.entity.BidRequest();
            req.setItemId(selectedItem.getId());
            req.setBidderId(currentUser.getId());
            req.setAmount(amount);

            // Gửi lên server
            Message msg = new Message("BID", objectMapper.writeValueAsString(req));
            connection.sendMessage(msg);

            // Không nhận phản hồi ở đây để tránh race condition với listener thread
            // Phản hồi sẽ được xử lý trong handleIncomingMessage qua case "BID_RESULT"
            
        } catch (NumberFormatException e) {
            bidMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
            bidMessageLabel.setText("Invalid amount format.");
        } catch (Exception e) {
            e.printStackTrace();
            bidMessageLabel.setTextFill(javafx.scene.paint.Color.RED);
            bidMessageLabel.setText("Network error: " + e.getMessage());
        }
    }

    @FXML
    private void handleAutoBid() {
        Item selectedItem = itemTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            bidMessageLabel.setText("Please select an item for Auto-Bid.");
            return;
        }
        try {
            double maxBid = Double.parseDouble(maxBidField.getText());
            double increment = Double.parseDouble(incrementField.getText());
            
            // Gửi lệnh REGISTER_AUTO_BID lên server
            bidMessageLabel.setTextFill(javafx.scene.paint.Color.BLUE);
            bidMessageLabel.setText("Auto-Bid requested.");
        } catch (Exception e) {
            bidMessageLabel.setText("Invalid Auto-Bid parameters.");
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