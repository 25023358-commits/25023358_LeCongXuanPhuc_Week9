package src.main.java.com.auction.controller;

import src.main.java.com.auction.client.AuctionClient;
import src.main.java.com.auction.client.ClientConnection;
import src.main.java.com.auction.entity.Message;
import src.main.java.com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BiddingController {
    @FXML private ListView<String> itemList;
    @FXML private TextField bidAmountField;
    @FXML private Button placeBidButton;
    @FXML private LineChart<Number, Number> priceChart;

    private ClientConnection connection;
    private ObjectMapper objectMapper = new ObjectMapper();
    private XYChart.Series<Number, Number> series = new XYChart.Series<>();
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private long startTime = System.currentTimeMillis();

    @FXML
    public void initialize() {
        try {
            connection = new ClientConnection();
            priceChart.getData().add(series);
            series.setName("Bid Price Over Time");

            // Poll for updates every 1 second
            executor.scheduleAtFixedRate(this::updateChart, 0, 1, TimeUnit.SECONDS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void placeBid() {
        try {
            double amount = Double.parseDouble(bidAmountField.getText());
            Message bidMsg = new Message("BID", "itemId", amount); // Assuming itemId from selected
            connection.sendMessage(bidMsg);
            Message response = connection.receiveMessage();
            // Handle response
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateChart() {
        // Simulate receiving bid updates
        // In real app, receive from server
        Platform.runLater(() -> {
            long time = (System.currentTimeMillis() - startTime) / 1000;
            double price = 1000 + Math.random() * 100; // Mock price
            series.getData().add(new XYChart.Data<>(time, price));
        });
    }

    @FXML
    private void logout() throws Exception {
        connection.close();
        executor.shutdown();
        AuctionClient.showLogin();
    }
}
