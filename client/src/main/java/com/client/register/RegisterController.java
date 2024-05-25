package com.client.register;

import com.client.login.MainLauncher;
import com.client.chatwindow.ChatController;
import com.client.login.LoginController;
import com.client.util.ResizeHelper;

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
import java.util.ResourceBundle;

public class RegisterController implements Initializable {
    @FXML private ImageView Defaultview;
    @FXML private TextField usernameTextfield;
    @FXML private PasswordField passwordTextfield;
    @FXML private PasswordField confirmPasswordTextfield;
    //@FXML private ChoiceBox imagePicker;
    
    public static LoginController loginCon;
    
    @FXML private BorderPane borderPane;
    private double xOffset;
    private double yOffset;
    private Scene scene;

    private static RegisterController instance;

    public RegisterController() {
        instance = this;
    }

    public static RegisterController getInstance() {
        return instance;
    }
    
    /* 
    ** Confirm button handler -> return to Login page
    */
    public void confirmHandler() throws IOException {
        try {
        	FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/LoginView.fxml"));
        	Parent window = fxmlLoader.load();
        	loginCon = fxmlLoader.getController();

        	Stage stage = MainLauncher.getPrimaryStage();
        	Scene scene = new Scene(window);
        	stage.setScene(scene);
        	stage.centerOnScreen();
    	} catch (IOException e) {
        	e.printStackTrace();
        	// Consider logging the error or showing an alert to the user
    	}
    }
    
    /* 
    ** Minimize handler
    */
    public void minimizeWindow(){
        MainLauncher.getPrimaryStage().setIconified(true);
    }

	/*
	** Close handler
	*/
    public void closeSystem(){
        Platform.exit();
        System.exit(0);
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
    
}