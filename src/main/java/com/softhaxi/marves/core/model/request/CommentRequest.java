package com.softhaxi.marves.core.model.request;

import java.io.Serializable;

public class CommentRequest implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 7003658079977323389L;
    private String content;
    private String reference;


    public CommentRequest() {
    }

    public CommentRequest(String content, String reference) {
        this.content = content;
        this.reference = reference;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getReference() {
        return this.reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    @Override
    public String toString() {
        return "{" +
            " content='" + getContent() + "'" +
            ", reference='" + getReference() + "'" +
            "}";
    }

}
