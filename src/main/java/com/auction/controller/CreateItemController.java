package com.auction.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @FXML
    public void initialize() {
        // Thêm các lựa chọn vào ComboBox
        categoryComboBox.getItems().addAll("Electronics", "Art");

        // Thêm Listener để theo dõi sự thay đổi lựa chọn trong ComboBox
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
        // Lấy dữ liệu từ các trường
        String name = nameField.getText();
        String description = descriptionArea.getText();
        String category = categoryComboBox.getValue();
        
        // Kiểm tra dữ liệu đầu vào
        if (name.isEmpty() || description.isEmpty() || priceField.getText().isEmpty() ||
            startDatePicker.getValue() == null || endDatePicker.getValue() == null || category == null) {
            messageLabel.setText("Please fill in all required fields.");
            return;
        }

        try {
            double price = Double.parseDouble(priceField.getText());
            LocalDateTime startTime = startDatePicker.getValue().atStartOfDay();
            LocalDateTime endTime = endDatePicker.getValue().atTime(23, 59, 59); // Kết thúc vào cuối ngày

            if(endTime.isBefore(startTime)) {
                messageLabel.setText("End date must be after start date.");
                return;
            }

            // Logic gửi dữ liệu đến Server sẽ được thêm vào đây
            System.out.println("--- Creating Item ---");
            System.out.println("Name: " + name);
            System.out.println("Category: " + category);
            System.out.println("Price: " + price);
            System.out.println("Start: " + startTime);
            System.out.println("End: " + endTime);

            if (category.equals("Electronics")) {
                int warranty = Integer.parseInt(warrantyField.getText());
                System.out.println("Warranty: " + warranty + " months");
            } else {
                String artist = artistField.getText();
                System.out.println("Artist: " + artist);
            }
            
            messageLabel.setTextFill(javafx.scene.paint.Color.GREEN);
            messageLabel.setText("Item creation request sent to server!");

        } catch (NumberFormatException e) {
            messageLabel.setText("Price and Warranty must be valid numbers.");
        }
    }

    @FXML
    private void handleCancel() {
        // Logic để đóng cửa sổ này
        nameField.getScene().getWindow().hide();
    }
}
