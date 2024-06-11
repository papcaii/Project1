package com.client.chatwindow;

import com.client.login.LoginController;

import com.messages.Conversation;
import com.messages.Message;
import com.messages.MessageType;
import com.messages.Status;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

import java.util.ArrayList;
import java.util.HashMap;

import static com.messages.MessageType.CONNECTED;

public class Listener implements Runnable {

    private static final String HASCONNECTED = "has connected";

    private static String picture;
    private Socket socket;
    private String hostname;
    private int port;

    private String username;
    private String password;

    public ChatController chatCon;
    public FriendRequestController friendRequestCon;
    
    private InputStream is;
    private OutputStream os;
    private ObjectInputStream input;
    private ObjectOutputStream output;

    public HashMap<Integer, Conversation> conversationMap;

    private boolean socketReady = false;
    private boolean isValid; 
    
    public static Logger logger = LoggerFactory.getLogger(Listener.class);

    //public Listener(String hostname, int port, String username, String password, String picture, ChatController controller) {
    public Listener(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public void setChatController(ChatController chatCon) {
        this.chatCon = chatCon;
    }

    public void setFriendRequestController(FriendRequestController friendRequestCon) {
        this.friendRequestCon = friendRequestCon;
    }

    public boolean isReady() {
        return this.socketReady;
    }

    public void run() {
        try {
            socket = new Socket(this.hostname, this.port);

            if (socket == null) {
                logger.error("Failed to create socket connection to " + this.hostname + ":" + this.port);
                LoginController.getInstance().showErrorDialog("Could not connect to server");
                return;
            }

            logger.info("Connection accepted " + socket.getInetAddress() + ":" + socket.getPort());
            
            // create input output stream
            this.os = socket.getOutputStream();
            this.output = new ObjectOutputStream(os);

            this.is = socket.getInputStream();
            this.input = new ObjectInputStream(is);

            if (this.input != null) {
                logger.info("Input stream ready");
                this.socketReady = true;
            } else {
                logger.error("Failed to initialize input stream");
                LoginController.getInstance().showErrorDialog("Failed to initialize input stream");
                close();
                return;
            }
            

        } catch (IOException e) {
            LoginController.getInstance().showErrorDialog("Could not connect to server");
            logger.error("Could not Connect");
            close();
            return;
        } catch (NullPointerException e) {
            LoginController.getInstance().showErrorDialog("Could not get output stream");
            logger.error("Could not get output stream");
            close();
            return;
        }

        try {
            logger.info("Sockets in and out ready!");
            
            while (socket.isConnected()) {
                Message message = null;
                message = (Message) this.input.readObject();

                if (message != null) {
                    logger.info("Message recieved:" + message.getType());
                    switch (message.getType()) {
                        case USER_MESSAGE:
                            chatCon.getInstance().addMessageToChatView(message);
                            break;
                        case SERVER:
                            chatCon.getInstance().addAsServer(message);
                            break;
                        case CONNECTED:
                            chatCon.getInstance().setUserListView(message);
                            break;
                        case DISCONNECTED:
                            chatCon.getInstance().setUserListView(message);
                            break;
                        case STATUS:
                            chatCon.getInstance().setUserListView(message);
                            break;
                        case S_LOGIN:
                            this.isValid = true;
                            this.username = message.getName();
                            logger.info("Successful login");
                            LoginController.getInstance().showChatScene();
                            break;

                        case S_REGISTER:
                            LoginController.getInstance().showInformationDialog(message.getMsg());
                            break;

                        case S_GET_FRIEND_REQUEST:
                            ArrayList<Conversation> requestList = new ArrayList<>(message.getConversationMap().values());
                            friendRequestCon.getInstance().setConversationListView(requestList);
                            break;

                        case S_UPDATE_CONVERSATION:
                            while (chatCon == null) {
                                try {
                                    logger.info("Waiting for chatCon to be initialized...");
                                    TimeUnit.SECONDS.sleep(1);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt(); // Restore interrupted status
                                    logger.error("Thread was interrupted", e);
                                }
                            }
                            chatCon.getInstance().setConversationListView(message);
                            this.conversationMap = message.getConversationMap();
                            break;

                        case S_CONVERSATION_CHAT:
                            // check if user is choosing this conversation
                            if (chatCon != null) {
                                Integer currentConversationID = chatCon.getCurrentTargetConversationID();
                                Integer targetConversationID = message.getTargetConversationID();

                                if (currentConversationID.equals(targetConversationID)) {
                                    chatCon.addMessageToChatView(message);
                                }
                            }
                            break;
                        case S_ERROR:
                            logger.info("Server promt an error with msg: " + message.getMsg());
                            LoginController.getInstance().showErrorDialog(message.getMsg());
                            break;
                        case S_SHOW_CONVERSATION_CHAT:
                        	logger.info("User"+this.username+"get context of conversation from server");
                        	chatCon.showContextOfConversation(message);
                        	break;
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void getMessageFromConversation(int conversationID)  throws IOException {
    	Message conversationNeeded = new Message();
    	conversationNeeded.setName(this.username);
    	conversationNeeded.setType(MessageType.C_SHOW_CONVERSATION_CHAT);
    	conversationNeeded.setTargetConversationID(conversationID);
        this.output.writeObject(conversationNeeded);
        this.output.flush();
    }
    
    public void sendMessageToConversation(int conversationID, String messageContext) throws IOException {
        Message createMessage = new Message();
        createMessage.setName(this.username);
        createMessage.setType(MessageType.C_CONVERSATION_CHAT);
        createMessage.setTargetConversationID(conversationID);
        createMessage.setMsg(messageContext);
        this.output.writeObject(createMessage);
        this.output.flush();
    }

    /* This method is used for sending a normal Message
 * @param msg - The message which the user generates
 */
    public void sendStatusUpdate(Status status) throws IOException {
        Message createMessage = new Message();
        createMessage.setName(username);
        createMessage.setType(MessageType.STATUS);
        createMessage.setStatus(status);
        createMessage.setPicture(picture);
        this.output.writeObject(createMessage);
        this.output.flush();
    }

    /* This method is used to validate a user (server will check pass from db) */
    public void login(String username, String password) throws IOException, ClassNotFoundException {
        logger.info("login() method enter");

        try {
            Message validateMessage = new Message();
            validateMessage.setName(username);
            validateMessage.setPassword(password);
            validateMessage.setType(MessageType.C_LOGIN);
            this.output.writeObject(validateMessage);
            this.output.flush();
            logger.debug("Sent validation message: " + validateMessage);
            
        } catch (IOException e) {
            logger.error("Exception in connect method: " + e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected exception in connect method: " + e.getMessage(), e);
            throw new RuntimeException(e); // Wrap unexpected exceptions in RuntimeException
        }
    }

    /* This method is used to validate a user registration(server will create new user in db) */
    public void register(String username, String password) throws IOException, ClassNotFoundException {
        logger.info("register() method enter");

        try {
            Message registerMessage = new Message();
            registerMessage.setName(username);
            registerMessage.setPassword(password);
            registerMessage.setType(MessageType.C_REGISTER);
            this.output.writeObject(registerMessage);
            this.output.flush();
            logger.debug("Sent register message: " + registerMessage);
            
        } catch (IOException e) {
            logger.error("Exception in connect method: " + e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected exception in connect method: " + e.getMessage(), e);
            throw new RuntimeException(e); // Wrap unexpected exceptions in RuntimeException
        }
    }

    /* This method is used to validate a user registration(server will create new user in db) */
    public void addFriend(String username) throws IOException, ClassNotFoundException {
        logger.info("addFriend() method enter");

        try {
            Message friendRequestMessage = new Message();
            friendRequestMessage.setName(username);
            friendRequestMessage.setType(MessageType.C_FRIEND_REQUEST);
            this.output.writeObject(friendRequestMessage);
            this.output.flush();
            logger.debug("Sent friendRequest message: " + friendRequestMessage);
            
        } catch (IOException e) {
            logger.error("Exception in connect method: " + e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected exception in connect method: " + e.getMessage(), e);
            throw new RuntimeException(e); // Wrap unexpected exceptions in RuntimeException
        }
    }

    public void createFriendShip(String username) throws IOException, ClassNotFoundException {
    	logger.info("createFriendShip() method enter");
    	try {
            Message friendShipMessage = new Message();
            friendShipMessage.setName(username);
            friendShipMessage.setType(MessageType.C_CREATE_FRIEND_SHIP);
            this.output.writeObject(friendShipMessage);
            this.output.flush();
            logger.debug("Create FriendShip: " + friendShipMessage);
            
        } catch (IOException e) {
            logger.error("Exception in connect method: " + e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected exception in connect method: " + e.getMessage(), e);
            throw new RuntimeException(e); // Wrap unexpected exceptions in RuntimeException
        }
    }
    
    public void getFriendRequest() throws IOException {
        try {
            Message msg = new Message();
            msg.setName(username);
            msg.setType(MessageType.C_GET_FRIEND_REQUEST);
            this.output.writeObject(msg);
            this.output.flush();
            
        } catch (IOException e) {
            logger.error("Exception in connect method: " + e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected exception in connect method: " + e.getMessage(), e);
            throw new RuntimeException(e); // Wrap unexpected exceptions in RuntimeException
        }

    }

    public void close() {
        try {
        if (this.input != null) {
            this.input.close();
        }
        if (this.is != null) {
            this.is.close();
        }
        if (this.output != null) {
            this.output.close();
        }
        if (this.os != null) {
            this.os.close();
        }
        if (this.socket != null) {
            this.socket.close();
        }
        } catch (IOException e) {
            logger.error("Failed to close resources", e);
        }
    }
}
