package com.auction.controller;

import com.auction.client.ClientConnection;
import com.auction.entity.Item;
import com.auction.entity.Message;
import com.auction.entity.User;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
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
    @FXML private TableColumn<Item, String> descColumn;
    @FXML private TableColumn<Item, String> priceColumn;
    @FXML private TableColumn<Item, String> endTimeColumn;
    @FXML private TableColumn<Item, Void> actionColumn;

    private ClientConnection connection;
    private User currentUser;

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        descColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));
        priceColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("$%.2f", cellData.getValue().getStartingPrice())));
        endTimeColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getEndTime() != null) {
                return new SimpleStringProperty(cellData.getValue().getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
            return new SimpleStringProperty("N/A");
        });

        // Setup Action Column
        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏ Edit");
            private final Button deleteBtn = new Button("🗑 Delete");
            private final HBox pane = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");

                editBtn.setOnAction(event -> {
                    Item item = getTableView().getItems().get(getIndex());
                    handleEditItem(item);
                });

                deleteBtn.setOnAction(event -> {
                    Item item = getTableView().getItems().get(getIndex());
                    handleDeleteItem(item);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    public void setConnectionAndUser(ClientConnection connection, User user) {
        this.connection = connection;
        this.currentUser = user;
    }

    public void fetchMyItems() {
        if (connection != null && currentUser != null) {
            try {
                connection.sendMessage(new Message("GET_SELLER_ITEMS", currentUser.getId()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void loadItems(List<Item> items) {
        Platform.runLater(() -> itemTable.setItems(FXCollections.observableArrayList(items)));
    }

    @FXML
    private void handleCreateItem() {
        openCreateEditModal(null);
    }

    private void handleEditItem(Item item) {
        openCreateEditModal(item);
    }

    private void handleDeleteItem(Item item) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Item");
        alert.setHeaderText("Are you sure you want to delete '" + item.getName() + "'?");
        alert.setContentText("This action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK || response == ButtonType.YES) {
                try {
                    connection.sendMessage(new Message("DELETE_ITEM", item.getId()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
            if (item != null) {
                controller.setEditingItem(item);
            }

            Stage stage = new Stage();
            stage.setTitle(item == null ? "Create New Item" : "Edit Item");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setOnHidden(event -> fetchMyItems());
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
