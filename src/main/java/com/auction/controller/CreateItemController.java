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
    private Spinner<Integer> startHourSpinner;
    @FXML
    private Spinner<Integer> startMinuteSpinner;
    
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private Spinner<Integer> endHourSpinner;
    @FXML
    private Spinner<Integer> endMinuteSpinner;
    
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
    private Item editingItem;
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

    public void setEditingItem(Item item) {
        this.editingItem = item;
        if (item != null) {
            nameField.setText(item.getName());
            descriptionArea.setText(item.getDescription());
            priceField.setText(String.valueOf(item.getStartingPrice()));
            if (item.getStartTime() != null) {
                startDatePicker.setValue(item.getStartTime().toLocalDate());
                startHourSpinner.getValueFactory().setValue(item.getStartTime().getHour());
                startMinuteSpinner.getValueFactory().setValue(item.getStartTime().getMinute());
            }
            if (item.getEndTime() != null) {
                endDatePicker.setValue(item.getEndTime().toLocalDate());
                endHourSpinner.getValueFactory().setValue(item.getEndTime().getHour());
                endMinuteSpinner.getValueFactory().setValue(item.getEndTime().getMinute());
            }
            
            if (item instanceof com.auction.entity.Electronics) {
                categoryComboBox.setValue("Electronics");
                warrantyField.setText(String.valueOf(((com.auction.entity.Electronics) item).getWarrantyMonths()));
            } else if (item instanceof com.auction.entity.Art) {
                categoryComboBox.setValue("Art");
                artistField.setText(((com.auction.entity.Art) item).getArtistName());
            }
            categoryComboBox.setDisable(true); // Don't allow changing category when editing
        }
    }

    @FXML
    public void initialize() {
        // Xóa sạch trước khi thêm để tránh trùng lặp nếu initialize gọi lại
        categoryComboBox.getItems().clear();
        categoryComboBox.getItems().addAll("Electronics", "Art");

        // Khởi tạo Spinner Giờ (0-23) và Phút (0-59)
        startHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 8));
        endHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 20));
        startMinuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        endMinuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));

        categoryComboBox.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            boolean isElectronics = "Electronics".equals(newValue);
            boolean isArt = "Art".equals(newValue);
            
            electronicsPane.setVisible(isElectronics);
            electronicsPane.setManaged(isElectronics);
            artPane.setVisible(isArt);
            artPane.setManaged(isArt);
            
            if (newValue != null) {
                messageLabel.setText(""); // Xóa thông báo lỗi khi đã chọn category
            }
        });
    }

    @FXML
    private void handleCreateItem() {
        if (connection == null || currentSeller == null) {
            messageLabel.setText("Error: Not connected or not logged in.");
            messageLabel.setTextFill(javafx.scene.paint.Color.RED);
            return;
        }

        // 1. Kiểm tra các trường cơ bản
        if (nameField.getText().trim().isEmpty()) {
            showError("Item Name is required.");
            return;
        }
        if (descriptionArea.getText().trim().isEmpty()) {
            showError("Description is required.");
            return;
        }
        if (priceField.getText().trim().isEmpty()) {
            showError("Starting Price is required.");
            return;
        }
        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            showError("Please select both Start and End dates.");
            return;
        }
        
        String category = categoryComboBox.getValue();
        if (category == null) {
            showError("Please select a Category (Electronics or Art).");
            return;
        }

        try {
            double price = Double.parseDouble(priceField.getText());
            
            int sHour = startHourSpinner.getValue();
            int sMin = startMinuteSpinner.getValue();
            LocalDateTime startTime = startDatePicker.getValue().atTime(sHour, sMin);
            
            int eHour = endHourSpinner.getValue();
            int eMin = endMinuteSpinner.getValue();
            LocalDateTime endTime = endDatePicker.getValue().atTime(eHour, eMin);

            if (endTime.isBefore(startTime)) {
                showError("End time must be after start time.");
                return;
            }

            // 2. Kiểm tra các trường đặc thù
            if (category.equals("Electronics")) {
                if (warrantyField.getText().trim().isEmpty()) {
                    showError("Warranty months is required for Electronics.");
                    return;
                }
            } else if (category.equals("Art")) {
                if (artistField.getText().trim().isEmpty()) {
                    showError("Artist name is required for Art.");
                    return;
                }
            }

            // ... (phần còn lại giữ nguyên)
            // Tạo ID cho sản phẩm hoặc dùng ID cũ nếu đang Edit
            String itemId = (editingItem != null) ? editingItem.getId() : "ITM_" + UUID.randomUUID().toString().substring(0, 8);
            Item newItem = null;

            // Dùng ItemFactory để tạo đối tượng Item
            if (category.equals("Electronics")) {
                int warranty = Integer.parseInt(warrantyField.getText().trim());
                newItem = ItemFactory.createItem("electronics", itemId, nameField.getText(), descriptionArea.getText(), price, startTime, endTime, currentSeller.getId(), warranty);
            } else if (category.equals("Art")) {
                newItem = ItemFactory.createItem("art", itemId, nameField.getText(), descriptionArea.getText(), price, startTime, endTime, currentSeller.getId(), artistField.getText().trim());
            }

            if (newItem == null) return;

            // Đóng gói đối tượng Item thành JSON
            String itemJson = objectMapper.writeValueAsString(newItem);
            String messageType = (editingItem != null) ? "UPDATE_ITEM" : "CREATE_ITEM";
            Message msg = new Message(messageType, itemJson);

            Platform.runLater(() -> {
                messageLabel.setTextFill(javafx.scene.paint.Color.BLUE);
                messageLabel.setText((editingItem != null ? "Updating" : "Creating") + " item...");
            });

            Thread sendThread = new Thread(() -> {
                try {
                    connection.sendMessage(msg);
                    Platform.runLater(() -> {
                        messageLabel.setTextFill(javafx.scene.paint.Color.GREEN);
                        messageLabel.setText("Success!");
                        nameField.getScene().getWindow().hide();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> showError("Network error: " + ex.getMessage()));
                }
            });
            sendThread.setDaemon(true);
            sendThread.start();

        } catch (NumberFormatException e) {
            showError("Price and Warranty must be valid numbers.");
        } catch (Exception e) {
            showError("Error: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        messageLabel.setTextFill(javafx.scene.paint.Color.RED);
        messageLabel.setText(msg);
    }

    @FXML
    private void handleCancel() {
        nameField.getScene().getWindow().hide();
    }
}