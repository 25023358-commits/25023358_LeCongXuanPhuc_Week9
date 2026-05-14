package com.auction.controller.auth;

import com.auction.controller.navigation.MainLayoutController;
import com.auction.entity.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import com.auction.client.ClientConnection;
import com.auction.entity.dto.auth.LoginRequest;
import com.auction.entity.message.Message;

import java.io.IOException;

public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField visiblePasswordField;
    @FXML
    private CheckBox showPasswordCheckBox;
    @FXML
    private Label messageLabel;
    @FXML
    private Button loginButton;

    private ClientConnection connection;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LoginController() {
        try {
            connection = new ClientConnection();
            // Đăng ký module để ObjectMapper có thể xử lý LocalDateTime
            objectMapper.registerModule(new JavaTimeModule());
        } catch (Exception e) {
            Platform.runLater(() -> messageLabel.setText("Error: Cannot connect to server."));
        }
    }

    @FXML
    public void initialize() {
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        showPasswordCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            visiblePasswordField.setVisible(newValue);
            visiblePasswordField.setManaged(newValue);
            passwordField.setVisible(!newValue);
            passwordField.setManaged(!newValue);
        });
    }

    @FXML
    private void handleLogin() {
        if (connection == null) {
            messageLabel.setText("No server connection.");
            return;
        }

        String username = usernameField.getText();
        String password = passwordField.getText();
        
        // Chạy login trong thread riêng để tránh treo UI
        new Thread(() -> {
            try {
                LoginRequest req = new LoginRequest();
                req.setUsername(username);
                req.setPassword(password);
                
                Message msg = new Message("LOGIN", objectMapper.writeValueAsString(req));
                connection.sendMessage(msg);
                
                Message response = connection.receiveMessage();
                
                if ("LOGIN_SUCCESS".equals(response.getType())) {
                    User loggedInUser = objectMapper.readValue(response.getData(), User.class);
                    
                    Platform.runLater(() -> {
                        messageLabel.setTextFill(javafx.scene.paint.Color.GREEN);
                        messageLabel.setText("Login successful. Loading dashboard...");
                        navigateToAuctionDashboard(loggedInUser);
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
        }).start();
    }

    private void navigateToAuctionDashboard(User user) {
        try {
            // Kiểm tra resource có tồn tại không
            java.net.URL fxmlLocation = getClass().getResource("/main-layout.fxml");
            if (fxmlLocation == null) {
                throw new java.io.FileNotFoundException("Could not find main-layout.fxml in resources.");
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            MainLayoutController controller = loader.getController();
            controller.setConnectionAndUser(connection, user);
            
            // Tải dữ liệu ban đầu
            fetchInitialItems();

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setTitle("Live Auction System - User: " + user.getUsername());
            Scene scene = new Scene(root, 1100, 750); // Thống nhất size dashboard
            stage.setScene(scene);
            
            // Căn giữa cửa sổ khi đổi size
            javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            stage.setX((bounds.getWidth() - 1100) / 2);
            stage.setY((bounds.getHeight() - 750) / 2);
            
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                messageLabel.setTextFill(javafx.scene.paint.Color.RED);
                messageLabel.setText("Error loading dashboard: " + e.getMessage());
            });
        }
    }
    
    private void fetchInitialItems() {
         new Thread(() -> {
             try {
                Message req = new Message("GET_ITEMS", "");
                connection.sendMessage(req);
                // Phản hồi sẽ được AuctionController lắng nghe và xử lý tự động
            } catch (Exception e) {
                 System.err.println("Could not request initial items: " + e.getMessage());
            }
         }).start();
    }

    @FXML
    private void switchToRegister(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/register.fxml"));
            Parent registerRoot = loader.load();

            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(registerRoot, 900, 600); // Thống nhất size register
            stage.setScene(scene);
            stage.setTitle("Create New Account");

            // Căn giữa
            javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            stage.setX((bounds.getWidth() - 900) / 2);
            stage.setY((bounds.getHeight() - 600) / 2);

        } catch (IOException e) {
            System.err.println("Error: Could not load registration screen: " + e.getMessage());
            Platform.runLater(() -> messageLabel.setText("Error: Could not load registration screen."));
        }
    }
}
