package com.client.chatwindow;

import java.io.*; 

import com.client.login.MainLauncher;
import com.client.login.LoginController;
import com.messages.Message;
import com.messages.Conversation;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;


public class ChatController implements Initializable {

    @FXML private TextArea messageBox;
    @FXML private Label usernameLabel;
    @FXML private ListView<Conversation> conversationListView;
    @FXML private ImageView userImageView;
    @FXML private ListView chatPane;
    @FXML GridPane gridPane;
    @FXML ComboBox statusComboBox;
    @FXML VBox propertyBox;

    private double xOffset;
    private double yOffset;
    
    private Status currentStatus = Status.OFFLINE;
    private int currentTargetConversationID = -1; 
    public Listener listener;

    private ChatController instance;
    private AddFriendController addFriendCon;
    private GroupInvitationController groupInvitationCon;
    private GroupAddController groupAddCon;
    
    Logger logger = LoggerFactory.getLogger(ChatController.class);

    public ChatController() {
        instance = this;
    }

    public ChatController getInstance() {
        return instance;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

	public void setUsernameLabel(String username) {
        this.usernameLabel.setText(username);
    }

    public int getCurrentTargetConversationID() {
        return this.currentTargetConversationID;
    }

    public void sendHandler() throws IOException {
        String msg = messageBox.getText();
        if (currentTargetConversationID == -1) {
            LoginController.showErrorDialog("You have not choosen any conversation");
            return;
        }
        if (!messageBox.getText().isEmpty()) {
            this.listener.sendMessageToConversation(currentTargetConversationID, msg);
            messageBox.clear();
        }
    }

    public void addFriendHandler() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/NewAddFriendView.fxml"));
            BorderPane window = fxmlLoader.load();
            addFriendCon = fxmlLoader.getController();
            addFriendCon.setListener(this.listener);
            listener.setAddFriendController(addFriendCon);
            listener.getFriendRequest();
            logger.info("set listener to add friend controller");

            Stage stage = (Stage) messageBox.getScene().getWindow();
            Scene scene = new Scene(window);
            stage.setScene(scene);

            // Set stage size to match scene size
            stage.setWidth(window.getPrefWidth());
            stage.setHeight(window.getPrefHeight());
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            // Consider logging the error or showing an alert to the user
        }
    }

