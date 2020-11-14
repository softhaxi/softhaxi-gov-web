package com.softhaxi.marves.core.model.response;

import java.util.Objects;

/**
 * @author Raja Sihombing
 * @since 1
 */
public class ErrorResponse extends RestfulResponse {

    /**
     *
     */
    private static final long serialVersionUID = -2106425652429431828L;
    protected Object error;


    public ErrorResponse() {
    }

    public ErrorResponse(int code, String status, Object error) {
        super(code, status);
        this.error = error;
    }

    public Object getError() {
        return this.error;
    }

    public void setError(Object error) {
        this.error = error;
    }

    public ErrorResponse error(Object error) {
        this.error = error;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ErrorResponse)) {
            return false;
        }
        ErrorResponse errorResponse = (ErrorResponse) o;
        return Objects.equals(error, errorResponse.error);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(error);
    }

    @Override
    public String toString() {
        return "{" +
            " error='" + getError() + "'" +
            "}";
    }

}
