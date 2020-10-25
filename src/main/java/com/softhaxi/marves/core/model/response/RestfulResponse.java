package com.softhaxi.marves.core.model.response;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Raja Sihombing
 * @since 1
 */
public class RestfulResponse implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 948098693693381496L;
    protected int code;
    protected String status;

    public RestfulResponse() {
    }

    public RestfulResponse(int code, String status) {
        this.code = code;
        this.status = status;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public RestfulResponse code(int code) {
        this.code = code;
        return this;
    }

    public RestfulResponse status(String status) {
        this.status = status;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof RestfulResponse)) {
            return false;
        }
        RestfulResponse response = (RestfulResponse) o;
        return Objects.equals(code, response.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, status);
    }

    @Override
    public String toString() {
        return "{" +
            " code='" + getCode() + "'" +
            ", status='" + getStatus() + "'" +
            "}";
    }

}
