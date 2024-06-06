package com.server;

import com.database.DatabaseManager;
import com.exception.InvalidUserException;
import com.messages.Message;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Server {

    private static final int PORT = 9001;
    private static final HashMap<String, User> names = new HashMap<>();
    private static HashSet<ObjectOutputStream> writers = new HashSet<>();   // List of outputs to user
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
        Connection connection = null;
        try {
            connection = DatabaseManager.getConnection();
            return connection != null; // Return true if connection is not null
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                logger.error("Error closing database connection: " + e.getMessage(), e);
            }
        }
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
                                	TimeUnit time = TimeUnit.SECONDS;
                                	time.sleep(2L);
                                	
                                    user = new User();
                                    user.setName(inputmsg.getName());
                                    user.setStatus(Status.ONLINE);
                                    addToUserList(user);         // add socket output to list

                                    logger.info("New user: " + user.getName());
                                }
                                break;
                            case REGISTER:
                                registerClient(inputmsg);
                                break;

                            case STATUS:
                                changeStatus(inputmsg);
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

        private Message changeStatus(Message inputmsg) throws IOException {
            logger.debug(inputmsg.getName() + " has changed status to " + inputmsg.getStatus());
            Message msg = new Message();
            msg.setName(user.getName());
            msg.setType(MessageType.STATUS);
            msg.setMsg("");
            User userObj = names.get(name);
            userObj.setStatus(inputmsg.getStatus());
            write(msg);
            return msg;
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
                logger.info("send response to client");
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

        private void sendMessageToTarget(ObjectOutputStream target, Message msg) throws IOException {
            target.writeObject(msg);
            target.flush();
            logger.info("Message sent to client: " + msg.getMsg());
        }

        private void sendMessageToAll(Message msg) throws IOException {
            // loop through all users
            for (ObjectOutputStream writer : writers) {
                sendMessageToTarget(writer, msg);
                writer.reset();
            }
        }

        private Message sendNotification(Message firstMessage) throws IOException {
            Message msg = new Message();
            msg.setMsg("has joined the chat.");
            msg.setType(MessageType.NOTIFICATION);
            msg.setName(firstMessage.getName());
            msg.setPicture(firstMessage.getPicture());
            write(msg);
            return msg;
        }

        // when an user logout
        private Message removeFromList() throws IOException {
            logger.debug("removeFromList() method Enter");
            Message msg = new Message();
            msg.setMsg("has left the chat.");
            msg.setType(MessageType.DISCONNECTED);
            msg.setName("SERVER");
            msg.setUserlist(users);
            write(msg);
            logger.debug("removeFromList() method Exit");
            return msg;
        }

        // when a new user login success
        private Message addToList() throws IOException {
            Message msg = new Message();
            msg.setMsg("Welcome, You have now joined the server! Enjoy chatting!");
            msg.setType(MessageType.LOGIN);
            msg.setName("SERVER");
            msg.setUserlist(users);
            write(msg);
            return msg;
        }

        // when a new user login success
        private Message addToUserList(User user) throws IOException {
            users.add(user);                    // add user to list
            names.put(user.getName(), user);    // add name to list
            writers.add(this.output);  

            Message msg = new Message();
            msg.setType(MessageType.UPDATE_USER);
            msg.setName("SERVER");
            msg.setUserlist(users);
            sendMessageToAll(msg);
            return msg;
        }

        private void write(Message msg) throws IOException {
            for (ObjectOutputStream writer : writers) {
                writer.writeObject(msg);
                writer.reset();
            }
        }

        private synchronized void closeConnections() {
            logger.debug("closeConnections() method Enter");
            logger.info("HashMap names:" + names.size() + " writers:" + writers.size() + " usersList size:" + users.size());
            if (name != null) {
                names.remove(name);
                logger.info("User: " + name + " has been removed!");
            }
            if (user != null) {
                users.remove(user);
                logger.info("User object: " + user + " has been removed!");
            }
            if (output != null) {
                writers.remove(output);
                logger.info("Writer object: " + output + " has been removed!");
            }
            closeQuietly(is);
            closeQuietly(input);
            closeQuietly(os);
            closeQuietly(output);
            try {
                removeFromList();
            } catch (Exception e) {
                e.printStackTrace();
            }
            logger.info("HashMap names:" + names.size() + " writers:" + writers.size() + " usersList size:" + users.size());
            logger.debug("closeConnections() method Exit");
        }

        private void closeQuietly(Closeable closeable) {
            if (closeable != null) {
                try {
                    closeable.close();
                    logger.info(closeable.getClass().getSimpleName() + " closed.");
                } catch (IOException e) {
                    logger.error("Error closing " + closeable.getClass().getSimpleName() + ": " + e.getMessage(), e);
                }
            }
        }
    }
}
