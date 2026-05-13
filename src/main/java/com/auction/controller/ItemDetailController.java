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

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Được gọi từ AuctionController sau khi nhận ITEM_DETAILS từ server.
     */
    public void loadDetails(String detailsJson) {
        try {
            JsonNode root = mapper.readTree(detailsJson);

            // Đọc item object
            Item item = mapper.treeToValue(root.get("item"), Item.class);
            String sellerName = root.has("sellerName") ? root.get("sellerName").asText() : "Unknown";
            int bidCount = root.has("bidCount") ? root.get("bidCount").asInt() : 0;
            double highestBid = root.has("highestBidAmount") ? root.get("highestBidAmount").asDouble() : 0.0;
            String winnerName = root.has("winnerName") ? root.get("winnerName").asText() : "N/A";

            // Cập nhật UI
            itemNameLabel.setText(item.getName());
            sellerLabel.setText(sellerName);
            descriptionLabel.setText(item.getDescription());
            startingPriceLabel.setText(String.format("$%.2f", item.getStartingPrice()));
            currentBidLabel.setText(String.format("$%.2f", item.getCurrentHighestBid()));
            bidCountLabel.setText(String.valueOf(bidCount));
            currentLeaderLabel.setText(winnerName);
            statusLabel.setText(item.getStatus() != null ? item.getStatus().name() : "N/A");

            // Thời gian
            startTimeLabel.setText(item.getStartTime() != null
                    ? item.getStartTime().format(DT_FMT) : "N/A");
            endTimeLabel.setText(item.getEndTime() != null
                    ? item.getEndTime().format(DT_FMT) : "N/A");

            // Loại & thông tin thêm
            String type = item.getType();
            if ("ELECTRONICS".equals(type)) {
                categoryLabel.setText("Electronics");
                if (root.get("item").has("warrantyMonths")) {
                    extraInfoLabel.setText("Warranty: " + root.get("item").get("warrantyMonths").asInt() + " months");
                }
            } else if ("ART".equals(type)) {
                categoryLabel.setText("Art");
                if (root.get("item").has("artistName")) {
                    extraInfoLabel.setText("Artist: " + root.get("item").get("artistName").asText());
                }
            } else {
                categoryLabel.setText(type != null ? type : "General");
                extraInfoLabel.setText("");
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
}
