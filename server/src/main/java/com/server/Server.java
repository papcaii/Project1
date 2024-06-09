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

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import java.util.ArrayList;
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
                            case USER_MESSAGE:
                                sendMessageToTarget(this.output, inputmsg);
                                break;
                            case LOGIN:
                                isValid = validateClient(inputmsg);
                                if (isValid) {
                                    this.name = inputmsg.getName();
                                    this.user = names.get(inputmsg.getName());
                                    writers.put(user.getID(), this.output);
                                    onlineUserMap.put(user.getID(), user);
                                    logger.info(user.getName()+" login now");
                                    getUserConversation(user.getID());
                                }
                                break;
                            case REGISTER:
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
                        }
                    }
                }
            } catch (SocketException socketException) {
                logger.error("Socket Exception for user " + name);
            } catch (InvalidUserException duplicateException){
                logger.error("Duplicate Username: " + name);
            } catch (Exception e){
                logger.error("Exception in run() method for user: " + name, e);
            } finally {
                closeConnections();
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
                            userIDs.add(rs.getInt("user_id"));
                        }
                    }
                }

                // Insert new friend request
                String insertMessageQuery = "INSERT INTO Message (context, sent_datetime, sender_id, conversation_id) VALUES (?, NOW(), ?, ?)";
                try (PreparedStatement st = connection.prepareStatement(insertMessageQuery)) {

                    st.setString(1, inputMsg.getMsg());
                    st.setInt(2, senderID);
                    st.setInt(3, targetConversationID);

                    int affectedRows = st.executeUpdate();
                    if (affectedRows > 0) {
                        logger.info("Message sent from user {} to conversation {}", senderID, targetConversationID);

                        // Send the message to each user in the conversation
                        for (int userID : userIDs) {
                            
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

                            return true;
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
                return true;
            }
        }

        private synchronized boolean validateClient(Message validateMessage) throws InvalidUserException {
            boolean isValid = false;

            logger.info(validateMessage.getName() + " is trying to connect with password " + validateMessage.getPassword());
            try (Connection connection = DatabaseManager.getConnection()) {
                logger.info("getConnection() exit");
                if (connection != null) {
                    logger.info("Successfully connected to the database!");
                } else {
                    logger.info("Cannot connect to database!");
                    return false;
                }

                try (PreparedStatement st = connection.prepareStatement(
                        "SELECT user_name, password FROM User WHERE user_name=? AND password=?")) {
                    st.setString(1, validateMessage.getName());
                    st.setString(2, validateMessage.getPassword());
                    try (ResultSet rs = st.executeQuery()) {
                        if (rs.next()) {
                            // Send this to allow this user to login
                            logger.info("Correct password");
                            Message msg = new Message();
                            msg.setType(MessageType.ACCEPTED);
                            msg.setName(validateMessage.getName());
                            sendMessageToTarget(this.output, msg);
                            isValid = true;

                            // 

                        } else {
                            logger.info("Incorrect password!");
                            Message msg = new Message();
                            msg.setMsg("Wrong username or password, please try again");
                            msg.setType(MessageType.DECLINED);
                            msg.setName("SERVER");
                            sendMessageToTarget(this.output, msg);
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
        
            logger.info(registerMessage.getName() + " is trying to create an account with password " + registerMessage.getPassword());
            boolean isValid = false;
        
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
                            Message msg = new Message();
                            msg.setMsg("User with name " + registerMessage.getName() + " already exists, please try login");
                            msg.setType(MessageType.DECLINED);
                            msg.setName("SERVER");
                            sendMessageToTarget(this.output, msg);
                            return false;
                        }
                    }
                }
        
                // Insert the new user if the username does not exist
                try (PreparedStatement st = connection.prepareStatement(
                    "INSERT INTO User (user_name, password, is_online, create_datetime) VALUES (?, ?, ?, NOW())")) {
                    st.setString(1, registerMessage.getName());
                    st.setString(2, registerMessage.getPassword());
                    st.setBoolean(3, false);
        
                    int affectedRows = st.executeUpdate();
                    if (affectedRows > 0) {
                        logger.info("A new user has been inserted successfully: " + registerMessage.getName());
                        Message msg = new Message();
                        msg.setMsg("User with name " + registerMessage.getName() + " is created, please login to continue");
                        msg.setType(MessageType.REGISTER_SUCCESS);
                        msg.setName("SERVER");
                        sendMessageToTarget(this.output, msg);
                        User user= new User();
                        user.setName(registerMessage.getName());
                        user.setStatus(Status.ONLINE);
                        int getID=-1;
                        try (PreparedStatement stToGetId = connection.prepareStatement(
                                "SELECT user_id FROM User WHERE user_name = ?")){
                        	stToGetId.setString(1, user.getName());
                        	try(ResultSet rsToGetId=stToGetId.executeQuery()){
                        		if (rsToGetId.next()) {
                        			getID=rsToGetId.getInt("user_id");
                        		}
                        	}
                        }
                        user.setID(getID);

                        return false;
                    } else {
                        logger.error("Failed to insert new user: " + registerMessage.getName());
                        return false;
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
        
        // Add friend and create conversation
        private synchronized boolean sendFriendRequest(Message inputMsg) throws IOException, InvalidUserException {
            String targetName = inputMsg.getName();
            boolean isValid = true;

            if (names.get(targetName) == null) {
                // Target user does not exist
                Message msg = new Message();
                msg.setType(MessageType.S_FRIEND_REQUEST);
                msg.setMsg("User does not exist");
                sendMessageToTarget(output, msg);
                return false;
            }

            if (targetName.equals(name)) {
                // Send request to themselves
                Message msg = new Message();
                msg.setType(MessageType.S_FRIEND_REQUEST);
                msg.setMsg("Cannot send request to yourself");
                sendMessageToTarget(output, msg);
                return false;
            }

            User requestUser = names.get(name);
            User targetUser = names.get(targetName);

            try (Connection connection = DatabaseManager.getConnection()) {
                if (connection == null) {
                    logger.error("Cannot connect to database!");
                    return false;
                }

                // Check if they are already friends
                String checkFriendshipQuery = "SELECT * FROM Friendship WHERE user1_id=? AND user2_id=?";
                try (PreparedStatement st = connection.prepareStatement(checkFriendshipQuery)) {
                    int requestUserID = requestUser.getID();
                    int targetUserID = targetUser.getID();
                    st.setInt(1, Math.min(requestUserID, targetUserID));
                    st.setInt(2, Math.max(requestUserID, targetUserID));

                    try (ResultSet rs = st.executeQuery()) {
                        if (rs.next()) {
                            // Already friends
                            logger.info("Already friends");
                            Message msg = new Message();
                            msg.setType(MessageType.S_FRIEND_REQUEST);
                            msg.setMsg("Already friends");
                            sendMessageToTarget(output, msg);
                            return false;
                        }
                    }
                }

                // Insert new friend request
                String insertFriendshipQuery = "INSERT INTO FriendRequest (sender_id, receiver_id, create_dt) VALUES (?, ?, NOW())";
                try (PreparedStatement st = connection.prepareStatement(insertFriendshipQuery)) {
                    int requestUserID = requestUser.getID();
                    int targetUserID = targetUser.getID();
                    st.setInt(1, requestUserID);
                    st.setInt(2, targetUserID);

                    int affectedRows = st.executeUpdate();
                    if (affectedRows > 0) {
                        logger.info("Friend request sent from user {} to user {}", requestUserID, targetUserID);
                        Message msg = new Message();
                        msg.setType(MessageType.S_FRIEND_REQUEST);
                        msg.setMsg("Successful");
                        sendMessageToTarget(output, msg);
                        return false;
                    } else {
                        logger.error("Failed to sent friend request from user {} to user {}", requestUserID, targetUserID);
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

        private void getUserFriendRequest(Message inputMsg) throws IOException{
            String userName = inputMsg.getName();
            int userID = names.get(userName).getID();
            int senderID;
            User senderUser;

            HashMap<Integer, Conversation> requestMap = new HashMap<Integer, Conversation>(); 

            try (Connection connection = DatabaseManager.getConnection()) {
                if (connection == null) {
                    logger.error("Cannot connect to database!");
                    Message msg = new Message();
                    msg.setType(MessageType.ERROR);
                    msg.setMsg("Cannot connect to database!");
                    sendMessageToTarget(output, msg);
                    return;
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

        
        private synchronized boolean createConversation(ArrayList<User> userList) throws IOException, InvalidUserException {
            int createConversationID = -1;

            logger.info("Server is trying to create a conversation with " + userList.size() + " users including:");
            for (User user : userList) {
                logger.info(user.getName());
            }

            try (Connection connection = DatabaseManager.getConnection()) {
                if (connection == null) {
                    logger.info("Cannot connect to database!");
                    return false;
                }
                logger.info("Successfully connected to the database!");

                // Insert the new conversation 
                String insertConversationSQL = "INSERT INTO Conversation (is_group, create_datetime, group_member) VALUES (0, NOW(), ?)";
                try (PreparedStatement st = connection.prepareStatement(insertConversationSQL, Statement.RETURN_GENERATED_KEYS)) {
                    st.setInt(1, userList.size());

                    int affectedRows = st.executeUpdate();
                    if (affectedRows > 0) {
                        try (ResultSet keys = st.getGeneratedKeys()) {
                            if (keys.next()) {
                                createConversationID = keys.getInt(1);
                                logger.info("A conversation has been created with ID " + createConversationID);
                            } else {
                                logger.error("Failed to retrieve the ID of the new conversation.");
                                return false;
                            }
                        }
                    } else {
                        logger.error("Failed to insert new conversation from: " + userList.get(0).getName());
                        return false;
                    }
                }

                for (User user : userList) {
                    // Update chat member
                    String insertChatMemberSQL = "INSERT INTO ChatMember (conversation_id, user_id, join_datetime) VALUES (?, ?, NOW())";
                    try (PreparedStatement st = connection.prepareStatement(insertChatMemberSQL)) {
                        st.setInt(1, createConversationID);
                        st.setInt(2, user.getID());
                        int affectedRows = st.executeUpdate();
                        if (affectedRows > 0) {
                            logger.info("User with ID " + user.getID() + " has been added to the conversation");
                        } else {
                            logger.error("Failed to add user with ID " + user.getID() + " to the conversation");
                            return false;
                        }
                    }
                }
            } catch (SQLException sqlException) {
                logger.error("SQL Exception: " + sqlException.getMessage(), sqlException);
                return false;
            }

            logger.info("createConversation() method exit");
            return true;
        }

        private synchronized void getUserConversation(int userID) throws IOException {            
            try (Connection connection = DatabaseManager.getConnection()) {
                ArrayList<Integer> conversationIDs = new ArrayList<Integer>();
                HashMap<Integer, Conversation> userConversation = new HashMap<Integer, Conversation>();
                
                if (connection == null) {
                    logger.info("Cannot connect to database!");
                    return;
                }
                logger.info("Successfully connected to the database!");

                // Insert the new conversation 
                String insertConversationSQL = "SELECT conversation_id FROM ChatMember WHERE user_id=?";
                try (PreparedStatement st = connection.prepareStatement(insertConversationSQL, Statement.RETURN_GENERATED_KEYS)) {
                    st.setInt(1, userID);
                    
                    try (ResultSet rs = st.executeQuery()) {
                        while (rs.next()) {
                            conversationIDs.add(rs.getInt("conversation_id"));
                       }
                    }
                }

                // If no conversation IDs were found, return an empty list
                if (conversationIDs.isEmpty()) {
                    return;
                }

                // Query to get the conversation details using the conversation IDs
                String selectConversationsSQL = "SELECT * FROM Conversation WHERE conversation_id IN (" +
                        conversationIDs.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")";
                try (PreparedStatement st = connection.prepareStatement(selectConversationsSQL)) {
                    try (ResultSet rs = st.executeQuery()) {
                        while (rs.next()) {
                            Conversation conversation = new Conversation();
                            conversation.setConversationID(rs.getInt("conversation_id"));
                            conversation.setConversationName(rs.getString("conversation_name"));
                            // Add more fields as necessary
                            userConversation.put(conversation.getConversationID(), conversation);

                            Message msg = new Message();
                            msg.setType(MessageType.S_UPDATE_CONVERSATION);
                            msg.setConversationMap(userConversation);
                            sendMessageToTarget(this.output, msg);
                        }
                    }
                }



            } catch (SQLException sqlException) {
                logger.error("SQL Exception: " + sqlException.getMessage(), sqlException);
                return;
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

        // when an user logout
        private Message removeFromList() throws IOException {
            logger.debug("removeFromList() method Enter");
            Message msg = new Message();
            msg.setMsg("has left the chat.");
            msg.setType(MessageType.DISCONNECTED);
            msg.setName("SERVER");
            msg.setUserlist(users);
            sendMessageToAll(msg);
            logger.debug("removeFromList() method Exit");
            return msg;
        }

        private synchronized void closeConnections() {
            logger.debug("closeConnections() method Enter");
            logger.info("HashMap online user:" + onlineUserMap.size() + " writers:" + writers.size() + " usersList size:" + users.size());

            if (user != null) {
                onlineUserMap.remove(user.getID());
                // logger.info("User object: " + user + " has been removed!");
            }
            if (output != null) {
                writers.remove(user.getID());
                // logger.info("Writer object: " + output + " has been removed!");
            }
            closeQuietly(is);
            closeQuietly(input);
            closeQuietly(os);
            closeQuietly(output);

            /*
            try {
                sendMessageToAll();
            } catch (Exception e) {
                e.printStackTrace();
            }
            */
            logger.info("HashMap names:" + names.size() + " writers:" + writers.size() + " usersList size:" + users.size());
            logger.debug("closeConnections() method Exit");
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