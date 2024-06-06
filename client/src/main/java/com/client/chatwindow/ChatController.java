package com.client.chatwindow;

import java.io.*; 

import com.client.login.MainLauncher;
import com.client.login.LoginController;
import com.messages.Message;
import com.messages.MessageType;
import com.messages.User;
import com.messages.Status;
import com.messages.User;
import com.messages.bubble.BubbleSpec;
import com.messages.bubble.BubbledLabel;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;


public class ChatController implements Initializable {

    @FXML private TextArea messageBox;
    @FXML private Label usernameLabel;
    @FXML private Label onlineCountLabel;
    @FXML private ListView userListView;
    @FXML private ImageView userImageView;
    @FXML private ListView chatPane;
    @FXML ListView statusList;
    @FXML BorderPane borderPane;
    @FXML ComboBox statusComboBox;

    private double xOffset;
    private double yOffset;
    
    public String currentTargetName = ""; 
    public Listener listener;

    private static ChatController instance;
    
    Logger logger = LoggerFactory.getLogger(ChatController.class);

    public ChatController() {
        instance = this;
    }

    public static ChatController getInstance() {
        return instance;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

	public void setUsernameLabel(String username) {
        this.usernameLabel.setText(username);
    }

    public void sendButtonAction() throws IOException {
        String msg = messageBox.getText();
        if (!messageBox.getText().isEmpty()) {
            this.listener.send(msg);
            messageBox.clear();
        }
    }


	// When a new message add to chat
    public synchronized void addToChat(Message msg) {
    
    	logger.info(msg.getPicture());
    
    	// Task to handle messages from other users
        Task<HBox> othersMessages = new Task<HBox>() {
            @Override
            public HBox call() throws Exception {
            
            	// Load the profile image of the sender
                Image image = new Image(getClass().getClassLoader().getResource(msg.getPicture()).toString());
                ImageView profileImage = new ImageView(image);
                profileImage.setFitHeight(32);
                profileImage.setFitWidth(32);
                
                // Create a BubbledLabel for the message text
                BubbledLabel bl6 = new BubbledLabel();
                bl6.setText(msg.getName() + ": " + msg.getMsg());
                bl6.setBackground(new Background(new BackgroundFill(Color.WHITE,null, null)));
                
                // Create an HBox to contain the profile image and message bubble
                HBox x = new HBox();
                bl6.setBubbleSpec(BubbleSpec.FACE_LEFT_CENTER);
                x.getChildren().addAll(profileImage, bl6);
                logger.debug("ONLINE USERS: " + Integer.toString(msg.getUserList().size()));
                
                // Update the online user count
                int onlineCount = msg.getUserList().size();  // Assuming msg.getUsers() returns the list of users
            	setOnlineLabel(String.valueOf(onlineCount));
                logger.info("Execute other message");
                return x;
            }
        };

		// Task to handle messages from the user
        Task<HBox> yourMessages = new Task<HBox>() {
            @Override
            public HBox call() throws Exception {
                Image image = userImageView.getImage();
                ImageView profileImage = new ImageView(image);
                profileImage.setFitHeight(32);
                profileImage.setFitWidth(32);

                BubbledLabel bl6 = new BubbledLabel();
                bl6.setText(msg.getMsg());
                bl6.setBackground(new Background(new BackgroundFill(Color.LIGHTGREEN, null, null)));
                
                HBox x = new HBox();
                x.setMaxWidth(chatPane.getWidth() - 20);
                x.setAlignment(Pos.TOP_RIGHT);
                bl6.setBubbleSpec(BubbleSpec.FACE_RIGHT_CENTER);
                x.getChildren().addAll(bl6, profileImage);

                setOnlineLabel(Integer.toString(msg.getUserList().size()));
                return x;
            }
        };
        
        // If task succeeded, add box to chat pane
        othersMessages.setOnSucceeded(event -> {
    		HBox messageBox = othersMessages.getValue();
    		Platform.runLater(() -> {
        		chatPane.getItems().add(messageBox);
    		});
		});
        
        yourMessages.setOnSucceeded(event -> {
    		HBox messageBox = yourMessages.getValue();
    		Platform.runLater(() -> {
        		chatPane.getItems().add(messageBox);
    		});
		});

		// Determine if the message is from the user or another user and start the corresponding task
        if (msg.getName().equals(usernameLabel.getText())) {
            Thread t2 = new Thread(yourMessages);
            t2.setDaemon(true);
            t2.start();
        } else {
        	logger.info("Execute other message");
            Thread t = new Thread(othersMessages);
            t.setDaemon(true);
            t.start();
        }
    }
    

	// Change number of online user
    public void setOnlineLabel(String usercount) {
        Platform.runLater(() -> onlineCountLabel.setText(usercount));
    }

    public void setUserListView(Message msg) {
        logger.info("setUserListView() method Enter with");
        
    	Platform.runLater(() -> {
        	try {
        	
        		// Update user list view
            	ObservableList<User> usersList = FXCollections.observableList(msg.getUserList());
            	userListView.setItems(usersList);
            	userListView.setCellFactory(new CellRenderer());
            
            	// Update online number
            	int onlineCount = msg.getUserList().size();  // Assuming msg.getUsers() returns the list of users
            	setOnlineLabel(String.valueOf(onlineCount));
            
            	logger.info("User list updated successfully with " + onlineCount + " users.");
        } catch (Exception e) {
            	logger.error("Error updating user list", e);
        }
    });
        logger.info("setUserListView() method Exit");
    }


	// sendButtonAction
    public void sendMethod(KeyEvent event) throws IOException {
        if (event.getCode() == KeyCode.ENTER) {
            sendButtonAction();
        }
    }

    @FXML
    public void closeApplication() {
        Platform.exit();
        System.exit(0);
    }

    /* Method to display server messages */
    
    public synchronized void addAsServer(Message msg) {
        Task<HBox> task = new Task<HBox>() {
            @Override
            public HBox call() throws Exception {
                BubbledLabel bl6 = new BubbledLabel();
                bl6.setText(msg.getMsg());
                bl6.setBackground(new Background(new BackgroundFill(Color.ALICEBLUE,
                        null, null)));
                HBox x = new HBox();
                bl6.setBubbleSpec(BubbleSpec.FACE_BOTTOM);
                x.setAlignment(Pos.CENTER);
                x.getChildren().addAll(bl6);
                return x;
            }
        };
        task.setOnSucceeded(event -> {
            chatPane.getItems().add(task.getValue());
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
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

		
		/* track status changed
        statusComboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                try {
                    Listener.sendStatusUpdate(Status.valueOf(newValue.toUpperCase()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        */

        /* Added to prevent the enter from adding a new line to inputMessageBox */
        messageBox.addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
            if (ke.getCode().equals(KeyCode.ENTER)) {
                try {
                    sendButtonAction();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ke.consume();
            }
        });
        
        // Add to track userListView
        userListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<User>() {
    		public void changed(ObservableValue<? extends User> observable, User oldUser, User newUser) {
        		logger.info("ListView selection changed to newValue = " + newUser.getName());
        		currentTargetName = newUser.getName();
    		}
		});

    }
}
