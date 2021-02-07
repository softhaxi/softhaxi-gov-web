package com.softhaxi.marves.core.model.request;

import java.io.Serializable;
import java.util.Date;

public class DispensationRequest implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 8372001577458034742L;

    private String type;
    private String description;
    private Date startDate;
    private Date endDate;

    public DispensationRequest() {
    }

    public DispensationRequest(String type, String description, Date startDate, Date endDate) {
        this.type = type;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getStartDate() {
        return this.startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return this.endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public DispensationRequest type(String type) {
        setType(type);
        return this;
    }

    public DispensationRequest startDate(Date startDate) {
        setStartDate(startDate);
        return this;
    }

    public DispensationRequest endDate(Date endDate) {
        setEndDate(endDate);
        return this;
    }

    @Override
    public String toString() {
        return "{" +
            " type='" + getType() + "'" +
            ", startDate='" + getStartDate() + "'" +
            ", endDate='" + getEndDate() + "'" +
            "}";
    }
}
