package com.softhaxi.marves.core.model.request;

import java.io.Serializable;
import java.util.Date;

import org.springframework.web.multipart.MultipartFile;

public class InvitationRequest implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -7377330212903413314L;
    
    private String action;
    private String id;
    private String code;
    private String title;
    private String category = "INOFFICE";
    private Date startDate;
    private Date endDate;
    private String description;
    private String startTime;
    private String endTime;
    private String invitee;
    private String location;
    private double latitude;
    private double longitude;
    private MultipartFile attachment;


    public InvitationRequest() {
    }

    public InvitationRequest(String action, String id, String code, String title, String category, Date startDate, Date endDate, String description, String startTime, String endTime, String invitee, String location, double latitude, double longitude, MultipartFile attachment) {
        this.action = action;
        this.id = id;
        this.code = code;
        this.title = title;
        this.category = category;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.invitee = invitee;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.attachment = attachment;
    }

    public String getAction() {
        return this.action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return this.category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Date getStartDate() {
        return this.startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return this.endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartTime() {
        return this.startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return this.endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getInvitee() {
        return this.invitee;
    }

    public void setInvitee(String invitee) {
        this.invitee = invitee;
    }

    public String getLocation() {
        return this.location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public double getLatitude() {
        return this.latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return this.longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public MultipartFile getAttachment() {
        return this.attachment;
    }

    public void setAttachment(MultipartFile attachment) {
        this.attachment = attachment;
    }

    @Override
    public String toString() {
        return "{" +
            " action='" + getAction() + "'" +
            ", id='" + getId() + "'" +
            ", code='" + getCode() + "'" +
            ", title='" + getTitle() + "'" +
            ", category='" + getCategory() + "'" +
            ", startDate='" + getStartDate() + "'" +
            ", endDate='" + getEndDate() + "'" +
            ", description='" + getDescription() + "'" +
            ", startTime='" + getStartTime() + "'" +
            ", endTime='" + getEndTime() + "'" +
            ", invitee='" + getInvitee() + "'" +
            ", location='" + getLocation() + "'" +
            ", latitude='" + getLatitude() + "'" +
            ", longitude='" + getLongitude() + "'" +
            ", attachment='" + getAttachment() + "'" +
            "}";
    }

}
