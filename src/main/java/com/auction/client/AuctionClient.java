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
        showWelcome();
    }

    public static void showWelcome() throws Exception {
        FXMLLoader loader = new FXMLLoader(AuctionClient.class.getResource("/welcome.fxml"));
        Parent root = loader.load();
        // Set kích thước cố định cho màn hình Welcome (900x600)
        Scene scene = new Scene(root, 900, 600);
        primaryStage.setTitle("Welcome to Auction System");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void showLogin() throws Exception {
        FXMLLoader loader = new FXMLLoader(AuctionClient.class.getResource("/login.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 350, 300);
        primaryStage.setTitle("Auction Client - Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void showBidding() throws Exception {
        FXMLLoader loader = new FXMLLoader(AuctionClient.class.getResource("/bidding.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Auction Client - Bidding Dashboard");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}