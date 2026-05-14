package com.auction.controller;

import com.auction.entity.Item;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.auction.client.ClientConnection;
import com.auction.entity.User;
import com.auction.entity.Message;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * ItemDetailController — Hiển thị chi tiết đầy đủ của một item đấu giá.
 * Nhận dữ liệu JSON từ server (type: ITEM_DETAILS).
 */
public class ItemDetailController {

    @FXML private Label itemNameLabel;
    @FXML private Label categoryLabel;
    @FXML private Label sellerLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label startingPriceLabel;
    @FXML private Label currentBidLabel;
    @FXML private Label bidCountLabel;
    @FXML private Label currentLeaderLabel;
    @FXML private Label startTimeLabel;
    @FXML private Label endTimeLabel;
    @FXML private Label statusLabel;
    @FXML private Label extraInfoLabel;
    @FXML private Button btnPayNow;

    private ClientConnection connection;
    private User currentUser;
    private Item currentItem;

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void setConnectionAndUser(ClientConnection connection, User user) {
        this.connection = connection;
        this.currentUser = user;
    }

    /**
     * Được gọi từ AuctionController sau khi nhận ITEM_DETAILS từ server.
     */
    public void loadDetails(String detailsJson) {
        try {
            JsonNode root = mapper.readTree(detailsJson);

            // Đọc item object
            Item item = mapper.treeToValue(root.get("item"), Item.class);
            this.currentItem = item;
            String sellerName = root.has("sellerName") ? root.get("sellerName").asText() : "Unknown";
            int bidCount = root.has("bidCount") ? root.get("bidCount").asInt() : 0;
            double highestBid = root.has("highestBidAmount") ? root.get("highestBidAmount").asDouble() : 0.0;
            String winnerName = root.has("winnerName") ? root.get("winnerName").asText() : "N/A";

            // Cập nhật UI an toàn (null-check)
            if (itemNameLabel != null) itemNameLabel.setText(item.getName());
            if (sellerLabel != null) sellerLabel.setText(sellerName);
            if (descriptionLabel != null) descriptionLabel.setText(item.getDescription());
            if (startingPriceLabel != null) startingPriceLabel.setText(String.format("$%.2f", item.getStartingPrice()));
            if (currentBidLabel != null) currentBidLabel.setText(String.format("$%.2f", item.getCurrentHighestBid()));
            if (bidCountLabel != null) bidCountLabel.setText(bidCount + " bids");
            if (currentLeaderLabel != null) currentLeaderLabel.setText(winnerName);
            if (statusLabel != null) statusLabel.setText(item.getStatus() != null ? item.getStatus().name() : "N/A");

            // Thời gian
            if (startTimeLabel != null) startTimeLabel.setText(item.getStartTime() != null ? item.getStartTime().format(DT_FMT) : "N/A");
            if (endTimeLabel != null) endTimeLabel.setText(item.getEndTime() != null ? item.getEndTime().format(DT_FMT) : "N/A");

            // Loại & thông tin thêm
            String type = item.getType();
            if ("ELECTRONICS".equals(type)) {
                if (categoryLabel != null) categoryLabel.setText("Electronics");
                if (extraInfoLabel != null && root.get("item").has("warrantyMonths")) {
                    extraInfoLabel.setText("Warranty: " + root.get("item").get("warrantyMonths").asInt() + " months");
                }
            } else if ("ART".equals(type)) {
                if (categoryLabel != null) categoryLabel.setText("Art");
                if (extraInfoLabel != null && root.get("item").has("artistName")) {
                    extraInfoLabel.setText("Artist: " + root.get("item").get("artistName").asText());
                }
            } else {
                if (categoryLabel != null) categoryLabel.setText("General");
                if (extraInfoLabel != null) extraInfoLabel.setText("");
            }

            // Hiện nút Pay Now nếu đang FINISHED và user hiện tại là winner
            if (btnPayNow != null) {
                if (item.getStatus() == Item.Status.FINISHED 
                    && currentUser != null 
                    && currentUser.getId().equals(item.getHighestBidderId())) {
                    btnPayNow.setVisible(true);
                    btnPayNow.setManaged(true);
                } else {
                    btnPayNow.setVisible(false);
                    btnPayNow.setManaged(false);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            itemNameLabel.setText("Error loading details: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) itemNameLabel.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handlePayNow() {
        if (currentItem == null || connection == null) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Payment");
        alert.setHeaderText("Pay for " + currentItem.getName());
        alert.setContentText(String.format("Are you sure you want to pay $%.2f for this item?", currentItem.getCurrentHighestBid()));

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    connection.sendMessage(new Message("PAY_ITEM", currentItem.getId()));
                    handleClose();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
