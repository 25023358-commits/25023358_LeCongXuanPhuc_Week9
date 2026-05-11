package com.auction.client;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AuctionClient extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        showLogin();
    }

    public static void showLogin() throws Exception {
        FXMLLoader loader = new FXMLLoader(AuctionClient.class.getResource("/login.fxml"));
        VBox root = loader.load();
        Scene scene = new Scene(root, 300, 200);
        primaryStage.setTitle("Auction Client - Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void showBidding() throws Exception {
        FXMLLoader loader = new FXMLLoader(AuctionClient.class.getResource("/bidding.fxml"));
        VBox root = loader.load();
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Auction Client - Bidding");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }
}
