package com.model;

import java.io.Serializable;
import java.util.ArrayList;

public class User implements Serializable {

	int ID;
	String name;
    String picture;
    Status status;
    ArrayList <Conversation> conversations;

    public int getID() {
		return this.ID;
	}

	public String getName() {
        return name;
    }
    
    public String getPicture() {
        return picture;
    }

    public Status getStatus() {
        return status;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
    
    public ArrayList<Conversation> getConversations() {
		return conversations;
	}

	public void addConversation(Conversation con) {
    	this.conversations.add(con);
    }

}
