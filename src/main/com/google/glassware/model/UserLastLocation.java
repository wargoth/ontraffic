package com.google.glassware.model;

import com.google.api.client.util.DateTime;
import com.google.api.services.mirror.model.Location;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import java.util.Date;

@PersistenceCapable
public class UserLastLocation {
    @PrimaryKey
    private String userToken;

    @Persistent
    private double lat;

    @Persistent
    private double lon;

    @Persistent
    private double accuracy;

    @Persistent
    private Date date;

    public UserLastLocation() {
    }

    public UserLastLocation(String userToken, Location location) {
        this.userToken = userToken;
        lat = location.getLatitude();
        lon = location.getLongitude();
        accuracy = location.getAccuracy();
        DateTime timestamp = location.getTimestamp();
        date = new Date(timestamp.getValue());
    }

    public String getUserToken() {
        return userToken;
    }

    public void setUserToken(String userToken) {
        this.userToken = userToken;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}


