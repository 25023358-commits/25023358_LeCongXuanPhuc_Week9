package com.auction.controller;

import com.auction.client.ClientConnection;
import com.auction.entity.BidRequest;
import com.auction.entity.Item;
import com.auction.entity.AuctionObserver;
import com.auction.entity.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AuctionController implements AuctionObserver {

    @FXML private TableView<Item> itemTable;
    @FXML private TableColumn<Item, String> nameColumn;
    @FXML private TableColumn<Item, Number> startPriceColumn;
    @FXML private TableColumn<Item, Number> highestBidColumn;
    @FXML private TableColumn<Item, String> bidderColumn;

    @FXML private TextField bidAmountField;
    @FXML private ListView<String> notificationList;

    private ObservableList<Item> itemList = FXCollections.observableArrayList();
    private String currentUserId = "GUEST_USER";
    private ClientConnection connection;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void setClientConnection(ClientConnection connection) {
        this.connection = connection;
    }

    @FXML
    public void initialize() {
        // Ánh xạ dữ liệu từ class Item vào các cột của TableView
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        startPriceColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getStartingPrice()));
        highestBidColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getCurrentHighestBid()));
        bidderColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getHighestBidderId()));

        itemTable.setItems(itemList);
        
        notificationList.getItems().add("Welcome to the Live Auction System!");
    }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
    }

    public void loadItems(ObservableList<Item> items) {
        this.itemList = items;
        itemTable.setItems(this.itemList);
        
        for (Item item : items) {
            item.addObserver(this);
        }
    }

    @FXML
    private void handlePlaceBid() {
        Item selectedItem = itemTable.getSelectionModel().getSelectedItem();
        
        if (selectedItem == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Error", "Please select an item to bid on!");
            return;
        }

        try {
            double amount = Double.parseDouble(bidAmountField.getText());

            if (connection == null) {
                showAlert(Alert.AlertType.ERROR, "Connection Error", "Not connected to server.");
                return;
            }

            BidRequest bidReq = new BidRequest();
            bidReq.setItemId(selectedItem.getId());
            bidReq.setBidderId(currentUserId);
            bidReq.setAmount(amount);

            Message msg = new Message("BID", objectMapper.writeValueAsString(bidReq));
            connection.sendMessage(msg);

            Message response = connection.receiveMessage();
            if ("BID_RESULT".equals(response.getType()) && "true".equals(response.getData())) {
                bidAmountField.clear();
                notificationList.getItems().add("Bid placed successfully for " + selectedItem.getName());
            } else {
                showAlert(Alert.AlertType.ERROR, "Bid Failed", "Your bid was rejected by the server.");
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please enter a valid number for the bid amount.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Network Error", "Could not send bid: " + e.getMessage());
        }
    }

    @Override
    public void update(String message) {
        Platform.runLater(() -> {
            notificationList.getItems().add(message);
            notificationList.scrollTo(notificationList.getItems().size() - 1);
            itemTable.refresh(); 
        });
    }

    @Override
    public void onAuctionEnd(String itemId, String winnerId, double finalPrice) {
        Platform.runLater(() -> {
            String endMessage = String.format("Auction for Item %s ended. Winner: %s at $%s", itemId, winnerId, finalPrice);
            notificationList.getItems().add(endMessage);
            notificationList.scrollTo(notificationList.getItems().size() - 1);
            itemTable.refresh();
        });
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}