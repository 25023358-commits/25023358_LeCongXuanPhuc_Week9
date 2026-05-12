package com.auction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import com.auction.client.ClientConnection;
import com.auction.entity.LoginRequest;
import com.auction.entity.Message;

import java.io.IOException;

public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label messageLabel;
    @FXML
    private Button loginButton;

    private ClientConnection connection;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LoginController() {
        try {
            connection = new ClientConnection();
        } catch (Exception e) {
            Platform.runLater(() -> messageLabel.setText("Error: Cannot connect to server."));
        }
    }

    @FXML
    private void handleLogin() {
        if (connection == null) {
            messageLabel.setText("No server connection.");
            return;
        }

        String username = usernameField.getText();
        String password = passwordField.getText();
        
        try {
            LoginRequest req = new LoginRequest();
            req.setUsername(username);
            req.setPassword(password);
            
            Message msg = new Message("LOGIN", objectMapper.writeValueAsString(req));
            connection.sendMessage(msg);
            
            Message response = connection.receiveMessage();
            
            if ("LOGIN_SUCCESS".equals(response.getType())) {
                Platform.runLater(() -> {
                    messageLabel.setTextFill(javafx.scene.paint.Color.GREEN);
                    messageLabel.setText("Login successful. Loading dashboard...");
                    navigateToAuctionDashboard(username);
                });
            } else {
                Platform.runLater(() -> {
                     messageLabel.setTextFill(javafx.scene.paint.Color.RED);
                     messageLabel.setText("Login failed: " + response.getData());
                });
            }
        } catch (Exception e) {
            Platform.runLater(() -> {
                 messageLabel.setTextFill(javafx.scene.paint.Color.RED);
                 messageLabel.setText("Error: " + e.getMessage());
            });
        }
    }

    private void navigateToAuctionDashboard(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction.fxml"));
            Parent root = loader.load();

            AuctionController controller = loader.getController();
            controller.setClientConnection(connection);
            controller.setCurrentUserId(username);
            
            // Yêu cầu server gửi danh sách item
            fetchInitialItems(controller);

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setTitle("Live Auction System - User: " + username);
            stage.setScene(new Scene(root, 750, 500));
        } catch (Exception e) {
            System.err.println("Error loading dashboard: " + e.getMessage());
            Platform.runLater(() -> messageLabel.setText("Error loading dashboard."));
        }
    }
    
    private void fetchInitialItems(AuctionController controller) {
         try {
            Message req = new Message("GET_ITEMS", "");
            connection.sendMessage(req);
            
            Message response = connection.receiveMessage();
            if ("ITEM_LIST".equals(response.getType())) {
                // Sửa lỗi: Cần tạo một lớp Wrapper hoặc đọc trực tiếp list từ JSON
                // Ở đây chúng ta tạm dùng mảng ảo để test UI do chưa đồng bộ entity Item với ItemRecord của DAO
                // (Chờ bạn hoàn thiện lớp Item)
                System.out.println("Items fetched: " + response.getData());
                
                /* Code thật sẽ như sau:
                List<Item> items = objectMapper.readValue(response.getData(), new TypeReference<List<Item>>(){});
                ObservableList<Item> observableItems = FXCollections.observableArrayList(items);
                Platform.runLater(() -> controller.loadItems(observableItems));
                */
            }
        } catch (Exception e) {
             System.err.println("Could not fetch initial items: " + e.getMessage());
        }
    }

    @FXML
    private void switchToRegister(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/register.fxml"));
            Parent registerRoot = loader.load();

            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(registerRoot);
            stage.setScene(scene);
            stage.setTitle("Create New Account");

        } catch (IOException e) {
            System.err.println("Error: Could not load registration screen: " + e.getMessage());
            Platform.runLater(() -> messageLabel.setText("Error: Could not load registration screen."));
        }
    }
}