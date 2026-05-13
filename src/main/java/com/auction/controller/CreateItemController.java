package com.auction.controller;

import com.auction.client.ClientConnection;
import com.auction.entity.Item;
import com.auction.entity.Message;
import com.auction.entity.User;
import com.auction.service.ItemFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.util.UUID;

public class CreateItemController {

    @FXML
    private TextField nameField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextField priceField;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private ComboBox<String> categoryComboBox;
    @FXML
    private VBox electronicsPane;
    @FXML
    private TextField warrantyField;
    @FXML
    private VBox artPane;
    @FXML
    private TextField artistField;
    @FXML
    private Label messageLabel;

    private ClientConnection connection;
    private User currentSeller;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CreateItemController() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    public void setConnection(ClientConnection connection) {
        this.connection = connection;
    }

    public void setCurrentSeller(User currentSeller) {
        this.currentSeller = currentSeller;
    }

    @FXML
    public void initialize() {
        categoryComboBox.getItems().addAll("Electronics", "Art");

        categoryComboBox.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            if (newValue == null) {
                electronicsPane.setVisible(false);
                electronicsPane.setManaged(false);
                artPane.setVisible(false);
                artPane.setManaged(false);
            } else if (newValue.equals("Electronics")) {
                electronicsPane.setVisible(true);
                electronicsPane.setManaged(true);
                artPane.setVisible(false);
                artPane.setManaged(false);
            } else if (newValue.equals("Art")) {
                electronicsPane.setVisible(false);
                electronicsPane.setManaged(false);
                artPane.setVisible(true);
                artPane.setManaged(true);
            }
        });
    }

    @FXML
    private void handleCreateItem() {
        if (connection == null || currentSeller == null) {
            messageLabel.setText("Error: Not connected or not logged in as seller.");
            return;
        }

        String name = nameField.getText();
        String description = descriptionArea.getText();
        String category = categoryComboBox.getValue();

        if (name.isEmpty() || description.isEmpty() || priceField.getText().isEmpty() ||
            startDatePicker.getValue() == null || endDatePicker.getValue() == null || category == null) {
            messageLabel.setText("Please fill in all required fields.");
            return;
        }

        try {
            double price = Double.parseDouble(priceField.getText());
            LocalDateTime startTime = startDatePicker.getValue().atStartOfDay();
            LocalDateTime endTime = endDatePicker.getValue().atTime(23, 59, 59);

            if (endTime.isBefore(startTime)) {
                messageLabel.setText("End date must be after start date.");
                return;
            }

            // Tạo ID cho sản phẩm
            String itemId = "ITM_" + UUID.randomUUID().toString().substring(0, 8);
            Item newItem = null;

            // Dùng ItemFactory để tạo đối tượng Item
            if (category.equals("Electronics")) {
                int warranty = Integer.parseInt(warrantyField.getText());
                newItem = ItemFactory.createItem("electronics", itemId, name, description, price, startTime, endTime, currentSeller.getId(), warranty);
            } else if (category.equals("Art")) {
                String artist = artistField.getText();
                if (artist.isEmpty()) {
                    messageLabel.setText("Please enter artist name.");
                    return;
                }
                newItem = ItemFactory.createItem("art", itemId, name, description, price, startTime, endTime, currentSeller.getId(), artist);
            }

            if (newItem == null) return;

            // Đóng gói đối tượng Item thành JSON
            String itemJson = objectMapper.writeValueAsString(newItem);
            Message msg = new Message("CREATE_ITEM", itemJson);

            // ✅ FIX: Gửi message trên background thread để không chặn UI Thread.
            // Không gọi connection.receiveMessage() ở đây vì listenThread trong
            // AuctionController đã đang đọc socket — gọi thêm sẽ gây race condition.
            // Response "CREATE_ITEM_SUCCESS" sẽ được xử lý bởi listener của AuctionController.
            Platform.runLater(() -> messageLabel.setText("Creating item..."));
            Thread sendThread = new Thread(() -> {
                try {
                    connection.sendMessage(msg);
                    // Đóng cửa sổ sau khi gửi thành công (server sẽ broadcast NEW_ITEM_ADDED)
                    Platform.runLater(() -> {
                        messageLabel.setTextFill(javafx.scene.paint.Color.GREEN);
                        messageLabel.setText("Item sent to server!");
                        nameField.getScene().getWindow().hide();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        messageLabel.setTextFill(javafx.scene.paint.Color.RED);
                        messageLabel.setText("Network error: " + ex.getMessage());
                    });
                }
            });
            sendThread.setDaemon(true);
            sendThread.start();

        } catch (NumberFormatException e) {
            messageLabel.setText("Price and Warranty must be valid numbers.");
        } catch (Exception e) {
            messageLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        nameField.getScene().getWindow().hide();
    }
}