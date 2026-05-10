package com.auction.controller;

import com.auction.client.AuctionClient;
import com.auction.client.ClientConnection;
import com.auction.entity.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class BiddingController {
    @FXML private TextField bidAmountField;
    @FXML private LineChart<Number, Number> priceChart;

    private ClientConnection connection;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final XYChart.Series<Number, Number> series = new XYChart.Series<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final long startTime = System.currentTimeMillis();
    private static final Logger logger = Logger.getLogger(BiddingController.class.getName());

    @FXML
    public void initialize() {
        try {
            connection = new ClientConnection();
            priceChart.getData().add(series);
            series.setName("Bid Price Over Time");

            // Poll for updates every 1 second
            executor.scheduleAtFixedRate(this::updateChart, 0, 1, TimeUnit.SECONDS);
        } catch (IOException e) {
            logger.severe("Error initializing connection: " + e.getMessage());
        }
    }

    @FXML
    private void placeBid() {
        try {
            double amount = Double.parseDouble(bidAmountField.getText());
            Message bidMsg = new Message("BID", "{\"itemId\":\"itemId\",\"amount\":" + amount + "}");
            connection.sendMessage(bidMsg);
            // Handle response if needed
        } catch (Exception e) {
            logger.severe("Error placing bid: " + e.getMessage());
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