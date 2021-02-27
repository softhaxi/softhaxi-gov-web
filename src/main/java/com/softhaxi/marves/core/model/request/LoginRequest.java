package com.softhaxi.marves.core.model.request;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Raja Sihombing
 * @since 1
 */
public class LoginRequest implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 8629797093319506417L;
    private String userid;
    private String password;
    private String oneSignalId;
    private boolean mockLocation;
    private String latitude;
    private String longitude;


    public LoginRequest() {
    }

    public LoginRequest(String userid, String password, String oneSignalId, boolean mockLocation, String latitude, String longitude) {
        this.userid = userid;
        this.password = password;
        this.oneSignalId = oneSignalId;
        this.mockLocation = mockLocation;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getUserid() {
        return this.userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOneSignalId() {
        return this.oneSignalId;
    }

    public void setOneSignalId(String oneSignalId) {
        this.oneSignalId = oneSignalId;
    }

    public boolean isMockLocation() {
        return this.mockLocation;
    }

    public boolean getMockLocation() {
        return this.mockLocation;
    }

    public void setMockLocation(boolean mockLocation) {
        this.mockLocation = mockLocation;
    }

    public String getLatitude() {
        return this.latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return this.longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public LoginRequest userid(String userid) {
        setUserid(userid);
        return this;
    }

    public LoginRequest password(String password) {
        setPassword(password);
        return this;
    }

    public LoginRequest oneSignalId(String oneSignalId) {
        setOneSignalId(oneSignalId);
        return this;
    }

    public LoginRequest mockLocation(boolean mockLocation) {
        setMockLocation(mockLocation);
        return this;
    }

    public LoginRequest latitude(String latitude) {
        setLatitude(latitude);
        return this;
    }

    public LoginRequest longitude(String longitude) {
        setLongitude(longitude);
        return this;
    }

    @Override
    public String toString() {
        return "{" +
            " userid='" + getUserid() + "'" +
            ", password='" + getPassword() + "'" +
            ", oneSignalId='" + getOneSignalId() + "'" +
            ", mockLocation='" + isMockLocation() + "'" +
            ", latitude='" + getLatitude() + "'" +
            ", longitude='" + getLongitude() + "'" +
            "}";
    }

}
