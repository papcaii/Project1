package com.messages;

import java.util.ArrayList;

public class Conversation {
	private int conversationId;
	private ArrayList<User> members;
	private boolean isGroup;
	private User groupMaster;
	
	public Conversation(int conversationId, ArrayList<User> members, boolean isGroup, User groupMaster) {
		super();
		this.conversationId=conversationId;
		this.isGroup = isGroup;
		this.members = members;
		this.groupMaster = groupMaster;
		this.members.add(groupMaster);
	}
	public ArrayList<User> getMembers() {
		return members;
	}
	public void setMembers(ArrayList<User> members) {
		this.members = members;
	}
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
	public int getConversationId() {
		return conversationId;
	}
	
	
}
