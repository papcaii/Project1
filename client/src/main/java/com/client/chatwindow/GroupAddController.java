package com.client.chatwindow;

import com.client.chatwindow.Listener;
import com.client.login.MainLauncher;
import com.client.login.LoginController;
import com.client.chatwindow.ChatController;
import com.messages.User;
import com.messages.Conversation;
import com.client.util.ResizeHelper;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.control.*;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import javafx.event.ActionEvent;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupAddController implements Initializable {
    @FXML private TextField nameTextField;
    @FXML private ListView requestListView;
    
    private static ChatController chatCon;
    private static Listener listener;
    private static GroupAddController instance;

    private String currentTargetName;
    
    private Conversation groupNow;

    Logger logger = LoggerFactory.getLogger(FriendRequestController.class);

    public GroupAddController() {
        instance = this;
    }

    public static GroupAddController getInstance() {
        return instance;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
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

    public void setUserListView(ArrayList<Conversation> groupConversationList) {
        logger.info("setUserListView() method Enter");
        
        Platform.runLater(() -> {
            try {
                // Update user list view
                ObservableList<Conversation> conversationList = FXCollections.observableList(groupConversationList);
                requestListView.setItems(conversationList);
                requestListView.setCellFactory(new CellRenderer());
            
        } catch (Exception e) {
                logger.error("Error updating user list", e);
        }
    });
        logger.info("setConversationListView() method Exit");
    }
    
    /*
    ** Confirm button handler -> send request
    */
    public void confirmHandler() {
        try {
            String userName = nameTextField.getText();
            if (userName == null || userName.isEmpty()) {
                LoginController.showErrorDialog("Name cannot be empty");
                return;
            }

            // listener send add friend action to server
            if (listener != null) {
                listener.sendGroupRequest(userName,groupNow);
            } else {
                LoginController.showErrorDialog("Listener is not initialized");
            }

        } catch (IOException e) {
            e.printStackTrace();
            LoginController.showErrorDialog("Failed to add friend: " + e.getMessage());
        } 
    }

    public void acceptHandler(ActionEvent event) throws IOException, ClassNotFoundException {
        if (this.currentTargetName == null) {
            LoginController.showErrorDialog("Must choose request to accept");
            return;
        }

        // Prompt user for confirmation
        if (!LoginController.showConfirmationDialog("Do you want to accept friend request from user " + this.currentTargetName + "?")) {
            return;
        }

        if (listener != null) {
            listener.createFriendShip(currentTargetName);
        } else {
            LoginController.showErrorDialog("Listener is not initialized");
        }

    }

    public void declineHandler(ActionEvent event) throws IOException, ClassNotFoundException{

        if (this.currentTargetName == null) {
            LoginController.showErrorDialog("Must choose request to decline");
            return;
        }

        // Prompt user for confirmation
        if (!LoginController.showConfirmationDialog("Do you want to decline friend request from user " + this.currentTargetName + "?")) {
            return;
        }

        if (listener != null) {
            listener.declineFriendRequest(currentTargetName);
        } else {
            LoginController.showErrorDialog("Listener is not initialized");
        }

    }

    /*
    ** return to ChatView
    */
    public void returnHandler() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/NewChatView.fxml"));
            GridPane window = fxmlLoader.load();
            chatCon = fxmlLoader.getController();
            chatCon.setListener(listener);
            chatCon.setUsernameLabel(this.listener.username);
            this.listener.setChatController(chatCon);

            Stage stage = (Stage) requestListView.getScene().getWindow();
            stage.setResizable(true);
            
            Scene scene = new Scene(window);
            stage.setScene(scene);
            stage.setWidth(window.getPrefWidth());
            stage.setHeight(window.getPrefHeight());

            ResizeHelper.addResizeListener(stage);
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
            LoginController.showErrorDialog("Failed to load chat view: " + e.getMessage());
        }
    }
}
