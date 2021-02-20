package com.softhaxi.marves.core.model.request;

import java.io.Serializable;

public class TicketRequest implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -1120593187343756397L;
    // private String sendTo;
    // private String title;
    private String id;
    private String content;
    private String status;
    
    

    public TicketRequest() {
    }

    public TicketRequest(String id, String content, String status) {
        this.id = id;
        this.content = content;
        this.status = status;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public TicketRequest id(String id) {
        setId(id);
        return this;
    }

    public TicketRequest content(String content) {
        setContent(content);
        return this;
    }

    public TicketRequest status(String status) {
        setStatus(status);
        return this;
    }

    @Override
    public String toString() {
        return "{" +
            " id='" + getId() + "'" +
            ", content='" + getContent() + "'" +
            ", status='" + getStatus() + "'" +
            "}";
    }

}
