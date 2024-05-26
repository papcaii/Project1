package com.messages;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class Message implements Serializable {

    private String name;
    private MessageType type;
    private String msg;
    private int count;
    private ArrayList<User> list;
    private ArrayList<User> users;

	private byte[] voiceMsg;
	private String picture;
	public Message() {
    }
    private Status status;


// Getter
    public String getPicture() {
        return picture;
    }

    public String getName() {
        return name;
    }

    public String getMsg() {

        return msg;
    }

	public MessageType getType() {
        return type;
    }

    public int getOnlineCount(){
        return this.count;
    }


    public ArrayList<User> getUserlist() {
        return list;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public ArrayList<User> getUsers() {
        return users;
    }

// Setter
    public void setUserlist(HashMap<String, User> userList) {
        this.list = new ArrayList<>(userList.values());
    }

    public void setOnlineCount(int count){
        this.count = count;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public void setUsers(ArrayList<User> users) {
        this.users = users;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setVoiceMsg(byte[] voiceMsg) {
        this.voiceMsg = voiceMsg;
    }
    
    public void setType(MessageType type) {
        this.type = type;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }


}
