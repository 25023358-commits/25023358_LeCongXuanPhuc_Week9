package com.auction.controller;

import com.auction.client.ClientConnection;
import com.auction.entity.Item;
import com.auction.entity.User;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuctionController {

    @FXML
    private Button createNewItemButton;
    @FXML
    private TableView<Item> itemTable;
    @FXML
    private TableColumn<Item, String> nameColumn;
    @FXML
    private TableColumn<Item, String> descriptionColumn;
    @FXML
    private TableColumn<Item, String> priceColumn;
    @FXML
    private TableColumn<Item, String> statusColumn;
    @FXML
    private TableColumn<Item, String> endTimeColumn;
    @FXML
    private Text selectedItemText;
    @FXML
    private TextField bidAmountField;
    @FXML
    private Label bidMessageLabel;
    @FXML
    private ListView<String> notificationList;

    private ClientConnection connection;
    private User currentUser;

    @FXML
    public void initialize() {
        // Cấu hình các cột của bảng
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        descriptionColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));
        priceColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("$%.2f", cellData.getValue().getCurrentHighestBid())));
        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus().name()));
        endTimeColumn.setCellValueFactory(cellData -> {
            LocalDateTime end = cellData.getValue().getEndTime();
            String formatted;
            if (end.isBefore(LocalDateTime.now())) {
                formatted = "Finished";
            } else {
                Duration duration = Duration.between(LocalDateTime.now(), end);
                long hours = duration.toHours();
                long minutes = duration.toMinutesPart();
                formatted = String.format("%d hours, %d mins", hours, minutes);
            }
            return new SimpleStringProperty(formatted);
        });

        // Listener để cập nhật ô "Selected Item" khi người dùng click vào một dòng
        itemTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedItemText.setText(newSelection.getName());
            } else {
                selectedItemText.setText("No item selected");
            }
        });
    }

    public void setConnection(ClientConnection connection) {
        this.connection = connection;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        // Chỉ hiển thị nút "Create New Item" nếu người dùng là Seller
        if (user != null && "SELLER".equals(user.getRole())) {
            createNewItemButton.setVisible(true);
            createNewItemButton.setManaged(true);
        }
    }

    public void loadItems(ObservableList<Item> items) {
        Platform.runLater(() -> itemTable.setItems(items));
    }

    @FXML
    private void handleCreateNewItem() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/create-item.fxml"));
            Parent root = loader.load();

            // Lấy controller của màn hình tạo item và truyền thông tin cần thiết
            CreateItemController controller = loader.getController();
            // controller.setConnection(this.connection);
            // controller.setCurrentSeller(this.currentUser);

            Stage stage = new Stage();
            stage.setTitle("Create New Auction Item");
            stage.setScene(new Scene(root));
            
            // Dùng Modality.APPLICATION_MODAL để cửa sổ mới hiện lên phải được xử lý xong
            // thì mới quay lại được cửa sổ chính, tránh việc mở nhiều cửa sổ.
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            // Sau khi cửa sổ tạo item đóng, làm mới lại danh sách
            // fetchItemsFromServer();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handlePlaceBid() {
        // Logic đặt giá sẽ được thêm ở đây
        bidMessageLabel.setText("Place Bid functionality is under construction.");
    }
}
