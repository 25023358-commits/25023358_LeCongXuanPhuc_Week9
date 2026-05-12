package com.auction.controller;

import com.auction.client.ClientConnection;
import com.auction.entity.Message;
import com.auction.entity.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private RadioButton bidderRadioButton;

    @FXML
    private RadioButton sellerRadioButton;

    @FXML
    private Button registerButton;

    @FXML
    private Label messageLabel;

    private final ToggleGroup roleToggleGroup = new ToggleGroup();
    private ClientConnection connection;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RegisterController() {
        try {
            connection = new ClientConnection();
        } catch (Exception e) {
            Platform.runLater(() -> messageLabel.setText("Cannot connect to server."));
        }
    }

    @FXML
    public void initialize() {
        bidderRadioButton.setToggleGroup(roleToggleGroup);
        sellerRadioButton.setToggleGroup(roleToggleGroup);
        bidderRadioButton.setSelected(true);
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        if (connection == null) {
            messageLabel.setText("No connection to server.");
            return;
        }

        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String role = bidderRadioButton.isSelected() ? "BIDDER" : "SELLER";

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Username and Password cannot be empty.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            messageLabel.setText("Passwords do not match.");
            return;
        }

        // Tạm thời để email mặc định, sau này có thể thêm trường email vào giao diện
        String email = username + "@example.com";

        try {
            RegisterRequest req = new RegisterRequest();
            req.setUsername(username);
            req.setPassword(password);
            req.setEmail(email);
            req.setRole(role);

            Message msg = new Message("REGISTER", objectMapper.writeValueAsString(req));
            connection.sendMessage(msg);

            Message response = connection.receiveMessage();

            if ("REGISTER_SUCCESS".equals(response.getType())) {
                Platform.runLater(() -> {
                    messageLabel.setTextFill(javafx.scene.paint.Color.GREEN);
                    messageLabel.setText("Registration successful! You can now login.");
                    // Bạn có thể tự động chuyển hướng về màn hình Login sau vài giây ở đây
                });
            } else {
                Platform.runLater(() -> {
                    messageLabel.setTextFill(javafx.scene.paint.Color.RED);
                    messageLabel.setText(response.getData());
                });
            }
        } catch (Exception e) {
            Platform.runLater(() -> messageLabel.setText("Error: " + e.getMessage()));
        }
    }

    @FXML
    private void switchToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent loginRoot = loader.load();

            Stage stage = (Stage) registerButton.getScene().getWindow();
            Scene scene = new Scene(loginRoot);
            stage.setScene(scene);
            stage.setTitle("Auction System Login");

        } catch (IOException e) {
            e.printStackTrace();
            messageLabel.setText("Error: Could not load login screen.");
        }
    }
}