package com.softhaxi.marves.core.model.response;

import java.util.Map;
import java.util.Objects;

/**
 * @author Raja Sihombing
 * @since 1
 */
public class GeneralResponse extends RestfulResponse {
    /**
     *
     */
    private static final long serialVersionUID = -6871275781582319591L;
    protected Object data;


    public GeneralResponse() {
    }

    public GeneralResponse(int code, String status, Object data) {
        super(code, status);
        this.data = data;
    }


    public GeneralResponse(Object data) {
        this.data = data;
    }

    public Object getData() {
        return this.data;
    }

    public void setData(Map<String,Object> data) {
        this.data = data;
    }

    public GeneralResponse data(Map<String,Object> data) {
        this.data = data;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof GeneralResponse)) {
            return false;
        }
        GeneralResponse response = (GeneralResponse) o;
        return Objects.equals(data, response.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        return "{" +
            " data='" + getData() + "'" +
            "}";
    }
    
}
