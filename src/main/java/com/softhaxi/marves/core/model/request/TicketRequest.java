package com.softhaxi.marves.core.model.request;

import java.io.Serializable;

public class TicketRequest implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -1120593187343756397L;
    // private String sendTo;
    // private String title;
    private String content;
    
    public TicketRequest() {
    }

    public TicketRequest(String content) {
        // this.sendTo = sendTo;
        // this.title = title;
        this.content = content;
    }

    // public String getSendTo() {
    //     return this.sendTo;
    // }

    // public void setSendTo(String sendTo) {
    //     this.sendTo = sendTo;
    // }

    // public String getTitle() {
    //     return this.title;
    // }

    // public void setTitle(String title) {
    //     this.title = title;
    // }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    // public TicketRequest sendTo(String sendTo) {
    //     this.sendTo = sendTo;
    //     return this;
    // }

    // public TicketRequest title(String title) {
    //     this.title = title;
    //     return this;
    // }

    public TicketRequest content(String content) {
        this.content = content;
        return this;
    }

    @Override
    public String toString() {
        return "{" +
            // " sendTo='" + getSendTo() + "'" +
            // ", title='" + getTitle() + "'" +
            " content='" + getContent() + "'" +
            "}";
    }

}
