package com.auction.controller;

import com.auction.client.AuctionClient;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

public class WelcomeController {

    @FXML
    private void goToLogin(ActionEvent event) {
        try {
            AuctionClient.showLogin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToRegister(ActionEvent event) {
        try {
            // Lấy Stage hiện tại từ sự kiện của nút bấm
            Button sourceButton = (Button) event.getSource();
            Stage stage = (Stage) sourceButton.getScene().getWindow();

            // Tải file register.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/register.fxml"));
            Parent root = loader.load();

            // Cập nhật Scene
            Scene scene = new Scene(root, 900, 600);
            stage.setScene(scene);
            stage.setTitle("Auction System - Register");

            // Căn giữa
            javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            stage.setX((bounds.getWidth() - 900) / 2);
            stage.setY((bounds.getHeight() - 600) / 2);
            
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi: Không thể tải màn hình Đăng ký.");
        }
    }
}