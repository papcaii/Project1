package com.client.chatwindow;

import com.client.login.LoginController;
import com.messages.Message;
import com.messages.MessageType;
import com.messages.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

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
    
    private InputStream is;
    private OutputStream os;
    private ObjectInputStream input;
    private ObjectOutputStream output;

    private boolean socketReady;
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

    public boolean isReady() {
        return this.socketReady;
    }

    public void run() {
        try {
            socket = new Socket(this.hostname, this.port);
            logger.info("Connection accepted " + socket.getInetAddress() + ":" + socket.getPort());
            
            // create input output stream
            this.os = socket.getOutputStream();
            this.output = new ObjectOutputStream(os);

            this.is = socket.getInputStream();
            this.input = new ObjectInputStream(is);
            this.output.flush();

            if (this.input != null) {
                logger.info("Input stream ready");
            } else {
                logger.error("Failed to initialize input stream");
                return;
            }
            this.socketReady = true;

        } catch (IOException e) {
            LoginController.getInstance().showErrorDialog("Could not connect to server");
            logger.error("Could not Connect");
        } catch (NullPointerException e) {
            LoginController.getInstance().showErrorDialog("Could not get output stream");
            logger.error("Could not get output stream");
        }

        try {
            logger.info("Sockets in and out ready!");
            
            while (socket.isConnected()) {
                Message message = null;
                message = (Message) this.input.readObject();

                if (message != null) {
                    logger.debug("Message recieved:" + message.getMsg() + " MessageType:" + message.getType() + "Name:" + message.getName());
                    switch (message.getType()) {
                        case USER_MESSAGE:
                            chatCon.getInstance().addToChat(message);
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
                        case ACCEPTED:
                            this.isValid = true;
                            this.username = message.getName();
                            logger.info("Successful login");
                            LoginController.getInstance().showChatScene();
                            break;
                        case DECLINED:
                            LoginController.getInstance().showErrorDialog(message.getMsg());
                            break;
                        case REGISTER_SUCCESS:
                            LoginController.getInstance().showErrorDialog(message.getMsg());
                            break;
                        case UPDATE_USER:
                            chatCon.getInstance().setUserListView(message);
                            break;
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /* This method is used for sending a normal Message
     * @param msg - The message which the user generates
     */
    public void send(String msg) throws IOException {
        Message createMessage = new Message();
        createMessage.setName(this.username);
        // createMessage.setTarget(conversationID);
        createMessage.setType(MessageType.USER_MESSAGE);
        createMessage.setMsg(msg);
        createMessage.setPicture(picture);
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
            validateMessage.setType(MessageType.LOGIN);
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
            registerMessage.setType(MessageType.REGISTER);
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
}
