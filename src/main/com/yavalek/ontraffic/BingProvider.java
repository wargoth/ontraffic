package com.yavalek.ontraffic;

import com.google.api.services.mirror.model.Location;
import com.google.gson.Gson;
import com.yavalek.ontraffic.model.LogRecord;
import com.yavalek.ontraffic.model.NearLog;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class BingProvider implements TrafficServiceProvider {
    private static final Logger LOG = Logger.getLogger(MainServlet.class.getSimpleName());

    public static final String BING_KEY = "AhyUKYbb6FvwtAtQEZSuRNdYmRJoAD50j-s7Sr2h94snimHFb6cjs5h8yy7u3gZ3";

    public StringBuilder getMapLink(Location location, List<NearLog> nearestLogs, HttpServletRequest reqest) {
        StringBuilder result = new StringBuilder();
        if (!nearestLogs.isEmpty()) {
            result.append("http://dev.virtualearth.net/REST/v1/Imagery/Map/Road");
        } else {
            result.append("http://dev.virtualearth.net/REST/v1/Imagery/Map/Road/")
                    .append(Utils.toStr(location))
                    .append("/12");
        }

        result.append("?mapSize=640,360")
                .append("&pp=").append(Utils.toStr(location)).append(";22");

        for (NearLog log : nearestLogs) {
            LogRecord rec = log.getLogRecord();

            result.append("&pp=").append(Utils.toStr(rec)).append(";19");
        }

        result.append("&mapLayer=TrafficFlow")
                .append("&key=").append(BING_KEY);

        return result;
    }

    @Override
    public List<NearLog> getNearestLogs(Location location) {
        try {
            return getNearLogs(location);
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private List<NearLog> getNearLogs(Location location) throws IOException {
        GeoLocation loc = GeoLocation.fromDegrees(location.getLatitude(), location.getLongitude());

        GeoLocation[] bounds = loc.boundingCoordinates(NotifyServlet.DISTANCE_THRESHOLD, NotifyServlet.EARTH_R);

        StringBuilder req = new StringBuilder();
        req.append("http://dev.virtualearth.net/REST/v1/Traffic/Incidents/")
                .append(Utils.toStr(bounds[0])).append(",").append(Utils.toStr(bounds[1]))
                .append("?key=" + BING_KEY)
                .append("&severity=3,4")
                .append("&type=1,2,3,8,9,10,11");

        LOG.info("Requesting traffic information from: " + req);

        URL url = new URL(req.toString());
        Gson gson = new Gson();

        TrafficResponse response = gson.fromJson(new InputStreamReader(url.openStream()), TrafficResponse.class);

        List<NearLog> result = new ArrayList<>();

        for (Totals incident : response.resourceSets) {
            for (TrafficIncident resource : incident.resources) {
                NearLog nearLog = new NearLog();
                LogRecord logRecord = new LogRecord();

                nearLog.setLogRecord(logRecord);

                double lat = resource.point.coordinates[0];
                double lng = resource.point.coordinates[1];

                nearLog.setDistance(Utils.toMiles(Utils.getDistance(lat, lng, location.getLatitude(), location.getLongitude())));

                logRecord.setLat(lat);
                logRecord.setLon(lng);
                logRecord.setLocationDesc(resource.getFullDescription());

                result.add(nearLog);
            }
        }

        Collections.sort(result);

        if (result.isEmpty()) {
            return result;
        }

        return result.subList(0, Math.min(NotifyServlet.MAX_NEARBY_LOGS, result.size()));
    }

    public static class TrafficResponse {
        public List<Totals> resourceSets;

    }

    public static class Totals {
        public List<TrafficIncident> resources;
    }

    public static class TrafficIncident {
        public String description;
        public Point point;
        public int type;

        public String getFullDescription() {
            StringBuilder builder = new StringBuilder();

            switch (type) {
                case 1: // Accident
                    builder.append("Accident");
                    break;

                case 2: // Congestion
                    builder.append("Congestion");
                    break;

                case 3: // DisabledVehicle
                    builder.append("Stalled vehicle");
                    break;

                case 4: // MassTransit
                    break;

                case 5: // Miscellaneous
                    break;

                case 6: // OtherNews
                    break;

                case 7: // PlannedEvent
                    builder.append("Planned Event");
                    break;

                case 8: // RoadHazard
                    builder.append("Road Hazard");
                    break;

                case 9: // Construction
                    break;

                case 10: // Alert
                    builder.append("Alert");
                    break;

                case 11: // Weather
                    builder.append("Weather alert");
                    break;
            }

            return builder.append(" ").append(description).toString();
        }
    }

    public static class Point {
        double[] coordinates;
    }
}