//    public void groupAddHandler() {
//        try {
//            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/GroupAddView.fxml"));
//            BorderPane window = fxmlLoader.load();
//            groupAddCon = fxmlLoader.getController();
//            groupAddCon.setListener(this.listener);
//
//            Stage stage = (Stage) messageBox.getScene().getWindow();
//            Scene scene = new Scene(window);
//            stage.setScene(scene);
//
//            // Set stage size to match scene size
//            stage.setWidth(window.getPrefWidth());
//            stage.setHeight(window.getPrefHeight());
//            stage.centerOnScreen();
//        } catch (IOException e) {
//            e.printStackTrace();
//            // Consider logging the error or showing an alert to the user
//        }
//    }
    
    public void groupHandler() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/GroupView.fxml"));
            BorderPane window = fxmlLoader.load();
            groupInvitationCon = fxmlLoader.getController();
            groupInvitationCon.setListener(this.listener);
            listener.setGroupInvitationCon(groupInvitationCon);
            listener.getGroupRequest("GroupInvitationController");

            Stage stage = (Stage) messageBox.getScene().getWindow();
            Scene scene = new Scene(window);
            stage.setScene(scene);

            // Set stage size to match scene size
            stage.setWidth(window.getPrefWidth());
            stage.setHeight(window.getPrefHeight());
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            // Consider logging the error or showing an alert to the user
        }
    }

    public void addGroupMemberHandler() {
    	try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/GroupAddView.fxml"));
            BorderPane window = fxmlLoader.load();
            groupAddCon = fxmlLoader.getController();
            groupAddCon.setListener(this.listener);
            groupAddCon.setGroupNow(currentTargetConversationID);
            listener.setGroupAddCon(groupAddCon);
            listener.getGroupRequest("GroupAddController");
            logger.info("set listener to add member to group controller");

            Stage stage = (Stage) messageBox.getScene().getWindow();
            Scene scene = new Scene(window);
            stage.setScene(scene);

            // Set stage size to match scene size
            stage.setWidth(window.getPrefWidth());
            stage.setHeight(window.getPrefHeight());
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            // Consider logging the error or showing an alert to the user
        }
    }

    public void outGroupHandler() {

        // Prompt user for confirmation
        if (!LoginController.showConfirmationDialog("Do you want to exit this group ?")) {
            return;
        }

        try {
            if (currentTargetConversationID == -1) {
                LoginController.showErrorDialog("You are not choosing any conversation");
                return;
            }
            this.listener.outGroup(currentTargetConversationID);
        } catch (IOException e) {
            e.printStackTrace();
            // Consider logging the error or showing an alert to the user
        }
    }

    public void refreshHandler() throws IOException {
        currentTargetConversationID = -1;
        Platform.runLater(() -> {
            try {
                conversationListView.getItems().clear();
                chatPane.getItems().clear();
                propertyBox.getChildren().clear();
                this.listener.sendUpdateConversationRequest();
            } catch (Exception e) {
                logger.error("Error updating user list", e);
            }
        });
    }

    // When a new message add to chat
    public synchronized void addMessageToChatView(Message msg) {

        logger.info("addMessageToChatView() method ENTER");

        // Task to handle messages from other users
        Task<HBox> othersMessages = new Task<HBox>() {
            @Override
            public HBox call() throws Exception {
                // Load the profile image of the sender
                Image image = userImageView.getImage();
                ImageView profileImage = new ImageView(image);
                profileImage.setFitHeight(32);
                profileImage.setFitWidth(32);

                // Create a BubbledLabel for the message text
                BubbledLabel bl6 = new BubbledLabel();
                bl6.setText(msg.getName() + ": " + msg.getMsg());
                bl6.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));

                // Create an HBox to contain the profile image and message bubble
                HBox x = new HBox();
                bl6.setBubbleSpec(BubbleSpec.FACE_LEFT_CENTER);
                x.getChildren().addAll(profileImage, bl6);

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

                return x;
            }
        };

        // Set onSucceeded event handler for othersMessages task
        othersMessages.setOnSucceeded(event -> {
            logger.info("Adding message from another user");
            HBox messageBox = othersMessages.getValue();
            Platform.runLater(() -> chatPane.getItems().add(messageBox));
        });

        // Set onSucceeded event handler for yourMessages task
        yourMessages.setOnSucceeded(event -> {
            logger.info("Adding message from self");
            HBox messageBox = yourMessages.getValue();
            Platform.runLater(() -> chatPane.getItems().add(messageBox));
        });

        // Set onFailed event handler for both tasks to log errors
        othersMessages.setOnFailed(event -> logger.error("Failed to load other user's message", othersMessages.getException()));
        yourMessages.setOnFailed(event -> logger.error("Failed to load user's message", yourMessages.getException()));

        // Determine if the message is from the user or another user and start the corresponding task
        if (msg.getName().equals(usernameLabel.getText())) {
            Thread t2 = new Thread(yourMessages);
            t2.setDaemon(true);
            t2.start();
        } else {
            Thread t = new Thread(othersMessages);
            t.setDaemon(true);
            t.start();
        }
    }
    
    public void setConversationListView(Message msg) {
        logger.info("setConversationListView() from ChatController method Enter");
        HashMap<Integer, Conversation> conversationMap = msg.getConversationMap();
        logger.info("Size of conversationMap: " + conversationMap.size());

        Platform.runLater(() -> {
            try {
                // Update user list view
                ObservableList<Conversation> conversationList = FXCollections.observableList(new ArrayList<>(conversationMap.values()));
                conversationListView.setItems(conversationList);
                conversationListView.setCellFactory(new CellRenderer());
            
        } catch (Exception e) {
                logger.error("Error updating user list", e);
        }
    });
        logger.info("setConversationListView() method Exit");
    }


	// // sendButtonAction
    // public void sendMethod(KeyEvent event) throws IOException {
    //     if (event.getCode() == KeyCode.ENTER) {
    //         sendHandler();
    //     }
    // }

    @FXML
    public void closeApplication() {
        Platform.exit();
        System.exit(0);
    }

    public void showConversationProperty(Message msg) {
        logger.info("showConversationProperty() method enter");

        Image image = userImageView.getImage();
        ImageView profileImage = new ImageView(image);
        profileImage.setFitHeight(64);
        profileImage.setFitWidth(64);

        Platform.runLater(() -> {
            Label conversationName = new Label(msg.getName());

            propertyBox.getChildren().clear();
            propertyBox.getChildren().add(profileImage);
            propertyBox.getChildren().add(conversationName);

            // if this conversation is a group, display the member of it
            if (msg.getUserList() != null) {
                logger.info("get group conversation");
                // Create a ListView to store the user list
                ListView<String> userListView = new ListView<>();
                // Convert ArrayList<User> to ObservableList<String>
                ObservableList<String> userNames = FXCollections.observableArrayList();
                for (User user : msg.getUserList()) {
                    userNames.add(user.getName());
                }
                userListView.setItems(userNames);

                // Create a ScrollPane and add the ListView to it
                ScrollPane scrollPane = new ScrollPane(userListView);
                scrollPane.setFitToWidth(true); // Ensures ListView width fits ScrollPane

                propertyBox.getChildren().add(new Label());
                propertyBox.getChildren().add(new Label());
                
                VBox.setVgrow(scrollPane, Priority.ALWAYS);

                Button addMemberButton = new Button("Add Member");
                addMemberButton.setMaxWidth(Double.MAX_VALUE);
                addMemberButton.setOnAction(event -> addGroupMemberHandler());
                
                Button outGroupButton = new Button("Out Group");
                outGroupButton.setMaxWidth(Double.MAX_VALUE);
                outGroupButton.setOnAction(event -> outGroupHandler());
                
                propertyBox.getChildren().add(addMemberButton);
                propertyBox.getChildren().add(new Label("Group Member"));
                propertyBox.getChildren().add(scrollPane);
                propertyBox.getChildren().add(outGroupButton);
            }

            // Center the children within the VBox
            propertyBox.setAlignment(Pos.TOP_CENTER);
            // Optionally, add spacing between the children
            propertyBox.setSpacing(10);
        });
    }
    
    public void showContextOfConversation(Message msg) {
        Platform.runLater(() -> {
            // clear the chat listview
            chatPane.getItems().clear();

            // add message to it
            if (msg.getContext() != null) {
                for (Message mes : msg.getContext()) {
                    addMessageToChatView(mes);
                }
            }
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    
        /* Drag and Drop */
        gridPane.setOnMousePressed(event -> {
            xOffset = MainLauncher.getPrimaryStage().getX() - event.getScreenX();
            yOffset = MainLauncher.getPrimaryStage().getY() - event.getScreenY();
            gridPane.setCursor(Cursor.CLOSED_HAND);
        });

        gridPane.setOnMouseDragged(event -> {
            MainLauncher.getPrimaryStage().setX(event.getScreenX() + xOffset);
            MainLauncher.getPrimaryStage().setY(event.getScreenY() + yOffset);

        });

        gridPane.setOnMouseReleased(event -> {
            gridPane.setCursor(Cursor.DEFAULT);
        });

        /* Added to prevent the enter from adding a new line to inputMessageBox */
        messageBox.addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
            if (ke.getCode().equals(KeyCode.ENTER)) {
                try {
                    sendHandler();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ke.consume();
            }
        });
        
        // Add to track conversationListView
        conversationListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Conversation>() {
            @Override
            public void changed(ObservableValue<? extends Conversation> observable, Conversation oldRequest, Conversation newRequest) {
                if (newRequest != null) {
                    currentTargetConversationID = newRequest.getConversationID();
                    //this.listener.getMessageFromConversation(currentTargetConversationID);
                    logger.info("ListView selection changed to newValue = " + currentTargetConversationID);
                    try {
                        getConversationProperty(currentTargetConversationID);
                        getMessageFromConversation(currentTargetConversationID);
                    } catch (IOException e) {
                        logger.error("Error getting message from conversation", e);
                        // Handle the error, e.g., show an alert to the user
                    }
                } else {
                    currentTargetConversationID = -1;
                    logger.info("ListView selection cleared.");
                }           
            }
        });

    }

    public void updateStatus(Status status) throws IOException {
        this.listener.sendStatusUpdate(status);
    }

    public void getConversationProperty(int targetConversationID) throws IOException {
        this.listener.getConversationProperty(targetConversationID);
    }

    public void getMessageFromConversation(int targetConversationID) throws IOException {
        this.listener.getMessageFromConversation(targetConversationID);
    }

}
