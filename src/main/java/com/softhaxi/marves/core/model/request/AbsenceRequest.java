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
    private Date dateTime;
    private double latitude;
    private double longitude;
    private String code;
    private String title;
    private boolean isMockLocation;
    private String description;
    //private MultipartFile photo;

    public AbsenceRequest() {
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

    public Date getDateTime() {
        return this.dateTime;
    }

    public void setDateTime(Date dateTime) {
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

    public boolean isIsMockLocation() {
        return this.isMockLocation;
    }

    public boolean getIsMockLocation() {
        return this.isMockLocation;
    }

    public void setIsMockLocation(boolean isMockLocation) {
        this.isMockLocation = isMockLocation;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AbsenceRequest type(String type) {
        this.type = type;
        return this;
    }

    public AbsenceRequest action(String action) {
        this.action = action;
        return this;
    }

    public AbsenceRequest dateTime(Date dateTime) {
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

    public AbsenceRequest code(String code) {
        this.code = code;
        return this;
    }

    public AbsenceRequest title(String title) {
        this.title = title;
        return this;
    }

    public AbsenceRequest isMockLocation(boolean isMockLocation) {
        this.isMockLocation = isMockLocation;
        return this;
    }

    public AbsenceRequest description(String description) {
        this.description = description;
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
            ", code='" + getCode() + "'" +
            ", title='" + getTitle() + "'" +
            ", isMockLocation='" + isIsMockLocation() + "'" +
            ", description='" + getDescription() + "'" +
            "}";
    }

}
