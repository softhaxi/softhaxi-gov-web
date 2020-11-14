package com.softhaxi.marves.core.model.request;

import java.io.Serializable;
import java.util.Date;

public class ChatRequest implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -1811199517525660475L;
    private String chatRoom;
    private String recipient;
    private String content;
    private Date dateTime;


    public ChatRequest() {
    }

    public ChatRequest(String chatRoom, String recipient, String content, Date dateTime) {
        this.chatRoom = chatRoom;
        this.recipient = recipient;
        this.content = content;
        this.dateTime = dateTime;
    }

    public String getChatRoom() {
        return this.chatRoom;
    }

    public void setChatRoom(String chatRoom) {
        this.chatRoom = chatRoom;
    }


    public String getRecipient() {
        return this.recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getDateTime() {
        return this.dateTime;
    }

    public void setDateTime(Date dateTime) {
        this.dateTime = dateTime;
    }

    public ChatRequest chatRoom(String chatRoom) {
        this.chatRoom = chatRoom;
        return this;
    }

    public ChatRequest recipient(String recipient) {
        this.recipient = recipient;
        return this;
    }

    public ChatRequest content(String content) {
        this.content = content;
        return this;
    }

    public ChatRequest dateTime(Date dateTime) {
        this.dateTime = dateTime;
        return this;
    }

    @Override
    public String toString() {
        return "{" +
            " chatRoom='" + getChatRoom() + "'" +
            ", recipient='" + getRecipient() + "'" +
            ", content='" + getContent() + "'" +
            ", dateTime='" + getDateTime() + "'" +
            "}";
    }

}
