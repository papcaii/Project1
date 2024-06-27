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

public class GroupAddMemberController implements Initializable {
    @FXML private TextField nameTextField;
    
    private static ChatController chatCon;
    private static Listener listener;

    private Conversation currentGroup;

    Logger logger = LoggerFactory.getLogger(GroupAddMemberController.class);

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setCurrentGroup(Conversation currentGroup) {
        this.currentGroup = currentGroup;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize any required data or components here
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
                listener.sendGroupRequest(userName,currentGroup);
            } else {
                LoginController.showErrorDialog("Listener is not initialized");
            }

        } catch (IOException e) {
            e.printStackTrace();
            LoginController.showErrorDialog("Failed to add friend: " + e.getMessage());
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

            Stage stage = (Stage) nameTextField.getScene().getWindow();
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
