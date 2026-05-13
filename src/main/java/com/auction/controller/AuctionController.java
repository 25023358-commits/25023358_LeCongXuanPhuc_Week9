package com.auction.controller;

import com.auction.client.ClientConnection;
import com.auction.entity.Item;
import com.auction.entity.Message;
import com.auction.entity.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
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
import java.util.List;

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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuctionController() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @FXML
    public void initialize() {
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

            CreateItemController controller = loader.getController();
            controller.setConnection(this.connection);
            controller.setCurrentSeller(this.currentUser);

            Stage stage = new Stage();
            stage.setTitle("Create New Auction Item");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            
            // Lắng nghe sự kiện đóng cửa sổ (bất kể bấm Cancel, X, hay Create thành công)
            stage.setOnHidden(event -> fetchItemsFromServer());
            
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi: Không thể mở cửa sổ tạo sản phẩm.");
        }
    }
    
    private void fetchItemsFromServer() {
         try {
            Message req = new Message("GET_ITEMS", "");
            connection.sendMessage(req);
            
            Message response = connection.receiveMessage();
            if ("ITEM_LIST".equals(response.getType())) {
                List<Item> items = objectMapper.readValue(response.getData(), new TypeReference<List<Item>>(){});
                ObservableList<Item> observableItems = FXCollections.observableArrayList(items);
                loadItems(observableItems);
            }
        } catch (Exception e) {
             System.err.println("Could not fetch items: " + e.getMessage());
        }
    }

    @FXML
    private void handlePlaceBid() {
        bidMessageLabel.setText("Place Bid functionality is under construction.");
    }
}