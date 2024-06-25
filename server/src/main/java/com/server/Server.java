package com.server;

import com.database.DatabaseManager;
import com.exception.InvalidUserException;
import com.messages.Message;
import com.messages.Conversation;
import com.messages.MessageType;
import com.messages.Status;
import com.messages.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mindrot.jbcrypt.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Server {

    private static final int PORT = 9001;
    private static final HashMap<String, User> names = new HashMap<>();
    private static final HashMap<Integer, User> userMap = new HashMap<>();
    private static final HashMap<Integer, User> onlineUserMap = new HashMap<>();
    private static HashMap<Integer, ObjectOutputStream> writers = new HashMap<>();   // List of outputs to user
    private static ArrayList<User> users = new ArrayList<>();               // List of user instance
    public static Logger logger = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) throws Exception {
        logger.info("Starting the chat server...");

        // Attempt to establish a connection to the database
        if (testDatabaseConnection()) {
            logger.info("Database connection established successfully.");
        } else {
            logger.error("Failed to connect to the database. Exiting...");
            return;
        }

        // Start the server only if the database connection is successful
        try (ServerSocket listener = new ServerSocket(PORT)) {
            logger.info("The chat server is running.");
            while (true) {
                new ClientHandler(listener.accept()).start();
            }
        } catch (Exception e) {
            logger.error("An error occurred while running the server: " + e.getMessage(), e);
        }
    }

    private static boolean testDatabaseConnection() {
        try (Connection connection = DatabaseManager.getConnection()) {
            logger.info("getConnection() exit");
            if (connection != null) {
                logger.info("Successfully connected to the database!");
            } else {
                logger.info("Cannot connect to database!");
                return false;
            }
            
            // Get user list and store in a map
            try (PreparedStatement st = connection.prepareStatement(
                "SELECT * FROM User")) {
    
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {
                        // Retrieve user information from the ResultSet
                        String username = rs.getString("user_name");
                        int userID = rs.getInt("user_id");

                        // Create a new User object
                        User user = new User();
                        user.setName(username);
                        user.setID(userID);
                        user.setStatus(Status.OFFLINE);

                        // Store the user object in the HashMap with username as the key
                        userMap.put(userID, user);
                        names.put(username, user);
                        logger.info("User added: {} with ID: {}", username, userID);
                    }
                }
            }

        } catch (SQLException e) {
            logger.error("SQL Exception: " + e.getMessage(), e);
        }
        return true;
    }

    private static class ClientHandler extends Thread {
        private String name;
        private Socket socket;
        private Logger logger = LoggerFactory.getLogger(ClientHandler.class);
        private User user;
        
        private InputStream is;
        private OutputStream os;
        private ObjectInputStream input;
        private ObjectOutputStream output;

        private boolean isValid = false;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
        }

        public void run() {
            logger.info("Attempting to connect a user...");
            try {
                this.is = socket.getInputStream();
                this.input = new ObjectInputStream(is);

                this.os = socket.getOutputStream();
                this.output = new ObjectOutputStream(os);

                this.output.flush();
                if (this.input != null) {
                    logger.info("Input stream ready");
                } else {
                    logger.error("Failed to initialize input stream");
                    return;
                }

                while (this.socket.isConnected()) {
                    Message inputmsg = (Message) this.input.readObject();
                    if (inputmsg != null) {
                        logger.info(inputmsg.getType() + " - " + inputmsg.getName() + ": " + inputmsg.getMsg());
                        switch (inputmsg.getType()) {
                            case C_LOGIN:
                                if (validateClient(inputmsg)) {
                                    this.name = inputmsg.getName();
                                    this.user = names.get(inputmsg.getName());
                                    writers.put(user.getID(), this.output);
                                    onlineUserMap.put(user.getID(), user);
                                    logger.info(user.getName()+" login now");
                                    updateClientStatus(user.getID(), Status.ONLINE);
                                    getUserConversation(user.getID());
                                }
                                break;
                            case C_REGISTER:
                                registerClient(inputmsg);
                                break;

                            case C_FRIEND_REQUEST:
                                sendFriendRequest(inputmsg);
                                break;

                            case C_GET_FRIEND_REQUEST:
                                getUserFriendRequest(inputmsg);
                                break;

                            case C_CONVERSATION_CHAT:
                                sendMessageToConversation(inputmsg);
                                break;

                            case C_SHOW_CONVERSATION_PROPERTY:
                                getConversationProperty(inputmsg);
                                break;

                            case C_SHOW_CONVERSATION_CHAT:
                            	getContextConversation(inputmsg);
                            	break;

                            case C_UPDATE_CONVERSATION:
                                getUserConversation(this.user.getID());
                                break;

                            case C_CREATE_FRIEND_SHIP:
                                createFriendship(inputmsg);
                                break;

                            case C_DECLINE_FRIEND_REQUEST:
                                declineFriendRequest(inputmsg);
                                break;

                            case C_CREATE_GROUP:
                            	User adminGroupUser = names.get(inputmsg.getName());
                            	createNewGroup(adminGroupUser, inputmsg.getMsg());
                            	break;
                            
                            case C_SEND_GROUP_REQUEST:
                            	sendRequestToAddGroup(inputmsg);
                            	break;
                            
                            case C_UPDATE_CONVERSATION_GROUP:
                            	getUserConversationGroup(this.user.getID());
                            	break;
                            	
                            case C_GET_GROUP_REQUEST:
                            	getUserGroupRequest(inputmsg);
                            	break;
                            
                            case C_ADD_PEOPLE_TO_GROUP:
                            	addUserToGroup(inputmsg);
                            	break;
                            
                            case C_REMOVE_FROM_GROUP:
                            	removeUserFromGroup(inputmsg);
                            	break;
                        }
                    }
                }
            } catch (SocketException socketException) {
                logger.error("Socket Exception for user " + name);
            } catch (InvalidUserException duplicateException){
                logger.error("Duplicate Username: " + name);
            } catch (Exception e){
                logger.error("User" + name + "disconnected");
            } finally {
                closeConnections();
            }
        }

        // When send request for join group to userTarget
        private synchronized boolean sendRequestToAddGroup(Message inputMsg) throws IOException, InvalidUserException {
            String targetName = inputMsg.getName();
            //boolean isValid = true;

            if (names.get(targetName) == null) {
                // Target user does not exist
                sendErrorToUser(this.output, "User "+targetName+" does not exist");
                return false;
            }

            if (targetName.equals(name)) {
                // Send request to themselves
                sendErrorToUser(this.output, "Cannot send request to yourself");
                return false;
            }
            User userAdmin=names.get(name);
            User userTarget=names.get(targetName);
            try (Connection connection = DatabaseManager.getConnection()) {
                if (connection == null) {
                    logger.error("Cannot connect to database!");
                    return false; 
                }
                // Check if there are request to targetUser invite group
                String checkRequestQuery = "SELECT * FROM GroupRequest WHERE receiver_id=? AND sender_id=? AND conversation_id=?";
                try (PreparedStatement st = connection.prepareStatement(checkRequestQuery)) {
                    st.setInt(1, userAdmin.getID());
                    st.setInt(2, userTarget.getID());
                    st.setInt(3, inputMsg.getTargetConversationID());

                    try (ResultSet rs = st.executeQuery()) {
                        if (rs.next()) {
                            // Already send request
                            sendErrorToUser(this.output, "You have sent "+targetName+" a request to join group "+inputMsg.getMsg());
                            return false;
                        }
                    }
                }

                // Check if userTarget already in group
                String checkInGroupQuery = "SELECT * FROM ChatMember WHERE user_id=? AND conversation_id=?";
                try (PreparedStatement st = connection.prepareStatement(checkInGroupQuery)) {
                	st.setInt(1, userTarget.getID());
                    st.setInt(2, inputMsg.getTargetConversationID());

                    try (ResultSet rs = st.executeQuery()) {
                        if (rs.next()) {
                            // Already request
                            sendNotificationToUser(this.output, targetName + " already in group "+inputMsg.getMsg());
                            return false;
                        }
                    }
                }

                // Insert new join request
                String insertFriendshipQuery = "INSERT INTO GroupRequest (sender_id, receiver_id, conversation_id, create_dt) VALUES (?, ?, ?, NOW())";
                try (PreparedStatement st = connection.prepareStatement(insertFriendshipQuery)) {

                    st.setInt(1, userAdmin.getID());
                    st.setInt(2, userTarget.getID());
                    st.setInt(3, inputMsg.getTargetConversationID());

                    int affectedRows = st.executeUpdate();
                    if (affectedRows > 0) {
                        logger.info("Group request sent from user {} to user {}", userAdmin.getID(), userTarget.getID());
                        sendNotificationToUser(this.output, "Successfully sent a request join group "+inputMsg.getMsg()+" to user " + targetName);
                        return false;
                    } else {
                        logger.error("Failed to sent friend request from user {} to user {}", userAdmin.getID(), userTarget.getID());
                        sendErrorToUser(this.output, "Fail to sent friend request to user " + targetName);
                        return false;
                    }
                }

            } catch (SQLException sqlException) {
                logger.error("SQL Exception: " + sqlException.getMessage(), sqlException);
                return false;
            } catch (IOException ioException) {
                logger.error("IO Exception: " + ioException.getMessage(), ioException);
                return false;
            }
        }
        
        // When user create new group (non another user)
        private void createNewGroup(User userAdmin,String groupName) throws IOException, InvalidUserException{
        	logger.info("New group has been created from" + userAdmin.getName());
        	try (Connection connection = DatabaseManager.getConnection()) {
                if (connection == null) {
                    logger.error("Cannot connect to database!");
                    return ;
                }           
                ArrayList<User> userList = new ArrayList<User>();
                userList.add(userAdmin);
                int conversationId = createConversation(userList,true);
                
                // Make new group in database and send message for listener
                String insertFriendshipQuery = "INSERT INTO GroupChat(group_name, conversation_id, group_admin) VALUES (?, ?, ?)";
                try (PreparedStatement st = connection.prepareStatement(insertFriendshipQuery)) {
                    st.setString(1, groupName);
                    st.setInt(2, conversationId);
                    st.setInt(3, userAdmin.getID());

                    int affectedRows = st.executeUpdate();
                    if (affectedRows > 0) {
                        logger.info("Group "+groupName+" has been created by "+userAdmin.getName());
                        sendNotificationToUser(this.output, "Successfully create new group with name " + groupName);
                    } else {
                        logger.error("Failed to create group {} from user {}", groupName, userAdmin.getName());
                    }
                }

            } catch (SQLException sqlException) {
                logger.error("SQL Exception: " + sqlException.getMessage(), sqlException);
            } catch (IOException ioException) {
                logger.error("IO Exception: " + ioException.getMessage(), ioException);
            }
        }
        
        // When client change their status
        private void updateClientStatus(int userID, Status status) {
            int friendID;

            // update in userMap
            User user = userMap.get(userID);
            String userName = user.getName();
            if (user != null) {
                user.setStatus(status);
            }

            try (Connection connection = DatabaseManager.getConnection()) {
                logger.info("getConnection() exit");
                if (connection == null) {
                    logger.info("Cannot connect to database!");
                    return ;
                }

                // send notification to this user's friends
                String getFriendsQuery = "SELECT CASE WHEN user1_id = ? THEN user2_id ELSE user1_id END AS friend_id " +
                                         "FROM Friendship WHERE user1_id = ? OR user2_id = ?";
                
                try (PreparedStatement st = connection.prepareStatement(getFriendsQuery)) {
                    st.setInt(1, userID);
                    st.setInt(2, userID);
                    st.setInt(3, userID);

                    try (ResultSet rs = st.executeQuery()) {
                        while (rs.next()) {
                            friendID = rs.getInt("friend_id");
                            User friend = userMap.get(friendID);
                            
                            // if friend is offline, skip
                            if (friend.getStatus() == Status.OFFLINE) {
                                continue;
                            }

                            sendNotificationToUser(writers.get(friendID), "Your friend " + userName + " is " + status.name());
                        }
                    }
                }

            } catch (SQLException sqlException) {
                logger.error("SQL Exception: " + sqlException.getMessage(), sqlException);
            } catch (IOException ioException) {
                logger.error("IO Exception: " + ioException.getMessage(), ioException);
            } finally {
                logger.info("send response to client");
            }

        }

        // when client choose conversation, its property will be loaded
        private void getConversationProperty(Message inputMsg) throws SQLException, IOException {
            int conID=inputMsg.getTargetConversationID();
            // logger.debug("User with name "+inputMsg.getName() + " change to conversation with ID = {}" + conID);
            ArrayList<Message> context = new ArrayList<Message>();
            try (Connection connection = DatabaseManager.getConnection()) {
                logger.info("getConnection() exit");
                if (connection != null) {
                    logger.info("Successfully connected to the database!");
                } else {
                    logger.info("Cannot connect to database!");
                    return ;
                }

                try (PreparedStatement st = connection.prepareStatement(
                        "SELECT * FROM Conversation WHERE conversation_id=?")) {
                    st.setInt(1, conID);
                    try (ResultSet rs = st.executeQuery()) {
                        if (rs.next()) {
                            // check if this conversation is a group
                            if (rs.getInt("is_group") == 0) {
                                // It's not a group, so get the friendship details
                                try (PreparedStatement st2 = connection.prepareStatement(
                                        "SELECT user1_id, user2_id FROM Friendship WHERE conversation_id = ?")) {
                                    st2.setInt(1, conID);
                                    try (ResultSet rs2 = st2.executeQuery()) {
                                        if (rs2.next()) {
                                            Message msg = new Message();
                                            msg.setType(MessageType.S_SHOW_CONVERSATION_PROPERTY);

                                            // Find the IDs of the two users in the conversation
                                            int user1ID = rs2.getInt("user1_id");
                                            int user2ID = rs2.getInt("user2_id");

                                            // Determine the friend (the user that is not the current user)
                                            int friendID;
                                            if (user1ID == names.get(this.name).getID()) {
                                                friendID = user2ID;
                                            } else {
                                                friendID = user1ID;
                                            }

                                            // Set the friend's name in the message
                                            msg.setName(userMap.get(friendID).getName());

                                            // Send the message to the target
                                            sendMessageToTarget(this.output, msg);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    return ;
                }
            } catch (SQLException sqlException) {
                logger.error("SQL Exception: " + sqlException.getMessage(), sqlException);
            } catch (IOException ioException) {
                logger.error("IO Exception: " + ioException.getMessage(), ioException);
            } finally {
                //logger.info("send response to client");
            }
        }

        // when client request to load message of conversation
        private void getContextConversation(Message inputMsg) throws SQLException, IOException {
        	int conID=inputMsg.getTargetConversationID();
        	logger.debug("User with name "+inputMsg.getName() + " change to conversation with ID = {}" + conID);
        	ArrayList<Message> context = new ArrayList<Message>();
        	try (Connection connection = DatabaseManager.getConnection()) {
                logger.info("getConnection() exit");
                if (connection != null) {
                    logger.info("Successfully connected to the database!");
                } else {
                    logger.info("Cannot connect to database!");
                    return ;
                }

                try (PreparedStatement st = connection.prepareStatement(
                        "SELECT * FROM Message WHERE conversation_id=? ORDER BY sent_datetime ASC")) {
                    st.setInt(1, conID);
                    try (ResultSet rs = st.executeQuery()) {
                        while (rs.next()) {
                        	// Get all messages from conversation 
                            Message messageGet=new Message();
                            messageGet.setMsg(rs.getString("context"));
                            messageGet.setName(userMap.get(rs.getInt("sender_id")).getName());
                            context.add(messageGet);
                        }
                    }

                    // Send this to allow this user to login
                    logger.info("Get all context from conversation {}"+conID);
                    Message msg = new Message();
                    msg.setType(MessageType.S_SHOW_CONVERSATION_CHAT);
                    msg.setContext(context);
                    sendMessageToTarget(this.output, msg);

                    return ;
                }
            } catch (SQLException sqlException) {
                logger.error("SQL Exception: " + sqlException.getMessage(), sqlException);
            } catch (IOException ioException) {
                logger.error("IO Exception: " + ioException.getMessage(), ioException);
            } finally {
                //logger.info("send response to client");
            }
        }
        

        private boolean sendMessageToConversation(Message inputMsg) throws IOException {
            logger.debug("User with name "+inputMsg.getName() + " trying to send message");
            User senderUser = names.get(inputMsg.getName());
            int senderID = senderUser.getID();
            int targetConversationID = inputMsg.getTargetConversationID();
            
            try (Connection connection = DatabaseManager.getConnection()) {
                logger.info("getConnection() exit");
                if (connection != null) {
                    logger.info("Successfully connected to the database!");
                } else {
                    logger.info("Cannot connect to database!");
                    return false;
                }

                // Get user from the conversation
                ArrayList<Integer> userIDs = new ArrayList<>();
                try (PreparedStatement st = connection.prepareStatement(
                        "SELECT user_id FROM ChatMember WHERE conversation_id=?")) {
                    st.setInt(1, targetConversationID);
                    try (ResultSet rs = st.executeQuery()) {
                        while (rs.next()) {
                            logger.info("get user with id " + rs.getInt("user_id"));
                            userIDs.add(rs.getInt("user_id"));
                        }
                    }
                }

                // Insert new friend request
                String insertMessageQuery = "INSERT INTO Message (context, sender_id, conversation_id) VALUES (?, ?, ?)";
                try (PreparedStatement st = connection.prepareStatement(insertMessageQuery)) {

                    st.setString(1, inputMsg.getMsg());
                    st.setInt(2, senderID);
                    st.setInt(3, targetConversationID);

                    int affectedRows = st.executeUpdate();
                    int conversationSize = userIDs.size();
                    logger.info("Message sent to conversation {} with {} users", targetConversationID, conversationSize);
                    if (affectedRows > 0) {

                        // Send the message to each user in the conversation
                        for (int userID : userIDs) {
                            logger.info("attempt to send to user " + userID);
                            
                            ObjectOutputStream targetOutput = writers.get(userID);
                            // if user not online, just write to database
                            if (targetOutput == null) {
                                logger.info("User with ID " + userID + " not online");
                                continue;
                            }

                            Message sendMessage = new Message();
                            sendMessage.setType(MessageType.S_CONVERSATION_CHAT);
                            sendMessage.setName(inputMsg.getName());
                            sendMessage.setMsg(inputMsg.getMsg());
                            sendMessage.setTargetConversationID(targetConversationID);
                            sendMessageToTarget(targetOutput, sendMessage);
                            logger.info("message sent to User with ID " + userID);
                        }
                    } else {
                        logger.error("Failed to sent message from user {} to conversation {}", senderID, targetConversationID);
                        return false;
                    }
                }

            } catch (SQLException sqlException) {
                logger.error("SQL Exception: " + sqlException.getMessage(), sqlException);
                return false;
            } catch (IOException ioException) {
                logger.error("IO Exception: " + ioException.getMessage(), ioException);
                return false;
            } finally {
            }
            return true;
        }

        // When user want to login
        private synchronized boolean validateClient(Message validateMessage) throws IOException, InvalidUserException {
            boolean isValid = false;

            logger.info(validateMessage.getName() + " is trying to login");
            
            if (names.get(validateMessage.getName()) == null) {
                sendErrorToUser(this.output, "This user do not exist, please register a new one");
                return isValid;
            }

            // Check if user is already online
            int userID = names.get(validateMessage.getName()).getID();
            if (onlineUserMap.get(userID) != null) {
                logger.info("User " + validateMessage.getName() + " is already online");
                sendErrorToUser(this.output, "You are already online, please check again");
                return isValid;
            }

            try (Connection connection = DatabaseManager.getConnection()) {
                logger.info("getConnection() exit");
                if (connection != null) {
                    logger.info("Successfully connected to the database!");
                } else {
                    logger.info("Cannot connect to database!");
                    return false;
                }

                try (PreparedStatement st = connection.prepareStatement(
                        "SELECT password FROM User WHERE user_id=?")) {
                    st.setInt(1, userID);
                    try (ResultSet rs = st.executeQuery()) {
                        if (rs.next()) {

                            String hashedPassword = rs.getString("password");
                            if (!BCrypt.checkpw(validateMessage.getPassword(), hashedPassword)) {
                                // password not match
                                logger.info("Wrong password");
                                sendErrorToUser(this.output, "Wrong password, please check again");
                                return false;
                            }

                            // Send this to allow this user to login
                            logger.info("Correct password");
                            Message msg = new Message();
                            msg.setType(MessageType.S_LOGIN);
                            msg.setName(validateMessage.getName());
                            sendMessageToTarget(this.output, msg);
                            isValid = true;

                        } else {
                            logger.info("Cannot found user in database!");
                            sendErrorToUser(this.output, "Cannot found user in database!");
                        }
                    }
                    return isValid;
                }
            } catch (SQLException sqlException) {
                logger.error("SQL Exception: " + sqlException.getMessage(), sqlException);
            } catch (IOException ioException) {
                logger.error("IO Exception: " + ioException.getMessage(), ioException);
            } finally {
                //logger.info("send response to client");
            }
            return isValid;
        }

        private synchronized boolean registerClient(Message registerMessage) throws InvalidUserException {
        
            logger.info(registerMessage.getName() + " is trying to create an account");
        
            try (Connection connection = DatabaseManager.getConnection()) {
                logger.info("getConnection() exit");
                if (connection != null) {
                    logger.info("Successfully connected to the database!");
                } else {
                    logger.info("Cannot connect to database!");
                    return false;
                }
        
                // Check if the username already exists
                try (PreparedStatement st = connection.prepareStatement(
                    "SELECT user_name FROM User WHERE user_name=?")) {
                    st.setString(1, registerMessage.getName());
        
                    try (ResultSet rs = st.executeQuery()) {
                        if (rs.next()) {
                            logger.info("Username exists: " + registerMessage.getName());
                            sendErrorToUser(this.output, "User with name " + registerMessage.getName() + " already exists, please try login");
                            return false;
                        }
                    }
                }
        

                // Hash the password
                String hashedPassword = BCrypt.hashpw(registerMessage.getPassword(), BCrypt.gensalt());

                // Insert the new user if the username does not exist
                String insertUserSQL = "INSERT INTO User (user_name, password) VALUES (?, ?)";
                try (PreparedStatement insertUserStmt = connection.prepareStatement(insertUserSQL, Statement.RETURN_GENERATED_KEYS)) {
                    insertUserStmt.setString(1, registerMessage.getName());
                    insertUserStmt.setString(2, hashedPassword);

                    int affectedRows = insertUserStmt.executeUpdate();
                    if (affectedRows > 0) {
                        try (ResultSet generatedKeys = insertUserStmt.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                int userId = generatedKeys.getInt(1);

                                logger.info("A new user has been inserted successfully: " + registerMessage.getName());
                                Message msg = new Message();
                                msg.setMsg("User with name " + registerMessage.getName() + " is created, please login to continue");
                                msg.setType(MessageType.S_REGISTER);
                                msg.setName("SERVER");
                                sendMessageToTarget(this.output, msg);

                                User user = new User();
                                user.setName(registerMessage.getName());
                                user.setID(userId);

                                names.put(user.getName(), user);
                                userMap.put(userId, user);

                                return true;
                            } else {
                                logger.error("Failed to retrieve user ID for new user: " + registerMessage.getName());
                            }
                        }
                    } else {
                        logger.error("Failed to insert new user: " + registerMessage.getName());
                    }
                }
            } catch (SQLException sqlException) {
                logger.error("SQL Exception: " + sqlException.getMessage(), sqlException);
                
            } catch (IOException ioException) {
                logger.error("IO Exception: " + ioException.getMessage(), ioException);

            } finally {
                logger.info("registerClient() method exit");
            }
        
            return isValid;
        }
        
        // send friend request
        private synchronized boolean sendFriendRequest(Message inputMsg) throws IOException, InvalidUserException {
            String targetName = inputMsg.getName();
            //boolean isValid = true;

            if (names.get(targetName) == null) {
                // Target user does not exist
            	sendErrorToUser(this.output, "User "+targetName+" does not exist");
                return false;
            }

            if (targetName.equals(name)) {
                // Send request to themselves
            	sendErrorToUser(this.output, "Cannot send request to yourself");
                return false;
            }

            User requestUser = names.get(name);
            User targetUser = names.get(targetName);

            int requestUserID = requestUser.getID();
            int targetUserID = targetUser.getID();

            try (Connection connection = DatabaseManager.getConnection()) {
                if (connection == null) {
                    logger.error("Cannot connect to database!");
                    return false;
                }

                // Check if they are already friends
                String checkFriendshipQuery = "SELECT * FROM Friendship WHERE user1_id=? AND user2_id=?";
                try (PreparedStatement st = connection.prepareStatement(checkFriendshipQuery)) {
                    st.setInt(1, Math.min(requestUserID, targetUserID));
                    st.setInt(2, Math.max(requestUserID, targetUserID));

                    try (ResultSet rs = st.executeQuery()) {
                        if (rs.next()) {
                            // Already friends
                            sendErrorToUser(this.output, "You and "+targetName+" already are friends!");
                            return false;
                        }
                    }
                }

                // Check if there are already friend request
                String checkFriendRequestQuery = "SELECT * FROM FriendRequest WHERE sender_id=? AND receiver_id=?";
                try (PreparedStatement st = connection.prepareStatement(checkFriendRequestQuery)) {
                    st.setInt(1, requestUserID);
                    st.setInt(2, targetUserID);

                    try (ResultSet rs = st.executeQuery()) {
                        if (rs.next()) {
                            // Already request
                            sendNotificationToUser(this.output, "You have already sent request to "+targetName+", please wait them to accept");
                            return false;
                        }
                    }
                }

                // Insert new friend request
                String insertFriendshipQuery = "INSERT INTO FriendRequest (sender_id, receiver_id) VALUES (?, ?)";
                try (PreparedStatement st = connection.prepareStatement(insertFriendshipQuery)) {

                    st.setInt(1, requestUserID);
                    st.setInt(2, targetUserID);

                    int affectedRows = st.executeUpdate();
                    if (affectedRows > 0) {
                        logger.info("Friend request sent from user {} to user {}", requestUserID, targetUserID);
                        sendNotificationToUser(this.output, "Successfully sent friend request to user " + targetName);
                        return false;
                    } else {
                        logger.error("Failed to sent friend request from user {} to user {}", requestUserID, targetUserID);
                        sendErrorToUser(this.output, "Fail to sent friend request to user " + targetName);
                        return false;
                    }
                }

            } catch (SQLException sqlException) {
                logger.error("SQL Exception: " + sqlException.getMessage(), sqlException);
                return false;
            } catch (IOException ioException) {
                logger.error("IO Exception: " + ioException.getMessage(), ioException);
                return false;
            }
        }

        private void getUserGroupRequest(Message inputMsg) throws IOException{
            String userName = inputMsg.getName();
            int userID = names.get(userName).getID();
            int groupID;

            HashMap<Integer, Conversation> requestMap = new HashMap<Integer, Conversation>(); 

            try (Connection connection = DatabaseManager.getConnection()) {
                if (connection == null) {
                    logger.error("Cannot connect to database!");
                    sendErrorToUser(this.output, "Cannot connect to database!");
                    return;
                }

                // Get user list and store in a map
                String query = "SELECT conversation_id, GroupChat.group_name FROM GroupRequest "
                		+ "JOIN GroupChat ON GroupChat.conversation_id=GroupRequest.conversation_id"
                		+ "WHERE user_target_id=?";
                try (PreparedStatement st = connection.prepareStatement(query)) {
                    st.setInt(1, userID);
                    try (ResultSet rs = st.executeQuery()) {
                        while (rs.next()) {
                            // Retrieve user information from the ResultSet
                            groupID = rs.getInt("conversation_id");

                            Conversation request = new Conversation();
                            request.setConversationID(rs.getInt("conversation_id"));
                            request.setConversationName(rs.getString("group_name"));

                            // Store the user object in the HashMap with username as the key
                            requestMap.put(groupID, request);
                            logger.debug("Sender with ID {} added", groupID);
                        }
                    }
                }

                Message msg = new Message();
                msg.setType(MessageType.S_GET_GROUP_REQUEST);
                msg.setConversationMap(requestMap);
                sendMessageToTarget(output, msg);

            } catch (SQLException e) {
                logger.error("SQL Exception: " + e.getMessage(), e);
                return;
            }
        }
        
        private void getUserFriendRequest(Message inputMsg) throws IOException{
            String userName = inputMsg.getName();
            int userID = names.get(userName).getID();
            int senderID;
            User senderUser;

            HashMap<Integer, Conversation> requestMap = new HashMap<Integer, Conversation>(); 

            try (Connection connection = DatabaseManager.getConnection()) {
                if (connection == null) {
                    logger.error("Cannot connect to database!");
                    sendErrorToUser(this.output, "Cannot connect to database");
                }

                // Get user list and store in a map
                String query = "SELECT sender_id FROM FriendRequest WHERE receiver_id=?";
                try (PreparedStatement st = connection.prepareStatement(query)) {
                    st.setInt(1, userID);
                    try (ResultSet rs = st.executeQuery()) {
                        while (rs.next()) {
                            // Retrieve user information from the ResultSet
                            senderID = rs.getInt("sender_id");

                            senderUser = userMap.get(senderID);
                            Conversation request = new Conversation();
                            request.setConversationID(senderID);
                            request.setConversationName(senderUser.getName());

                            // Store the user object in the HashMap with username as the key
                            requestMap.put(senderID, request);
                            logger.debug("Sender with ID {} added", senderID);
                        }
                    }
                }

                Message msg = new Message();
                msg.setType(MessageType.S_GET_FRIEND_REQUEST);
                msg.setConversationMap(requestMap);
                sendMessageToTarget(output, msg);

            } catch (SQLException e) {
                logger.error("SQL Exception: " + e.getMessage(), e);
                return;
            }
        }

        
        private void removeUserFromGroup(Message message) throws IOException, InvalidUserException {
            User user = names.get(message.getName());
            int conversationId=message.getTargetConversationID();

            try (Connection connection = DatabaseManager.getConnection()) {
                if (connection == null) {
                    logger.error("Cannot connect to database!");
                    return ;
                }

                // Check if user already in group
                String deleteUserQuery = "DELETE FROM ChatMember WHERE group_id=? AND receiver_id=?";
                try (PreparedStatement stateDelete = connection.prepareStatement(deleteUserQuery)) {
                    stateDelete.setInt(1, conversationId);
                    stateDelete.setInt(2, user.getID());

                    int affectedRows = stateDelete.executeUpdate();
                    if (affectedRows > 0) {
                        // logger.info("");
                    } else {
                        logger.error("Cant delete user or not exist");
                        return ;
                    }
                }
            } catch (SQLException sqlException) {
                logger.error("SQL Exception: " + sqlException.getMessage(), sqlException);
            } 
        }
        
        // 
        private boolean addUserToGroup(Message message) throws InvalidUserException {
            User user = names.get(message.getName());
            int conversationId=message.getTargetConversationID();

            try (Connection connection = DatabaseManager.getConnection()) {
                if (connection == null) {
                    logger.error("Cannot connect to database!");
                    return false;
                }

                // Check if user already in group
                String checkFriendshipQuery = "SELECT * FROM ChatMember WHERE conversation_id=? AND user_id=?";
                try (PreparedStatement st = connection.prepareStatement(checkFriendshipQuery)) {
                    st.setInt(1, conversationId);
                    st.setInt(2, user.getID());

                    try (ResultSet rs = st.executeQuery()) {
                        if (rs.next()) {
                            // Already in group
                            logger.info("Already in group");
                            sendErrorToUser(this.output, "You has been in this group");
                            return false;
                        }
                    }
                }

                // Delete group request
                String deleteFriendRequestQuery = "DELETE FROM GroupRequest WHERE group_id=? AND receiver_id=?";
                try (PreparedStatement st = connection.prepareStatement(deleteFriendRequestQuery)) {
                    st.setInt(1, message.getTargetConversationID());
                    st.setInt(2, user.getID());

                    int affectedRows = st.executeUpdate();
                    if (affectedRows > 0) {
                        // logger.info("");
                    } else {
                        logger.error("Cant delete group request or not exist");
                        return false;
                    }
                }           

                // Make friendship
                String insertFriendshipQuery = "INSERT INTO ChatMember (conversation_id, user_id) VALUES (?, ?)";
                try (PreparedStatement st = connection.prepareStatement(insertFriendshipQuery)) {
                    st.setInt(1, message.getTargetConversationID());
                    st.setInt(2, user.getID());

                    int affectedRows = st.executeUpdate();
                    if (affectedRows > 0) {
                        logger.info("You has been join group " + message.getMsg());
                        sendNotificationToUser(this.output, "You has been join group " + message.getMsg());
                        return true;
                    } else {
                        logger.error("Failed to join grop {}", message.getMsg());
                        return false;
                    }
                }

            } catch (SQLException sqlException) {
                logger.error("SQL Exception: " + sqlException.getMessage(), sqlException);
                return false;
            } catch (IOException ioException) {
                logger.error("IO Exception: " + ioException.getMessage(), ioException);
                return false;
            }
        }
        
        private boolean createFriendship(Message message) throws InvalidUserException {
            User requestUser = names.get(message.getName());
            int requestID = requestUser.getID();
            int receiverID = this.user.getID();

            try (Connection connection = DatabaseManager.getConnection()) {
                if (connection == null) {
                    logger.error("Cannot connect to database!");
                    return false;
                }

                // Check if they are already friends
                String checkFriendshipQuery = "SELECT * FROM Friendship WHERE user1_id=? AND user2_id=?";
                try (PreparedStatement st = connection.prepareStatement(checkFriendshipQuery)) {
                    st.setInt(1, Math.min(requestID, receiverID));
                    st.setInt(2, Math.max(requestID, receiverID));

                    try (ResultSet rs = st.executeQuery()) {
                        if (rs.next()) {
                            // Already friends
                            logger.info("Already friends");
                            sendErrorToUser(this.output, "You and this user are already friend");
                            return false;
                        }
                    }
                }

                // Delete friend request
                String deleteFriendRequestQuery = "DELETE FROM FriendRequest WHERE sender_id=? AND receiver_id=?";
                try (PreparedStatement st = connection.prepareStatement(deleteFriendRequestQuery)) {
                    st.setInt(1, requestID);
                    st.setInt(2, receiverID);

                    int affectedRows = st.executeUpdate();
                    if (affectedRows > 0) {
                        // logger.info("");
                    } else {
                        logger.error("Cant delete friend request or not exist");
                        return false;
                    }
                }

                ArrayList<User> userList = new ArrayList<User>();
                userList.add(userMap.get(requestID));
                userList.add(userMap.get(receiverID));

                int conversationID = createConversation(userList,false);           

                // Make friendship
                String insertFriendshipQuery = "INSERT INTO Friendship (user1_id, user2_id, conversation_id) VALUES (?, ?, ?)";
                try (PreparedStatement st = connection.prepareStatement(insertFriendshipQuery)) {
                    st.setInt(1, Math.min(requestID, receiverID));
                    st.setInt(2, Math.max(requestID, receiverID));
                    st.setInt(3, conversationID);

                    int affectedRows = st.executeUpdate();
                    if (affectedRows > 0) {
                        logger.info("Friendship sent between user {} to user {} has been made", requestID, receiverID);
                        // Message msg = new Message();
                        // msg.setType(MessageType.S_CREATE_FRIEND_SHIP);
                        // msg.setMsg("You and " + message.getName() + " are friend now");
                        // sendMessageToTarget(output, msg);
                        sendNotificationToUser(this.output, "You and " + message.getName() + " are friend now");
                        return true;
                    } else {
                        logger.error("Failed to accept friend request from user {}", requestID);
                        return false;
                    }
                }

            } catch (SQLException sqlException) {
                logger.error("SQL Exception: " + sqlException.getMessage(), sqlException);
                return false;
            } catch (IOException ioException) {
                logger.error("IO Exception: " + ioException.getMessage(), ioException);
                return false;
            }
        }

        private boolean declineFriendRequest(Message message) throws InvalidUserException {
            User requestUser = names.get(message.getName());
            int requestID = requestUser.getID();
            int receiverID = this.user.getID();

            try (Connection connection = DatabaseManager.getConnection()) {
                if (connection == null) {
                    logger.error("Cannot connect to database!");
                    return false;
                }

                // Delete friend request
                String deleteFriendRequestQuery = "DELETE FROM FriendRequest WHERE sender_id=? AND receiver_id=?";
                try (PreparedStatement st = connection.prepareStatement(deleteFriendRequestQuery)) {
                    st.setInt(1, requestID);
                    st.setInt(2, receiverID);

                    int affectedRows = st.executeUpdate();
                    if (affectedRows > 0) {
                        sendNotificationToUser(this.output, "Successfully delete friend request from " + message.getName());
                    } else {
                        logger.error("Cant delete friend request or not exist");
                        sendNotificationToUser(this.output, "Fail to delete friend request from " + message.getName());
                        return false;
                    }
                    return true;
                }

            } catch (SQLException sqlException) {
                logger.error("SQL Exception: " + sqlException.getMessage(), sqlException);
                return false;
            } catch (IOException ioException) {
                logger.error("IO Exception: " + ioException.getMessage(), ioException);
                return false;
            }
        }
        
        private synchronized int createConversation(ArrayList<User> userList, boolean isGroup) throws IOException, InvalidUserException {
            int createConversationID = -1;

            logger.info("Server is trying to create a conversation with " + userList.size() + " users including:");
            for (User user : userList) {
                logger.info(user.getName() + "\n");
            }

            try (Connection connection = DatabaseManager.getConnection()) {
                if (connection == null) {
                    logger.info("Cannot connect to database!");
                    return -1;
                }
                logger.info("Successfully connected to the database!");

                // Insert new conversation 
                String insertConversationSQL = "INSERT INTO Conversation (is_group) VALUES (?)";
                try (PreparedStatement st = connection.prepareStatement(insertConversationSQL, Statement.RETURN_GENERATED_KEYS)) {
                  st.setInt(1, isGroup ? 1:0);

                    int affectedRows = st.executeUpdate();
                    if (affectedRows > 0) {
                        try (ResultSet keys = st.getGeneratedKeys()) {
                            if (keys.next()) {
                                createConversationID = keys.getInt(1);
                                logger.info("A conversation has been created with ID " + createConversationID);
                            } else {
                                logger.error("Failed to retrieve the ID of the new conversation.");
                                return createConversationID;
                            }
                        }
                    } else {
                        logger.error("Failed to insert new conversation from: " + userList.get(0).getName());
                        return -1;
                    }
                }

                for (User user : userList) {
                    // Update chat member
                    String insertChatMemberSQL = "INSERT INTO ChatMember (conversation_id, user_id) VALUES (?, ?)";
                    try (PreparedStatement st = connection.prepareStatement(insertChatMemberSQL)) {
                        st.setInt(1, createConversationID);
                        st.setInt(2, user.getID());
                        int affectedRows = st.executeUpdate();
                        if (affectedRows > 0) {
                            logger.info("User with ID " + user.getID() + " has been added to the conversation");
                        } else {
                            logger.error("Failed to add user with ID " + user.getID() + " to the conversation");
                            return -1;
                        }
                    }
                }

            } catch (SQLException sqlException) {
                logger.error("SQL Exception: " + sqlException.getMessage(), sqlException);
                return -1;
            }

            logger.info("createConversation() method exit");
            return createConversationID;
        }

        //Return the list of group of user
        private synchronized void getUserConversationGroup(int userID) throws IOException {            
            try (Connection connection = DatabaseManager.getConnection()) {
                HashMap<Integer, Conversation> userConversationGroupMap = new HashMap<>();

                if (connection == null) {
                    logger.info("Cannot connect to database!");
                    return;
                }
                logger.info("Successfully connected to the database!");

                // find group conversation
                String insertConversationSQL = "SELECT conversation_id, GroupChat.group_name, GroupChat.group_admin FROM Conversation "
                		+ "JOIN ChatMember ON ChatMember.conversation_id=Conversation.conversation_id"
                		+ "JOIN GroupChat ON GroupChat.conversation_id=ChatMember.conversation_id"
                		+ "WHERE ChatMember.user_id=? and Conversation.is_group=1";
                try (PreparedStatement st = connection.prepareStatement(insertConversationSQL, Statement.RETURN_GENERATED_KEYS)) {
                    st.setInt(1, userID);

                    try (ResultSet rs = st.executeQuery()) {
                        while (rs.next()) {

                            Conversation conversation = new Conversation();
                            conversation.setGroup(true);
                            conversation.setConversationID(rs.getInt("conversation_id"));
                            conversation.setConversationName(rs.getString("group_name"));
                            conversation.setGroupMaster(userMap.get(rs.getInt("group_admin")));

                            logger.info("Loaded a group conversation with id " + rs.getInt("conversation_id"));
                            userConversationGroupMap.put(rs.getInt("conversation_id"), conversation);
                        }
                    }
                }

                // Send group conversation map to user
                Message msg = new Message();
                msg.setType(MessageType.S_UPDATE_CONVERSATION_GROUP);
                msg.setConversationMap(userConversationGroupMap);
                logger.info("Size of conversation map: " + userConversationGroupMap.size());
                sendMessageToTarget(this.output, msg);

            } catch (SQLException sqlException) {
                logger.error("SQL Exception: " + sqlException.getMessage(), sqlException);
            }
        }
        
        
        //Return the list of conversation view of client 
        private synchronized void getUserConversation(int userID) throws IOException {            
            try (Connection connection = DatabaseManager.getConnection()) {
                HashMap<Integer, Conversation> userConversationMap = new HashMap<>();
                User friend;

                if (connection == null) {
                    logger.info("Cannot connect to database!");
                    return;
                }
                logger.info("Successfully connected to the database!");

                // get friend's info and convert to conversation
                String insertConversationSQL = "SELECT user1_id, user2_id, conversation_id FROM Friendship WHERE user1_id=? OR user2_id=?";
                try (PreparedStatement st = connection.prepareStatement(insertConversationSQL, Statement.RETURN_GENERATED_KEYS)) {
                    st.setInt(1, userID);
                    st.setInt(2, userID);

                    try (ResultSet rs = st.executeQuery()) {
                        while (rs.next()) {

                            if (rs.getInt("user1_id") == userID) {
                                friend = userMap.get(rs.getInt("user2_id"));
                            } else {
                                friend = userMap.get(rs.getInt("user1_id"));
                            }

                            Conversation conversation = new Conversation();
                            conversation.setConversationID(rs.getInt("conversation_id"));
                            conversation.setConversationName(friend.getName());
                            conversation.setUserStatus(friend.getStatus());
                            conversation.setGroup(false);

                            logger.info("Loaded a conversation with id " + rs.getInt("conversation_id"));
                            userConversationMap.put(rs.getInt("conversation_id"), conversation);
                        }
                    }
                }

                // get friend's info and convert to conversation
                String queryGroupNameSQL = "SELECT gc.group_name, cm.conversation_id FROM ChatMember cm JOIN GroupChat gc ON cm.conversation_id = gc.conversation_id WHERE cm.user_id = ?;";
                try (PreparedStatement st = connection.prepareStatement(queryGroupNameSQL, Statement.RETURN_GENERATED_KEYS)) {
                    st.setInt(1, userID);

                    try (ResultSet rs = st.executeQuery()) {
                        while (rs.next()) {

                            Conversation conversation = new Conversation();
                            conversation.setConversationID(rs.getInt("conversation_id"));
                            conversation.setConversationName(rs.getString("group_name"));
                            conversation.setUserStatus(Status.ONLINE);
                            conversation.setGroup(true);

                            logger.info("Loaded a conversation with id " + rs.getInt("conversation_id"));
                            userConversationMap.put(rs.getInt("conversation_id"), conversation);
                        }
                    }
                }

                // Send conversation map to user
                Message msg = new Message();
                msg.setType(MessageType.S_UPDATE_CONVERSATION);
                msg.setConversationMap(userConversationMap);
                logger.info("Size of conversation map: " + userConversationMap.size());
                sendMessageToTarget(this.output, msg);

            } catch (SQLException sqlException) {
                logger.error("SQL Exception: " + sqlException.getMessage(), sqlException);
                sendErrorToUser(this.output, "Get error when load conversation");
            }
        }


        private void sendMessageToTarget(ObjectOutputStream target, Message msg) throws IOException {
            target.writeObject(msg);
            target.flush();
            logger.info("Message sent to client: " + msg.getType());
        }

        private void sendMessageToAll(Message msg) throws IOException {
            // loop through all users
            for (ObjectOutputStream writer : writers.values()) {
                sendMessageToTarget(writer, msg);
                writer.reset();
            }
        }

        // // when an user logout
        // private Message removeFromList() throws IOException {
        //     logger.debug("removeFromList() method Enter");
        //     Message msg = new Message();
        //     msg.setMsg("has left the chat.");
        //     msg.setType(MessageType.DISCONNECTED);
        //     msg.setName("SERVER");
        //     msg.setUserlist(users);
        //     sendMessageToAll(msg);
        //     logger.debug("removeFromList() method Exit");
        //     return msg;
        // }

        private synchronized void closeConnections() {
            updateClientStatus(this.user.getID(), Status.OFFLINE);
            logger.debug("closeConnections() method Enter");

            if (user != null) {
                onlineUserMap.remove(this.user.getID());
                // logger.info("User object: " + user + " has been removed!");

                writers.remove(this.user.getID());
                // logger.info("Writer object: " + output + " has been removed!");
            }
            closeQuietly(is);
            closeQuietly(input);
            closeQuietly(os);
            closeQuietly(output);

            logger.info("HashMap online user:" + onlineUserMap.size() + " writers:" + writers.size() + " usersList size:" + users.size());
            logger.debug("closeConnections() method Exit");
        }

        private void sendErrorToUser (ObjectOutputStream userOutput, String message) throws IOException {
            Message msg = new Message();
            msg.setMsg(message);
            msg.setType(MessageType.S_ERROR);
            msg.setName("SERVER");
            sendMessageToTarget(userOutput, msg);
        }

        private void sendNotificationToUser(ObjectOutputStream userOutput, String message) throws IOException {
            Message msg = new Message();
            msg.setMsg(message);
            msg.setType(MessageType.S_NOTIFICATION);
            msg.setName("SERVER");
            sendMessageToTarget(userOutput, msg);
        }

        private void closeQuietly(Closeable closeable) {
            if (closeable != null) {
                try {
                    closeable.close();
                    //logger.info(closeable.getClass().getSimpleName() + " closed.");
                } catch (IOException e) {
                    logger.error("Error closing " + closeable.getClass().getSimpleName() + ": " + e.getMessage(), e);
                }
            }
        }
    }
}