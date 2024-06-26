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

public class Listener implements Runnable {

    private static final String HASCONNECTED = "has connected";

    private static String picture;
    private Socket socket;
    private String hostname;
    private int port;

    public String username;
    public String password;

    public ChatController chatCon;
    public AddFriendController addFriendCon;
    public GroupAddController groupAddCon;
    
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

    public void setGroupAddCon(GroupAddController groupChatCon) {
		this.groupAddCon = groupChatCon;
	}

	public void setAddFriendController(AddFriendController addFriendCon) {
        this.addFriendCon = addFriendCon;
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
                        case S_LOGIN:
                            this.isValid = true;
                            this.username = message.getName();
                            logger.info("Successful login");
                            LoginController.getInstance().showChatScene();
                            break;

                        case S_REGISTER:
                            LoginController.getInstance().showInformationDialog(message.getMsg());
                            break;
                        
                        // When user open friend request view, fr request will be loaded
                        case S_GET_FRIEND_REQUEST:
                            while (addFriendCon == null) {
                                try {
                                    logger.info("Waiting for addFriendCon to be initialized...");
                                    TimeUnit.SECONDS.sleep(1);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt(); // Restore interrupted status
                                    logger.error("Thread was interrupted", e);
                                }
                            }
                            ArrayList<Conversation> requestList = new ArrayList<>(message.getConversationMap().values());
                            addFriendCon.setUserListView(requestList);
                            break;

                        // update list of conversation
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
                            logger.info("Get conversation map " + message.getConversationMap());
                            chatCon.getInstance().setConversationListView(message);
                            // this.conversationMap = message.getConversationMap();
                            break;

                        // new message sent to client
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

                        // when server promt an error
                        case S_ERROR:
                            logger.info("Server promt an error with msg: " + message.getMsg());
                            LoginController.getInstance().showErrorDialog(message.getMsg());
                            break;

                        case S_NOTIFICATION:
                            logger.info("Notification from server: " + message.getMsg());
                            LoginController.getInstance().showInformationDialog(message.getMsg());
                            break;

                        case S_SHOW_CONVERSATION_PROPERTY:
                            chatCon.showConversationProperty(message);

                        // load message of a specific conversation
                        case S_SHOW_CONVERSATION_CHAT:
                        	logger.info("User" + this.username + "get context of conversation from server");
                        	chatCon.showContextOfConversation(message);
                        	break;
                        
                        // load conversation group, the values store in userMap
                        case S_UPDATE_CONVERSATION_GROUP:
                        	break;
                        	
                        // create new blank group
                        case S_CREATE_GROUP:
                        	break;
                        	
                        // show all request from group 
                        case S_GET_GROUP_REQUEST:
                        	while (groupAddCon == null) {
                                try {
                                    logger.info("Waiting for groupAddCon to be initialized...");
                                    TimeUnit.SECONDS.sleep(1);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt(); // Restore interrupted status
                                    logger.error("Thread was interrupted", e);
                                }
                            }
                            ArrayList<Conversation> requestGroupList = new ArrayList<>(message.getConversationMap().values());
                            groupAddCon.setUserListView(requestGroupList);
                        	break;
                        
                        // add people to  group
                        case S_ADD_PEOPLE_TO_GROUP:
                        	break;
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void getConversationProperty(int conversationID) throws IOException {
        Message message = new Message();
        message.setName(this.username);
        message.setType(MessageType.C_SHOW_CONVERSATION_PROPERTY);
        message.setTargetConversationID(conversationID);
        this.output.writeObject(message);
        this.output.flush();
    }

    public void getMessageFromConversation(int conversationID) throws IOException {
    	Message message = new Message();
    	message.setName(this.username);
    	message.setType(MessageType.C_SHOW_CONVERSATION_CHAT);
    	message.setTargetConversationID(conversationID);
        this.output.writeObject(message);
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

    public void sendUpdateConversationRequest() throws IOException {
        Message refreshMessage = new Message();
        refreshMessage.setType(MessageType.C_UPDATE_CONVERSATION);
        this.output.writeObject(refreshMessage);
        this.output.flush();
    }
    
    public void sendUpdateConversationGroupRequest() throws IOException {
        Message refreshMessage = new Message();
        refreshMessage.setType(MessageType.C_UPDATE_CONVERSATION_GROUP);
        this.output.writeObject(refreshMessage);
        this.output.flush();
    }

    /* This method is used for sending a normal Message
 * @param msg - The message which the user generates
 */
    public void sendStatusUpdate(Status status) throws IOException {
        Message createMessage = new Message();
        createMessage.setName(username);
        createMessage.setType(MessageType.C_UPDATE_STATUS);
        createMessage.setStatus(status);
        this.output.writeObject(createMessage);
        this.output.flush();
    }

    // send request to add people from group
    public void sendGroupRequest(String userTarget,Conversation group) throws IOException {
    	logger.info("Send request for "+userTarget+" to join group " + group.getConversationName());
    	try {
            Message validateMessage = new Message();
            validateMessage.setName(userTarget);
            validateMessage.setMsg(group.getConversationName());
            validateMessage.setTargetConversationID(group.getConversationID());
            validateMessage.setType(MessageType.C_SEND_GROUP_REQUEST);
            this.output.writeObject(validateMessage);
            this.output.flush();
            logger.debug("Sent validation message: " + validateMessage);
            
        } catch (IOException e) {
            logger.error("Exception in connect method: " + e.getMessage(), e);
            throw e;
        }
    }
    
    // send create new group to server 
    public void createGroup(String groupName) throws IOException{
    	logger.info("Group with name " + groupName + " has been request from " + this.username + " to created");
    	try {
            Message sendMessage = new Message();
            sendMessage.setName(this.username);
            sendMessage.setMsg(groupName);
            sendMessage.setType(MessageType.C_CREATE_GROUP);
            this.output.writeObject(sendMessage);
            this.output.flush();
            // logger.debug("Sent message: " + sendMessage);
            
        } catch (IOException e) {
            logger.error("Exception in connect method: " + e.getMessage(), e);
            throw e;
        }
    	
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

    // When user accept friend request
    public void createFriendShip(String username) throws IOException, ClassNotFoundException {
    	logger.info("createFriendShip() method enter");

        Message friendShipMessage = new Message();
        friendShipMessage.setName(username);
        friendShipMessage.setType(MessageType.C_CREATE_FRIEND_SHIP);
        this.output.writeObject(friendShipMessage);
        this.output.flush();
            
    }
    
    public void declineFriendRequest(String requestUserName) throws IOException {
        Message declineMessage = new Message();
        declineMessage.setName(requestUserName);
        declineMessage.setType(MessageType.C_DECLINE_FRIEND_REQUEST);
        this.output.writeObject(declineMessage);
        this.output.flush();
    }

    // get full group requests
    public void getGroupRequest() throws IOException {
        try {
            Message msg = new Message();
            msg.setName(username);
            msg.setType(MessageType.C_GET_GROUP_REQUEST);
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
    
    // get full friend request from another user  
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

    // user out group
    public void outGroup(int conversationID) throws IOException {
    	try {
            Message msg = new Message();
            msg.setName(this.username);
            msg.setTargetConversationID(conversationID);
            msg.setType(MessageType.C_REMOVE_FROM_GROUP);
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
    
    // user join group
    public void joinToGroup(String userName, Conversation group) throws IOException {
    	try {
            Message msg = new Message();
            msg.setName(username);
            msg.setTargetConversationID(group.getConversationID());
            msg.setMsg(group.getConversationName());
            msg.setType(MessageType.C_ADD_PEOPLE_TO_GROUP);
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
