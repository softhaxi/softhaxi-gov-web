package com.softhaxi.marves.core.model.request;

import java.io.Serializable;

/**
 * UserRequest
 */
public class UserRequest implements Serializable {

    private String action;
    private String id;
    private String roleId;
    private String oneSignalId;


    public UserRequest() {
    }

    public UserRequest(String action, String id, String roleId, String oneSignalId) {
        this.action = action;
        this.id = id;
        this.roleId = roleId;
        this.oneSignalId = oneSignalId;
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

    public String getRoleId() {
        return this.roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getOneSignalId() {
        return this.oneSignalId;
    }

    public void setOneSignalId(String oneSignalId) {
        this.oneSignalId = oneSignalId;
    }

    @Override
    public String toString() {
        return "{" +
            " action='" + getAction() + "'" +
            ", id='" + getId() + "'" +
            ", roleId='" + getRoleId() + "'" +
            ", oneSignalId='" + getOneSignalId() + "'" +
            "}";
    }
    
    
}