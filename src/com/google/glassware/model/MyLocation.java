package com.google.glassware.model;

import com.google.api.client.util.DateTime;
import com.google.api.services.mirror.model.Location;

import java.util.Date;

public class MyLocation {
    public double lat;
    public double lon;
    public double accuracy;
    public Date date;

    public MyLocation(Location location) {
        lat = location.getLatitude();
        lon = location.getLongitude();
        accuracy = location.getAccuracy();
        DateTime timestamp = location.getTimestamp();
        date = new Date(timestamp.getValue());
    }
}


