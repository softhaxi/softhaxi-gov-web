package com.softhaxi.marves.core.model.request;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

public class CalendarEventRequest {
    private String id;
    private String action;
    private String type;
    private String category;
    private String name;
    private @DateTimeFormat(iso = ISO.DATE) LocalDate date;


    public CalendarEventRequest() {
    }

    public CalendarEventRequest(String id, String action, String type, String category, String name, LocalDate date) {
        this.id = id;
        this.action = action;
        this.type = type;
        this.category = category;
        this.name = name;
        this.date = date;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAction() {
        return this.action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return this.category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getDate() {
        return this.date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public CalendarEventRequest id(String id) {
        setId(id);
        return this;
    }

    public CalendarEventRequest action(String action) {
        setAction(action);
        return this;
    }

    public CalendarEventRequest type(String type) {
        setType(type);
        return this;
    }

    public CalendarEventRequest category(String category) {
        setCategory(category);
        return this;
    }

    public CalendarEventRequest name(String name) {
        setName(name);
        return this;
    }

    public CalendarEventRequest date(LocalDate date) {
        setDate(date);
        return this;
    }

    @Override
    public String toString() {
        return "{" +
            " id='" + getId() + "'" +
            ", action='" + getAction() + "'" +
            ", type='" + getType() + "'" +
            ", category='" + getCategory() + "'" +
            ", name='" + getName() + "'" +
            ", date='" + getDate() + "'" +
            "}";
    }

}
