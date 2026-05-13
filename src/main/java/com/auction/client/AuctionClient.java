package com.auction.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AuctionClient extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        // Thiết lập kích thước tối thiểu để không bị vỡ giao diện
        primaryStage.setMinWidth(400);
        primaryStage.setMinHeight(400);
        showWelcome();
    }

    public static void showWelcome() throws Exception {
        FXMLLoader loader = new FXMLLoader(AuctionClient.class.getResource("/welcome.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1000, 650);
        primaryStage.setTitle("Welcome to Auction Exchange");
        primaryStage.setScene(scene);
        primaryStage.show();
        centerWindow();
    }

    public static void showLogin() throws Exception {
        FXMLLoader loader = new FXMLLoader(AuctionClient.class.getResource("/login.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 900, 600); // Tăng size login cho cân đối
        primaryStage.setTitle("Auction Client - Login");
        primaryStage.setScene(scene);
        primaryStage.show();
        centerWindow();
    }

    private static void centerWindow() {
        javafx.geometry.Rectangle2D primScreenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        primaryStage.setX((primScreenBounds.getWidth() - primaryStage.getWidth()) / 2);
        primaryStage.setY((primScreenBounds.getHeight() - primaryStage.getHeight()) / 2);
    }
    
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}