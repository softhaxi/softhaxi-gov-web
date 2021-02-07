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

    public LoginRequest() {
    }

    public LoginRequest(String userid, String password, String oneSignalId) {
        this.userid = userid;
        this.password = password;
        this.oneSignalId = oneSignalId;
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
        return oneSignalId;
    }

    public void setOneSignalId(String oneSignalId) {
        this.oneSignalId = oneSignalId;
    }

    public LoginRequest userid(String userid) {
        this.userid = userid;
        return this;
    }

    public LoginRequest password(String password) {
        this.password = password;
        return this;
    }

    public LoginRequest oneSignalId(String oneSignalId) {
        this.oneSignalId = oneSignalId;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof LoginRequest)) {
            return false;
        }
        LoginRequest loginRequest = (LoginRequest) o;
        return Objects.equals(userid, loginRequest.userid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userid, password);
    }

    @Override
    public String toString() {
        return "{" +
            " userID='" + getUserid() + "'" +
            ", password='" + getPassword() + "'" +
            "}";
    }

}
