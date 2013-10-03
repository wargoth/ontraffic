package com.yavalek.ontraffic;

import com.google.api.services.mirror.model.Location;
import com.yavalek.ontraffic.model.LogRecord;
import com.yavalek.ontraffic.model.UserLastLocation;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Utils {
    public static String toStr(Location location) {
        return location.getLatitude() + "," + location.getLongitude();
    }

    public static String toStr(LogRecord rec) {
        return rec.getLat() + "," + rec.getLon();
    }

    public static float toMiles(double km) {
        return Math.round(km * 10f / 1.60934f) / 10f;
    }

    public static float toKm(double km) {
        return Math.round(km * 10f) / 10f;
    }

    public static double getDistance(double aLat, double aLon, double bLat, double bLon) {
        GeoLocation a = GeoLocation.fromDegrees(aLat, aLon);
        GeoLocation b = GeoLocation.fromDegrees(bLat, bLon);
        return a.distanceTo(b, NotifyServlet.EARTH_R);
    }

    public static String toStr(GeoLocation bound) {
        return bound.getLatitudeInDegrees() + "," + bound.getLongitudeInDegrees();
    }

    public static int toMPH(double kmph) {
        return (int) (kmph / 1.60934);
    }

    public static double getSpeedKmph(UserLastLocation a, UserLastLocation b) {
        double distance = getDistanceKm(a, b);
        double hours = Math.abs(a.getDate().getTime() - b.getDate().getTime()) / 1000.0 / 3600.0;

        return distance / hours;
    }

    public static double getDistanceKm(UserLastLocation a, UserLastLocation b) {
        return getDistance(a.getLat(), a.getLon(), b.getLat(), b.getLon());
    }

    private static String encode(String s) throws UnsupportedEncodingException {
        return URLEncoder.encode(s, "UTF-8");
    }
}
