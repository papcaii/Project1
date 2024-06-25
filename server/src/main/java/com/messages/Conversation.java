package com.messages;

import java.io.Serializable;
import java.util.ArrayList;

public class Conversation implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private int conversationId;
	private String conversationName;
	private boolean isGroup;
	private User groupMaster;
	private Status userStatus;
	
	public Conversation() {}

	public boolean isGroup() {
		return isGroup;
	}
	
	public void setGroup(boolean isGroup) {
		this.isGroup = isGroup;
	}

	public User getGroupMaster() {
		return groupMaster;
	}

	public void setGroupMaster(User groupMaster) {
		this.groupMaster = groupMaster;
	}

	public int getConversationID() {
		return conversationId;
	}
	
	public void setConversationID(int conversationId) {
		this.conversationId = conversationId;
	}
	
	public String getConversationName() {
		return conversationName;
	}
	public void setConversationName(String conversationName) {
		this.conversationName = conversationName;
	}
	
	public Status getUserStatus() {
		return userStatus;
	}
	
	public void setUserStatus(Status status) {
		this.userStatus = status;
	}
	
}
