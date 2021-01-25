package com.softhaxi.marves.core.model.request;

import java.io.Serializable;
import java.util.Date;

public class InvitationRequest implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -7377330212903413314L;
    
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


    public InvitationRequest() {
    }

    public InvitationRequest(String id, String code, String title, String category, Date startDate, Date endDate, String description, String startTime, String endTime, String invitee, String location, double latitude, double longitude) {
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

    public InvitationRequest id(String id) {
        setId(id);
        return this;
    }

    public InvitationRequest code(String code) {
        setCode(code);
        return this;
    }

    public InvitationRequest title(String title) {
        setTitle(title);
        return this;
    }

    public InvitationRequest category(String category) {
        setCategory(category);
        return this;
    }

    public InvitationRequest startDate(Date startDate) {
        setStartDate(startDate);
        return this;
    }

    public InvitationRequest endDate(Date endDate) {
        setEndDate(endDate);
        return this;
    }

    public InvitationRequest description(String description) {
        setDescription(description);
        return this;
    }

    public InvitationRequest startTime(String startTime) {
        setStartTime(startTime);
        return this;
    }

    public InvitationRequest endTime(String endTime) {
        setEndTime(endTime);
        return this;
    }

    public InvitationRequest invitee(String invitee) {
        setInvitee(invitee);
        return this;
    }

    public InvitationRequest location(String location) {
        setLocation(location);
        return this;
    }

    public InvitationRequest latitude(double latitude) {
        setLatitude(latitude);
        return this;
    }

    public InvitationRequest longitude(double longitude) {
        setLongitude(longitude);
        return this;
    }

    @Override
    public String toString() {
        return "{" +
            " code='" + getCode() + "'" +
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
            "}";
    }

}
