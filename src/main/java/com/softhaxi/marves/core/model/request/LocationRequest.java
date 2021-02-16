package com.softhaxi.marves.core.model.request;

import java.util.Date;

public class LocationRequest {
    
    private Date dateTime;
    private double latitude;
    private double longitude;
    private boolean isMockLocation;


    public LocationRequest() {
    }

    public LocationRequest(Date dateTime, double latitude, double longitude, boolean isMockLocation) {
        this.dateTime = dateTime;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isMockLocation = isMockLocation;
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

    public boolean isMockLocation() {
        return this.isMockLocation;
    }

    public void setIsMockLocation(boolean isMockLocation) {
        this.isMockLocation = isMockLocation;
    }

    public LocationRequest dateTime(Date dateTime) {
        setDateTime(dateTime);
        return this;
    }

    public LocationRequest latitude(double latitude) {
        setLatitude(latitude);
        return this;
    }

    public LocationRequest longitude(double longitude) {
        setLongitude(longitude);
        return this;
    }

    public LocationRequest isMockLocation(boolean isMockLocation) {
        setIsMockLocation(isMockLocation);
        return this;
    }

    @Override
    public String toString() {
        return "{" +
            " dateTime='" + getDateTime() + "'" +
            ", latitude='" + getLatitude() + "'" +
            ", longitude='" + getLongitude() + "'" +
            ", isMockLocation='" + isMockLocation() + "'" +
            "}";
    }

}
