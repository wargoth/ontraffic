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

public class MapquestProvider implements TrafficServiceProvider {
    public static final String MAPQUEST_KEY = "Fmjtd%7Cluub2hu1n1%2Crn%3Do5-9utx1f";

    private static final Logger LOG = Logger.getLogger(MainServlet.class.getSimpleName());

    public StringBuilder getMapLink(Location location, List<NearLog> nearestLogs, HttpServletRequest reqest) {
        StringBuilder result = new StringBuilder();
        result.append("http://www.mapquestapi.com/staticmap/v4/getmap")
                .append("?size=640,360")
                .append("&type=map")
                .append("&pois=pcenter,").append(Utils.toStr(location));

        if (!nearestLogs.isEmpty()) {
            result.append("&xis=").append(WebUtil.buildUrl(reqest, "/static/images/transparent.png")).append(",").append(nearestLogs.size());

            for (NearLog log : nearestLogs) {
                LogRecord rec = log.getLogRecord();
                result.append(",,").append(Utils.toStr(rec)).append(",,,,");
            }
        } else {
            result.append("&center=").append(Utils.toStr(location))
                    .append("&zoom=13");
        }

        result.append("&imagetype=png")
                .append("&traffic=1&scalebar=false")
                .append("&key=").append(MAPQUEST_KEY);

        return result;
    }

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
        req.append("http://www.mapquestapi.com/traffic/v2/incidents?key=")
                .append(MAPQUEST_KEY)
                .append("&boundingBox=").append(Utils.toStr(bounds[0])).append(",").append(Utils.toStr(bounds[1]))
                .append("&filters=construction,incidents")
                .append("&inFormat=kvp&outFormat=json");

        LOG.info("Requesting traffic information from: " + req);

        URL url = new URL(req.toString());
        Gson gson = new Gson();

        TrafficResponse response = gson.fromJson(new InputStreamReader(url.openStream()), TrafficResponse.class);

        List<NearLog> result = new ArrayList<>();

        for (Incident incident : response.incidents) {
            NearLog nearLog = new NearLog();
            LogRecord logRecord = new LogRecord();

            nearLog.setLogRecord(logRecord);
            nearLog.setDistance(Utils.toMiles(Utils.getDistance(incident.lat, incident.lng, location.getLatitude(), location.getLongitude())));

            logRecord.setLat(incident.lat);
            logRecord.setLon(incident.lng);
            logRecord.setLocationDesc(incident.fullDesc);

            result.add(nearLog);
        }

        Collections.sort(result);

        if (result.isEmpty()) {
            return result;
        }

        return result.subList(0, Math.min(NotifyServlet.MAX_NEARBY_LOGS, result.size()));
    }

    public static class TrafficResponse {
        public List<Incident> incidents;
    }

    public static class Incident {
        public String fullDesc;
        public double lat;
        public double lng;
    }
}
