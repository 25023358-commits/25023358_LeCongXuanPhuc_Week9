package com.auction.controller;

import com.auction.client.ClientConnection;
import com.auction.dao.UserDAO;
import com.auction.entity.Item;
import com.auction.entity.Message;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.stream.Collectors;

public class AdminDashboardController {

    @FXML private TableView<UserDAO.UserRecord> userTable;
    @FXML private TableColumn<UserDAO.UserRecord, String> usernameColumn;
    @FXML private TableColumn<UserDAO.UserRecord, String> roleColumn;
    @FXML private TableColumn<UserDAO.UserRecord, String> balanceColumn;
    @FXML private TableColumn<UserDAO.UserRecord, Void> actionColumn;

    @FXML private TableView<Item> itemTable;
    @FXML private TableColumn<Item, String> itemNameColumn;
    @FXML private TableColumn<Item, String> itemStatusColumn;
    @FXML private TableColumn<Item, String> itemPriceColumn;
    @FXML private TableColumn<Item, Void> itemActionColumn;

    @FXML private ListView<String> systemLogList;
    @FXML private Label totalUsersLabel;
    @FXML private Label activeItemsLabel;
    @FXML private Label systemVolumeLabel;
    @FXML private TextField userSearchField;

    private ClientConnection connection;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private List<UserDAO.UserRecord> allUsers = new java.util.ArrayList<>();

    @FXML
    public void initialize() {
        // User Table Config
        usernameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().username));
        roleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().role));
        balanceColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("$%.2f", cellData.getValue().balance)));

        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("Ban User");
            {
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 10px; -fx-cursor: hand;");
                deleteBtn.setOnAction(event -> handleDeleteUser(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    UserDAO.UserRecord u = getTableView().getItems().get(getIndex());
                    setGraphic("ADMIN".equals(u.role) ? null : deleteBtn);
                }
            }
        });

        // Item Table Config
        itemNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        itemStatusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus().toString()));
        itemPriceColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("$%.2f", cellData.getValue().getCurrentHighestBid())));

        itemActionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button removeBtn = new Button("Remove");
            {
                removeBtn.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-size: 10px; -fx-cursor: hand;");
                removeBtn.setOnAction(event -> handleRemoveItem(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : removeBtn);
            }
        });

        userSearchField.textProperty().addListener((obs, oldVal, newVal) -> filterUsers(newVal));
        addSystemLog("SYSTEM: Authority initialized. Awaiting logs...");
    }

    public void setConnection(ClientConnection connection) {
        this.connection = connection;
        handleRefreshAll();
    }

    @FXML
    public void handleRefreshAll() {
        if (connection != null) {
            try {
                connection.sendMessage(new Message("GET_ALL_USERS", ""));
                connection.sendMessage(new Message("GET_ITEMS", ""));
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public void loadUsers(String usersJson) {
        try {
            List<UserDAO.UserRecord> users = objectMapper.readValue(usersJson, new TypeReference<List<UserDAO.UserRecord>>() {});
            this.allUsers = users;
            Platform.runLater(() -> {
                userTable.setItems(FXCollections.observableArrayList(users));
                totalUsersLabel.setText(String.valueOf(users.size()));
                double totalBalance = users.stream().mapToDouble(u -> u.balance).sum();
                systemVolumeLabel.setText(String.format("$%.2f", totalBalance));
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void loadItems(String itemsJson) {
        try {
            List<Item> items = objectMapper.readValue(itemsJson, new TypeReference<List<Item>>() {});
            Platform.runLater(() -> {
                itemTable.setItems(FXCollections.observableArrayList(items));
                activeItemsLabel.setText(String.valueOf(items.size()));
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void addSystemLog(String message) {
        Platform.runLater(() -> {
            String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            systemLogList.getItems().add(0, "[" + time + "] " + message);
            if (systemLogList.getItems().size() > 50) systemLogList.getItems().remove(50);
        });
    }

    private void filterUsers(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            userTable.setItems(FXCollections.observableArrayList(allUsers));
            return;
        }
        String lower = searchText.toLowerCase();
        List<UserDAO.UserRecord> filtered = allUsers.stream()
                .filter(u -> u.username.toLowerCase().contains(lower))
                .collect(Collectors.toList());
        userTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void handleDeleteUser(UserDAO.UserRecord user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Ban/Delete " + user.username + "?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try { connection.sendMessage(new Message("DELETE_USER", user.id)); } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    private void handleRemoveItem(Item item) {
        // Simple removal for now
        addSystemLog("ADMIN: Action 'Remove Item' triggered for " + item.getName());
        // Could add specific DELETE_ITEM message if needed
    }
}
