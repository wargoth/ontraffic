package com.google.glassware;

import com.google.glassware.model.UserLastLocation;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

public class LocationTest {
    @Test
    public void testGetDistance() {
        double distance = NotifyServlet.getDistanceKm(getA(), getB());
        System.out.println(distance);
    }

    private UserLastLocation getB() {
        UserLastLocation b = new UserLastLocation();
        b.setLat(37.4005515);
        b.setLon(-122.0997893);
        b.setDate(new Date());
        return b;
    }

    private UserLastLocation getA() {
        UserLastLocation a = new UserLastLocation();
        a.setLat(37.3949892);
        a.setLon(-122.0668457);
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.MINUTE, -10);
        a.setDate(instance.getTime());
        return a;
    }

    @Test
    public void testSpeedKmph() throws Exception {
        double speedKmph = NotifyServlet.getSpeedKmph(getA(), getB());
        System.out.println(speedKmph);
    }
}
