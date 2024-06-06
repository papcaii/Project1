package com.messages;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class Message implements Serializable {

	
    private String sender;
    private String password;
    private MessageType type;
    private String msg;
    private int onlineCount;
    private ArrayList<User> userList;

	private String picturePath;
	private Status status;
	
	// Constructor
	public Message() {
    }
    


// Getter
    public String getPicture() {
        return picturePath;
    }

    public String getName() {
        return sender;
    }

    public String getPassword() {
        return password;
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

// Setter
    public void setUserlist(ArrayList<User> userList) {
        this.userList = userList;
    }

    public void setPicture(String picture) {
        this.picturePath = picture;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setType(MessageType type) {
        this.type = type;
    }
    
    public void setName(String name) {
        this.sender = name;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }


}
