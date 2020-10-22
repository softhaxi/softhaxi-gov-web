package com.softhaxi.marves.core.model.response;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Raja Sihombing
 * @since 1
 */
public class Response<T> implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 948098693693381496L;
    protected int code;
    protected String status;
    protected T data;


    public Response() {
    }

    public Response(int code, String status, T data) {
        this.code = code;
        this.status = status;
        this.data = data;
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

    public T getData() {
        return this.data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Response<T> code(int code) {
        this.code = code;
        return this;
    }

    public Response<T> status(String status) {
        this.status = status;
        return this;
    }

    public Response<T> data(T data) {
        this.data = data;
        return this;
    }
    
    @SuppressWarnings (value="unchecked")
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Response)) {
            return false;
        }
        Response<T> response = (Response<T>) o;
        return Objects.equals(code, response.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, status, data);
    }

    @Override
    public String toString() {
        return "{" +
            " code='" + getCode() + "'" +
            ", status='" + getStatus() + "'" +
            ", data='" + getData() + "'" +
            "}";
    }

}
