package com.client.chatwindow;

import com.client.Listener;
import com.client.login.MainLauncher;
import com.client.login.LoginController;
import com.model.User;
import com.model.Conversation;
import com.client.util.ResizeHelper;
import com.client.util.CellRenderer;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.layout.BorderPane;

import javafx.scene.control.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.net.URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FriendRequestController implements Initializable {
    @FXML private ListView requestListView;
    
    private static ChatController chatCon;
    private static Listener listener;
    private static FriendRequestController instance;

    private String currentTargetName;

    Logger logger = LoggerFactory.getLogger(FriendRequestController.class);

    public FriendRequestController() {
        instance = this;
    }

    public static FriendRequestController getInstance() {
        return instance;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize any required data or components here
        // Add to track userListView
        requestListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Conversation>() {
            @Override
            public void changed(ObservableValue<? extends Conversation> observable, Conversation oldRequest, Conversation newRequest) {
                if (newRequest != null) {
                    currentTargetName = newRequest.getConversationName();
                    logger.info("ListView selection changed to newValue = " + currentTargetName);
                } else {
                    currentTargetName = null;
                    logger.info("ListView selection cleared.");
                }           
            }
        });
    }
    
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setUserListView(ArrayList<Conversation> userConversationList) {
        logger.info("setUserListView() method Enter");
        
        Platform.runLater(() -> {
            try {
                // Update user list view
                ObservableList<Conversation> conversationList = FXCollections.observableList(userConversationList);
                requestListView.setItems(conversationList);
                requestListView.setCellFactory(new CellRenderer());
            
        } catch (Exception e) {
                logger.error("Error updating user list", e);
        }
    });
        logger.info("setConversationListView() method Exit");
    }

    public void acceptHandler(ActionEvent event) throws IOException, ClassNotFoundException {
        if (this.currentTargetName == null) {
            showAlert("Error", "Must choose request to accept");
            return;
        }

        // Prompt user for confirmation
        if (!LoginController.showConfirmationDialog("Do you want to accept friend request from user " + this.currentTargetName + "?")) {
            return;
        }

        if (listener != null) {
            listener.createFriendShip(currentTargetName);
        } else {
            showAlert("Error", "Listener is not initialized");
        }

    }

    public void declineHandler(ActionEvent event) throws IOException, ClassNotFoundException{

        if (this.currentTargetName == null) {
            showAlert("Error", "Must choose request to decline");
            return;
        }

        // Prompt user for confirmation
        if (!LoginController.showConfirmationDialog("Do you want to decline friend request from user " + this.currentTargetName + "?")) {
            return;
        }

        if (listener != null) {
            listener.declineFriendRequest(currentTargetName);
        } else {
            showAlert("Error", "Listener is not initialized");
        }

    }

    /*
    ** Return button handler -> return to ChatView
    */
    public void returnHandler(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/ChatView.fxml"));
            BorderPane window = fxmlLoader.load();
            chatCon = fxmlLoader.getController();
            chatCon.setListener(this.listener);
            chatCon.setUsernameLabel(this.listener.username);
            this.listener.setChatController(chatCon);

            Stage stage = MainLauncher.getPrimaryStage();
            Scene scene = new Scene(window);
            stage.setScene(scene);

            // Set stage size to match scene size
            stage.setWidth(window.getPrefWidth());
            stage.setHeight(window.getPrefHeight());
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load chat view: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
