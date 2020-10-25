package com.softhaxi.marves.core.model.request;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;

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
    private String payload;
    private boolean isMockLocation;
    private String info;
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

    public String getPayload() {
        return this.payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
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

    public String getInfo() {
        return this.info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    // public MultipartFile getPhoto() {
    //     return photo;
    // }

    // public void setPhoto(MultipartFile photo) {
    //     this.photo = photo;
    // }

    public AbsenceRequest type(String type) {
        this.type = type;
        return this;
    }

    public AbsenceRequest action(String action) {
        this.action = action;
        return this;
    }

    public AbsenceRequest dateTime(Timestamp dateTime) {
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

    public AbsenceRequest payload(String payload) {
        this.payload = payload;
        return this;
    }

    public AbsenceRequest isMockLocation(boolean isMockLocation) {
        this.isMockLocation = isMockLocation;
        return this;
    }

    public AbsenceRequest info(String info) {
        this.info = info;
        return this;
    }

    // public AbsenceRequest photo(MultipartFile photo) {
    //     this.photo = photo;
    //     return this;
    // }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AbsenceRequest)) {
            return false;
        }
        AbsenceRequest absenceRequest = (AbsenceRequest) o;
        return Objects.equals(type, absenceRequest.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, action, dateTime, latitude, longitude, payload, isMockLocation, info);
    }

    @Override
    public String toString() {
        return "{" +
            " type='" + getType() + "'" +
            ", action='" + getAction() + "'" +
            ", dateTime='" + getDateTime() + "'" +
            ", latitude='" + getLatitude() + "'" +
            ", longitude='" + getLongitude() + "'" +
            ", payload='" + getPayload() + "'" +
            ", isMockLocation='" + isIsMockLocation() + "'" +
            ", info='" + getInfo() + "'" +
            // ", photo='" + getPhoto() + "'" +
            "}";
    }

}
