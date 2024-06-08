package com.client.chatwindow;

import com.client.login.MainLauncher;

import com.messages.User;

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
        requestListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<User>() {
            public void changed(ObservableValue<? extends User> observable, User oldUser, User newUser) {
                logger.info("ListView selection changed to newValue = " + newUser.getName());
                currentTargetName = newUser.getName();
            }
        });
    }
    
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setUserListView(ArrayList<User> userArrayList) {
        logger.info("setUserListView() method Enter with");
        
        Platform.runLater(() -> {
            try {
            
                // Update user list view
                ObservableList<User> usersList = FXCollections.observableList(userArrayList);
                requestListView.setItems(usersList);
                requestListView.setCellFactory(new CellRenderer());
                        
                logger.info("User list updated successfully");
        } catch (Exception e) {
                logger.error("Error updating user list", e);
        }
    });
        logger.info("setUserListView() method Exit");
    }

    public void acceptHandler(ActionEvent event) {

    }

    public void declineHandler(ActionEvent event) {

    }

    /*
    ** Return button handler -> return to ChatView
    */
    public void returnHandler(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/ChatView.fxml"));
            BorderPane window = fxmlLoader.load();
            chatCon = fxmlLoader.getController();
            chatCon.setListener(listener);

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
