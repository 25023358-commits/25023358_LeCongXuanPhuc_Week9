package com.auction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import com.auction.client.ClientConnection;
import com.auction.entity.LoginRequest;
import com.auction.entity.Message;

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

    public LoginController() {
        try {
            connection = new ClientConnection();
        } catch (Exception e) {
            messageLabel.setText("Cannot connect to server");
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        try {
            LoginRequest req = new LoginRequest();
            req.setUsername(username);
            req.setPassword(password);
            Message msg = new Message("LOGIN", new ObjectMapper().writeValueAsString(req));
            connection.sendMessage(msg);
            Message response = connection.receiveMessage();
            if ("LOGIN_SUCCESS".equals(response.getType())) {
                Platform.runLater(() -> messageLabel.setText("Login successful"));
            } else {
                Platform.runLater(() -> messageLabel.setText("Login failed"));
            }
        } catch (Exception e) {
            Platform.runLater(() -> messageLabel.setText("Error: " + e.getMessage()));
        }
    }
}
