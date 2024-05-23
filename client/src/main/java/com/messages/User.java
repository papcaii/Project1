package com.messages;

import java.io.Serializable;

/**
 * Created by Dominic on 01-May-16.
 */
public class User implements Serializable {

	String name;
    String picture;
    Status status;

    public String getName() {
        return name;
    }
    
    public String getPicture() {
        return picture;
    }

    public Status getStatus() {
        return status;
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


}
