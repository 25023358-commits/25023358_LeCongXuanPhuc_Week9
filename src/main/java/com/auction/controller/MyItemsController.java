package com.auction.controller;

import com.auction.client.ClientConnection;
import com.auction.entity.Item;
import com.auction.entity.Message;
import com.auction.entity.User;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MyItemsController {

    @FXML private TableView<Item> itemTable;
    @FXML private TableColumn<Item, String> nameColumn;
    @FXML private TableColumn<Item, String> priceColumn;
    @FXML private TableColumn<Item, String> winnerColumn;
    @FXML private TableColumn<Item, String> statusColumn;
    @FXML private TableColumn<Item, Void> actionColumn;

    @FXML private Label totalRevenueLabel;
    @FXML private Label activeAuctionsLabel;
    @FXML private Label totalBidsLabel;
    @FXML private ListView<String> salesLogList;

    private ClientConnection connection;
    private User currentUser;

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        priceColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("$%.2f", cellData.getValue().getCurrentHighestBid())));
        winnerColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getHighestBidderId() != null ? cellData.getValue().getHighestBidderId() : "No bids"
        ));
        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus().toString()));

        // Setup Action Column
        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox pane = new HBox(8, editBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-size: 11px; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 11px; -fx-cursor: hand;");

                editBtn.setOnAction(event -> handleEditItem(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(event -> handleDeleteItem(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    Item i = getTableView().getItems().get(getIndex());
                    // Don't allow delete if already has bids
                    deleteBtn.setDisable(i.getCurrentHighestBid() > i.getStartingPrice());
                    setGraphic(pane);
                }
            }
        });
        
        addSalesLog("SYSTEM: Dashboard ready. Tracking your items...");
    }

    public void setConnectionAndUser(ClientConnection connection, User user) {
        this.connection = connection;
        this.currentUser = user;
    }

    public void fetchMyItems() {
        if (connection != null && currentUser != null) {
            try {
                connection.sendMessage(new Message("GET_SELLER_ITEMS", currentUser.getId()));
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public void loadItems(List<Item> items) {
        Platform.runLater(() -> {
            itemTable.setItems(FXCollections.observableArrayList(items));
            
            // Calculate Stats
            double revenue = items.stream()
                .filter(i -> i.getStatus() == Item.Status.FINISHED || i.getStatus() == Item.Status.PAID || i.getStatus() == Item.Status.RUNNING)
                .mapToDouble(Item::getCurrentHighestBid)
                .sum();
            long active = items.stream().filter(i -> i.getStatus() == Item.Status.RUNNING || i.getStatus() == Item.Status.OPEN).count();
            
            totalRevenueLabel.setText(String.format("$%.2f", revenue));
            activeAuctionsLabel.setText(String.valueOf(active));
            totalBidsLabel.setText(String.valueOf(items.size())); // Simplified for now
        });
    }

    public void addSalesLog(String message) {
        Platform.runLater(() -> {
            String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            salesLogList.getItems().add(0, "[" + time + "] " + message);
            if (salesLogList.getItems().size() > 50) salesLogList.getItems().remove(50);
        });
    }

    @FXML
    private void handleCreateItem() {
        openCreateEditModal(null);
    }

    private void handleEditItem(Item item) {
        openCreateEditModal(item);
    }

    private void handleDeleteItem(Item item) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete '" + item.getName() + "'?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    connection.sendMessage(new Message("DELETE_ITEM", item.getId()));
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    private void openCreateEditModal(Item item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/create-item.fxml"));
            Parent root = loader.load();
            CreateItemController controller = loader.getController();
            controller.setConnection(this.connection);
            controller.setCurrentSeller(this.currentUser);
            if (item != null) controller.setEditingItem(item);

            Stage stage = new Stage();
            stage.setTitle(item == null ? "Create New Item" : "Edit Item");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setOnHidden(event -> fetchMyItems());
            stage.showAndWait();
        } catch (IOException e) { e.printStackTrace(); }
    }
}
