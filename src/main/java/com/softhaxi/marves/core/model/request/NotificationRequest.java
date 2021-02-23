package com.softhaxi.marves.core.model.request;

public class NotificationRequest {
    private String recipientGroup;
    private String division;
    private String subject;
    private String message;


    public NotificationRequest() {
    }

    public NotificationRequest(String recipientGroup, String division, String subject, String message) {
        this.recipientGroup = recipientGroup;
        this.division = division;
        this.subject = subject;
        this.message = message;
    }

    public String getRecipientGroup() {
        return this.recipientGroup;
    }

    public void setRecipientGroup(String recipientGroup) {
        this.recipientGroup = recipientGroup;
    }

    public String getDivision() {
        return this.division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public String getSubject() {
        return this.subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationRequest recipientGroup(String recipientGroup) {
        setRecipientGroup(recipientGroup);
        return this;
    }

    public NotificationRequest division(String division) {
        setDivision(division);
        return this;
    }

    public NotificationRequest subject(String subject) {
        setSubject(subject);
        return this;
    }

    public NotificationRequest message(String message) {
        setMessage(message);
        return this;
    }
    
    @Override
    public String toString() {
        return "{" +
            " recipientGroup='" + getRecipientGroup() + "'" +
            ", division='" + getDivision() + "'" +
            ", subject='" + getSubject() + "'" +
            ", message='" + getMessage() + "'" +
            "}";
    }

}
