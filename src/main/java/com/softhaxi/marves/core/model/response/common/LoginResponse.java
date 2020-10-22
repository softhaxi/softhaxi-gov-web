package com.softhaxi.marves.core.model.response.common;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * @author Raja Sihombing
 * @since 1
 */
public class LoginResponse implements Serializable{
    /**
     *
     */
    private static final long serialVersionUID = -6871275781582319591L;
    protected String type;
    protected String accessToken;
    protected Timestamp expired;


    public LoginResponse() {
    }

    public LoginResponse(String type, String accessToken, Timestamp expired) {
        this.type = type;
        this.accessToken = accessToken;
        this.expired = expired;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Timestamp getExpired() {
        return this.expired;
    }

    public void setExpired(Timestamp expired) {
        this.expired = expired;
    }

    public LoginResponse type(String type) {
        this.type = type;
        return this;
    }

    public LoginResponse accessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    public LoginResponse expired(Timestamp expired) {
        this.expired = expired;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof LoginResponse)) {
            return false;
        }
        LoginResponse loginResponse = (LoginResponse) o;
        return Objects.equals(type, loginResponse.type) && Objects.equals(accessToken, loginResponse.accessToken) && Objects.equals(expired, loginResponse.expired);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, accessToken, expired);
    }

    @Override
    public String toString() {
        return "{" +
            " type='" + getType() + "'" +
            ", accessToken='" + getAccessToken() + "'" +
            ", expired='" + getExpired() + "'" +
            "}";
    }
    
}
