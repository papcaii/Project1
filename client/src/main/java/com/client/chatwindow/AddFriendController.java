package com.client.register;

import com.client.chatwindow.Listener;
import com.client.login.MainLauncher;
import com.client.chatwindow.ChatController;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AddFriendController implements Initializable {
    @FXML private TextField nameTextField;
    
    private static ChatController chatCon;
    private static Listener listener;
    private static AddFriendController instance;

    public AddFriendController() {
        instance = this;
    }

    public static AddFriendController getInstance() {
        return instance;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize any required data or components here
    }
    
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /*
    ** Confirm button handler -> add friend
    */
    public void confirmHandler() {
        try {
            String name = nameTextField.getText();
            if (name == null || name.isEmpty()) {
                showAlert("Error", "Name cannot be empty");
                return;
            }

            if (listener != null) {
                listener.addFriend(name);
            } else {
                showAlert("Error", "Listener is not initialized");
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to add friend: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            showAlert("Error", "Not found class: " + e.getMessage());
        }
    }

    /*
    ** Return button handler -> return to ChatView
    */
    public void returnHandler() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/ChatView.fxml"));
            Parent window = fxmlLoader.load();
            chatCon = fxmlLoader.getController();
            chatCon.setListener(listener);

            Stage stage = MainLauncher.getPrimaryStage();
            Scene scene = new Scene(window);
            stage.setScene(scene);
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
