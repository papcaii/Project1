package com.client.login;

import com.client.chatwindow.ChatController;
import com.client.chatwindow.Listener;
import com.client.util.ResizeHelper;
import com.client.register.RegisterController;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.Random;
import java.util.Optional;
import java.util.ResourceBundle;

public class LoginController implements Initializable {
    @FXML private ImageView Defaultview;
    @FXML public  TextField hostnameTextfield;
    @FXML private TextField portTextfield;
    @FXML private TextField usernameTextfield;
    @FXML private PasswordField passwordTextfield;
    //@FXML private ChoiceBox imagePicker;
    
    public static ChatController chatCon;
    public static RegisterController registerCon;
    @FXML private BorderPane borderPane;
    private double xOffset;
    private double yOffset;
    private Scene scene;
    public Listener listener;

    private static LoginController instance;

    // Default constructor for FXMLLoader
    public LoginController() {
        instance = this;
    }

    public static LoginController getInstance() {
        return instance;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setHostnameTextfield(String hostname) {
        this.hostnameTextfield.setText(hostname);
    }

    public void setPortTextfield(String port) {
        this.portTextfield.setText(port);
    }

    public void setupListener() {
        String hostname = hostnameTextfield.getText();
        int port;
        
        // Validate and parse port number
        try {
            port = Integer.parseInt(portTextfield.getText());
        } catch (NumberFormatException e) {
            showErrorDialog("Invalid port number. Please enter a valid integer.");
            return;
        }

        String username = usernameTextfield.getText();
        String picture = "images/default.png";
        String password = passwordTextfield.getText();

        // Check if any of the required fields are empty
        if (hostname.isEmpty()) {
            showErrorDialog("Please fill in all required fields.");
            return;
        }

        this.listener = new Listener(hostname, port);
        Thread x = new Thread(listener);
        x.start();

        // Wait until the socket initialization is complete and the listener is ready
        long startTime = System.currentTimeMillis();
        long timeout = 3000; // 10 seconds timeout for connection

        while (!listener.isReady()) {
            if (System.currentTimeMillis() - startTime > timeout) {
                showErrorDialog("Connection timed out. Please check your network settings and try again.");
                listener.close(); // Ensure the listener thread is stopped
                this.listener = null;
                return;
            }

            try {
                Thread.sleep(100); // Adjust this value as needed
            } catch (InterruptedException e) {
                e.printStackTrace();
                showErrorDialog("An error occurred while attempting to connect.");
                listener.close(); // Ensure the listener thread is stopped
                this.listener = null;
                return;
            }
        }
    }
    
    /* 
    ** Login button handler
    */
    public void loginButtonHandler() throws IOException, ClassNotFoundException {

        String username = usernameTextfield.getText();
        String picture = "images/default.png";
        String password = passwordTextfield.getText();

        // Check if any of the required fields are empty
        if (username.isEmpty() || password.isEmpty()) {
            showErrorDialog("Please fill in all required fields.");
            return;
        }

        if (this.listener == null) {
            setupListener();
        }
        this.listener.login(username, password);
    }
    
    /* 
    ** Register button handler
    */
    public void registerButtonHandler() {
    	try {

            if (this.listener == null) {
                setupListener();
            }

        	FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/RegisterView.fxml"));
        	Parent window = fxmlLoader.load();
        	registerCon = fxmlLoader.getController();
            registerCon.setListener(this.listener);
            registerCon.setHostnameTextfield(this.hostnameTextfield.getText());
            registerCon.setPortTextfield(this.portTextfield.getText());

        	Stage stage = MainLauncher.getPrimaryStage();
        	Scene scene = new Scene(window);
        	stage.setScene(scene);
        	stage.centerOnScreen();

    	} catch (IOException e) {
        	e.printStackTrace();
        	// Consider logging the error or showing an alert to the user
    	}
	}
    
    // Minimize handler

    public void minimizeWindow(){
        MainLauncher.getPrimaryStage().setIconified(true);
    }

	// Close handler
    public void closeSystem(){
        Platform.exit();
        System.exit(0);
    }

    // login success, change to chat scene controller
    public void showChatScene() {
        Platform.runLater(() -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/ChatView.fxml"));
                Parent window = fxmlLoader.load();
                this.scene = new Scene(window);
                chatCon = fxmlLoader.<ChatController>getController();
    
                Stage stage = (Stage) hostnameTextfield.getScene().getWindow();
                stage.setResizable(true);
                stage.setWidth(1040);
                stage.setHeight(620);
    
                stage.setOnCloseRequest((WindowEvent e) -> {
                    Platform.exit();
                    System.exit(0);
                });
                stage.setScene(this.scene);
                stage.setMinWidth(800);
                stage.setMinHeight(300);
                ResizeHelper.addResizeListener(stage);
                stage.centerOnScreen();

                chatCon.setListener(this.listener);
                chatCon.setUsernameLabel(usernameTextfield.getText());
                this.listener.setChatController(chatCon);

                //con.setImageLabel(selectedPicture.getText());
            } catch (IOException e) {
                e.printStackTrace();
                // You might want to show an alert or log this exception appropriately
            }
        });
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        /* Drag and Drop */
        borderPane.setOnMousePressed(event -> {
            xOffset = MainLauncher.getPrimaryStage().getX() - event.getScreenX();
            yOffset = MainLauncher.getPrimaryStage().getY() - event.getScreenY();
            borderPane.setCursor(Cursor.CLOSED_HAND);
        });

        borderPane.setOnMouseDragged(event -> {
            MainLauncher.getPrimaryStage().setX(event.getScreenX() + xOffset);
            MainLauncher.getPrimaryStage().setY(event.getScreenY() + yOffset);

        });

        borderPane.setOnMouseReleased(event -> {
            borderPane.setCursor(Cursor.DEFAULT);
        });

    }

    /* This displays an alert message to the user */
    public static void showErrorDialog(String message) {
        Platform.runLater(()-> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning!");
            alert.setHeaderText(message);
            alert.setContentText("Please check for server issues and check if the server is running.");
            alert.showAndWait();
        });

    }

    public static boolean showConfirmationDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText(message);
    
        ButtonType buttonYes = new ButtonType("Yes");
        ButtonType buttonNo = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);
    
        alert.getButtonTypes().setAll(buttonYes, buttonNo);
    
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == buttonYes;
    }
}
