package com.messages;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class Message implements Serializable {

    private String name;
    private String password;
    private int targetConversationID;
    private MessageType type;
    private String msg;
    private ArrayList<User> userList;
    private HashMap<Integer, Conversation> conversationMap;
    private ArrayList <Message> context; 

	private String picturePath;
	private Status status;
	
	// Constructor
	public Message() {
    }
    


	// Getter
    public String getPicture() {
        return picturePath;
    }

    public ArrayList<Message> getContext() {
		return context;
	}



	public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public int getTargetConversationID() {
        return targetConversationID;
    }

    public String getMsg() {

        return msg;
    }

	public MessageType getType() {
        return type;
    }


    public ArrayList<User> getUserList() {
        return userList;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public HashMap<Integer, Conversation> getConversationMap() {
    	return conversationMap;
    }

// Setter
    public void setUserlist(ArrayList<User> userList) {
        this.userList = userList;
    }

    public void setPicture(String picture) {
        this.picturePath = picture;
    }
    
    public void setContext(ArrayList<Message> context) {
		this.context = context;
	}

	public void setStatus(Status status) {
        this.status = status;
    }

    public void setType(MessageType type) {
        this.type = type;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setTargetConversationID(int targetConversationID) {
        this.targetConversationID = targetConversationID;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
    
    public void setConversationMap(HashMap<Integer, Conversation> conversationMap) {
    	this.conversationMap = conversationMap;
	}
}

