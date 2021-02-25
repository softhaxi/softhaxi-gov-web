package com.softhaxi.marves.core.model.request;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Raja Sihombing
 * @since 1
 */
public class AbsenceRequest implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 442484045745715890L;
    private String type;
    private String action;
    private String dateTime;
    private double latitude;
    private double longitude;
    private boolean isMockLocation;
    private String code;
    private String referenceId;
    private String location;
    private String organizer;
    private String title;
    private String description;
    private String startTime;
    private String endTime;


    public AbsenceRequest() {
    }

    public AbsenceRequest(String type, String action, String dateTime, double latitude, double longitude, boolean isMockLocation, String code, String referenceId, String location, String organizer, String title, String description, String startTime, String endTime) {
        this.type = type;
        this.action = action;
        this.dateTime = dateTime;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isMockLocation = isMockLocation;
        this.code = code;
        this.referenceId = referenceId;
        this.location = location;
        this.organizer = organizer;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAction() {
        return this.action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDateTime() {
        return this.dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
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

    public boolean isIsMockLocation() {
        return this.isMockLocation;
    }

    public boolean getIsMockLocation() {
        return this.isMockLocation;
    }

    public void setIsMockLocation(boolean isMockLocation) {
        this.isMockLocation = isMockLocation;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getReferenceId() {
        return this.referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getLocation() {
        return this.location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getOrganizer() {
        return this.organizer;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public AbsenceRequest type(String type) {
        this.type = type;
        return this;
    }

    public AbsenceRequest action(String action) {
        this.action = action;
        return this;
    }

    public AbsenceRequest dateTime(String dateTime) {
        this.dateTime = dateTime;
        return this;
    }

    public AbsenceRequest latitude(double latitude) {
        this.latitude = latitude;
        return this;
    }

    public AbsenceRequest longitude(double longitude) {
        this.longitude = longitude;
        return this;
    }

    public AbsenceRequest isMockLocation(boolean isMockLocation) {
        this.isMockLocation = isMockLocation;
        return this;
    }

    public AbsenceRequest code(String code) {
        this.code = code;
        return this;
    }

    public AbsenceRequest referenceId(String referenceId) {
        this.referenceId = referenceId;
        return this;
    }

    public AbsenceRequest location(String location) {
        this.location = location;
        return this;
    }

    public AbsenceRequest organizer(String organizer) {
        this.organizer = organizer;
        return this;
    }

    public AbsenceRequest title(String title) {
        this.title = title;
        return this;
    }

    public AbsenceRequest description(String description) {
        this.description = description;
        return this;
    }

    public AbsenceRequest startTime(String startTime) {
        this.startTime = startTime;
        return this;
    }

    @Override
    public String toString() {
        return "{" +
            " type='" + getType() + "'" +
            ", action='" + getAction() + "'" +
            ", dateTime='" + getDateTime() + "'" +
            ", latitude='" + getLatitude() + "'" +
            ", longitude='" + getLongitude() + "'" +
            ", isMockLocation='" + isIsMockLocation() + "'" +
            ", code='" + getCode() + "'" +
            ", referenceId='" + getReferenceId() + "'" +
            ", location='" + getLocation() + "'" +
            ", organizer='" + getOrganizer() + "'" +
            ", title='" + getTitle() + "'" +
            ", description='" + getDescription() + "'" +
            ", startTime='" + getStartTime() + "'" +
            ", endTime='" + getEndTime() + "'" +
            "}";
    }

}
