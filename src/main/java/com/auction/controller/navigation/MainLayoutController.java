package com.auction.controller.navigation;

import com.auction.client.ClientConnection;
import com.auction.controller.admin.AdminDashboardController;
import com.auction.controller.auction.AuctionController;
import com.auction.controller.auction.MyItemsController;
import com.auction.entity.message.Message;
import com.auction.entity.user.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import java.io.IOException;

public class MainLayoutController {

    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Button btnDashboard;
    @FXML private Button btnMyItems;
    @FXML private Button btnAdminPanel;
    @FXML private StackPane contentArea;

    private ClientConnection connection;
    private User currentUser;

    private Parent dashboardView;
    private AuctionController auctionController;

    private Parent myItemsView;
    private MyItemsController myItemsController;

    public MyItemsController getMyItemsController() {
        return myItemsController;
    }

    public void setConnectionAndUser(ClientConnection conn, User user) {
        this.connection = conn;
        this.currentUser = user;

        userNameLabel.setText(user.getUsername());
        userRoleLabel.setText(user.getRole());

        if ("SELLER".equals(user.getRole())) {
            btnMyItems.setVisible(true);
            btnMyItems.setManaged(true);
        }

        if ("ADMIN".equals(user.getRole())) {
            btnAdminPanel.setVisible(true);
            btnAdminPanel.setManaged(true);
        }

        loadDashboard();
        
        if ("ADMIN".equals(user.getRole())) {
            showAdminPanel(); // Đè giao diện lên trên
        }
    }

    @FXML
    private void showDashboard() {
        setActiveButton(btnDashboard);
        if (dashboardView == null) {
            loadDashboard();
        } else {
            contentArea.getChildren().setAll(dashboardView);
        }
    }

    @FXML
    private void showMyItems() {
        setActiveButton(btnMyItems);
        if (myItemsView == null) {
            loadMyItems();
        } else {
            contentArea.getChildren().setAll(myItemsView);
            if (myItemsController != null) {
                myItemsController.fetchMyItems();
            }
        }
    }

    @FXML
    public void showAdminPanel() {
        setActiveButton(btnAdminPanel);
        loadAdminPanel();
    }

    @FXML
    private void handleLogout() {
        try {
            if (connection != null) {
                connection.sendMessage(new Message("LOGOUT", ""));
                connection.close();
            }
            com.auction.client.AuctionClient.showLogin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction.fxml"));
            dashboardView = loader.load();
            auctionController = loader.getController();
            auctionController.setConnection(connection);
            auctionController.setCurrentUser(currentUser);
            auctionController.setMainLayoutController(this);
            auctionController.startMessageListener();
            
            contentArea.getChildren().setAll(dashboardView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadMyItems() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/my-items.fxml"));
            myItemsView = loader.load();
            myItemsController = loader.getController();
            myItemsController.setConnectionAndUser(connection, currentUser);
            
            contentArea.getChildren().setAll(myItemsView);
            myItemsController.fetchMyItems();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadAdminPanel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/admin-dashboard.fxml"));
            Parent adminView = loader.load();
            AdminDashboardController controller = loader.getController();
            controller.setConnection(connection);
            
            contentArea.getChildren().setAll(adminView);
            
            // Pass the controller back to AuctionController to listen for incoming messages
            if (auctionController != null) {
                auctionController.setAdminDashboardController(controller);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setActiveButton(Button activeBtn) {
        btnDashboard.getStyleClass().remove("sidebar-btn-active");
        btnMyItems.getStyleClass().remove("sidebar-btn-active");
        if (btnAdminPanel != null) btnAdminPanel.getStyleClass().remove("sidebar-btn-active");
        if (activeBtn != null) activeBtn.getStyleClass().add("sidebar-btn-active");
    }
}
