package com.yavalek.ontraffic;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.mirror.Mirror;
import com.google.api.services.mirror.model.*;
import com.google.gson.Gson;
import com.yavalek.ontraffic.model.LogRecord;
import com.yavalek.ontraffic.model.NearLog;
import com.yavalek.ontraffic.model.UserLastLocation;
import com.yavalek.ontraffic.model.UserSettings;
import com.yavalek.ontraffic.model.mapquest.Incident;
import com.yavalek.ontraffic.model.mapquest.TrafficResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Handles the notifications sent back from subscriptions
 *
 * @author Jenny Murphy - http://google.com/+JennyMurphy
 */
public class NotifyServlet extends HttpServlet {
    private static final long serialVersionUID = 7257039357957674961L;

    private static final Logger LOG = Logger.getLogger(MainServlet.class.getSimpleName());
    public static final int SPEED_THRESHOLD = 20; // in km/h
    public static final int DISTANCE_THRESHOLD = 10; // in km
    public static final int MAX_NEARBY_LOGS = 5;
    public static final String MAPQUEST_KEY = "Fmjtd%7Cluub2hu1n1%2Crn%3Do5-9utx1f";
    public static final double EARTH_R = 6371.0;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Respond with OK and status 200 in a timely fashion to prevent redelivery
        response.setContentType("text/html");
        Writer writer = response.getWriter();
        writer.append("OK");
        writer.close();

        // Get the notification object from the request body (into a string so we
        // can log it)
        BufferedReader notificationReader =
                new BufferedReader(new InputStreamReader(request.getInputStream()));
        String notificationString = "";

        // Count the lines as a very basic way to prevent Denial of Service attacks
        int lines = 0;
        while (notificationReader.ready()) {
            notificationString += notificationReader.readLine();
            lines++;

            // No notification would ever be this long. Something is very wrong.
            if (lines > 1000) {
                throw new IOException("Attempted to parse notification payload that was unexpectedly long.");
            }
        }

        LOG.info("got raw notification " + notificationString);

        JsonFactory jsonFactory = new JacksonFactory();

        // If logging the payload is not as important, use
        // jacksonFactory.fromInputStream instead.
        Notification notification = jsonFactory.fromString(notificationString, Notification.class);

        LOG.info("Got a notification with ID: " + notification.getItemId());

        // Figure out the impacted user and get their credentials for API calls
        String userId = notification.getUserToken();
        Credential credential = AuthUtil.getCredential(userId);
        Mirror mirrorClient = MirrorClient.getMirror(credential);


        if (notification.getCollection().equals("locations")) {
            processLocations(notification, credential, request, response);
        } else if (notification.getCollection().equals("timeline")) {
            processTimeline(notification, credential, mirrorClient);

        }
    }

    private void processTimeline(Notification notification, Credential credential, Mirror mirrorClient) throws IOException {
        // Get the impacted timeline item
        TimelineItem timelineItem = mirrorClient.timeline().get(notification.getItemId()).execute();
        LOG.info("Notification impacted timeline item with ID: " + timelineItem.getId());

        // If it was a share, and contains a photo, bounce it back to the user.
        if (notification.getUserActions().contains(new UserAction().setType("SHARE"))
                && timelineItem.getAttachments() != null && timelineItem.getAttachments().size() > 0) {
            LOG.info("It was a share of a photo. Sending the photo back to the user.");

            // Get the first attachment
            String attachmentId = timelineItem.getAttachments().get(0).getId();
            LOG.info("Found attachment with ID " + attachmentId);

            // Get the attachment content
            InputStream stream =
                    MirrorClient.getAttachmentInputStream(credential, timelineItem.getId(), attachmentId);

            // Create a new timeline item with the attachment
            TimelineItem echoPhotoItem = new TimelineItem();
            echoPhotoItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));
            echoPhotoItem.setText("Echoing your shared photo");

            MirrorClient.insertTimelineItem(credential, echoPhotoItem, "image/jpeg", stream);

        } else {
            LOG.warning("I don't know what to do with this notification, so I'm ignoring it.");
        }
    }

    private void processLocations(Notification notification, Credential credential, HttpServletRequest request, HttpServletResponse response) throws IOException {
        LOG.info("Notification of updated location");
        Mirror glass = MirrorClient.getMirror(credential);
        // item id is usually 'latest'
        Location location = glass.locations().get(notification.getItemId()).execute();
        LOG.info("Current location is " + location.getLatitude() + ", " + location.getLongitude());

        UserLastLocation newLocation = new UserLastLocation(notification.getUserToken(), location);

        UserLastLocation lastLocation = UserLastLocation.getUserLastLocation(notification.getUserToken());
        Database.persist(newLocation);

        if (lastLocation == null) {
            LOG.info("Last location was not set, exiting");
            return;
        }

        UserSettings userSettings = UserSettings.getUserSettings(notification.getUserToken());

        if (!userSettings.isNotificationEnabled()) {
            LOG.info("Notification disabled for the current user. Exiting");
            return;
        }

        double speed = getSpeedKmph(lastLocation, newLocation);

        LOG.info("Current speed is " + speed);

        if (speed > SPEED_THRESHOLD || userSettings.isTestingAccount()) {
            onDriving(request, credential, location, speed, userSettings);
        }
    }

    protected void onDriving(HttpServletRequest req, Credential credential, Location location,
                             double speed, UserSettings userSettings) throws IOException {
        LOG.info("Driving detected");

        List<NearLog> nearestLogs = getNearestLogs(location);

        StringBuilder read = new StringBuilder();
        StringBuilder html = new StringBuilder();

        html.append("<article class=\"photo\" style=\"left: 0px; visibility: visible;\">");
        html.append("<img src=\"").append(getMapLink(location, nearestLogs, req)).append("\" width=\"100%\" height=\"100%\">");
        html.append("<footer>")
                .append("<p>on-traffic</p>")
                .append("</footer>")
                .append("</article>");

        populateSpeakableText(nearestLogs, read);

        List<MenuItem> menuItemList = MirrorClient.getDefaultMenuItems(req);
        menuItemList.add(0, new MenuItem().setAction("READ_ALOUD"));

        TimelineItem timelineItem = new TimelineItem()
                .setHtml(html.toString())
                .setSpeakableText(read.toString())
                .setSpeakableType("Traffic report")
                .setMenuItems(menuItemList)
                .setNotification(new NotificationConfig().setLevel("DEFAULT"))
                .setLocation(location);
        insertOrUpdate(credential, userSettings, timelineItem);
    }

    private void populateSpeakableText(List<NearLog> nearestLogs, StringBuilder read) {
        if (nearestLogs.isEmpty()) {
            read.append("Traffic seems to be usual.");
            return;
        }
        for (NearLog logRecord : nearestLogs) {
            float miles = toMiles(logRecord.getDistance());
            String desc = logRecord.getLogRecord().getLocationDesc();

            read.append(miles)
                    .append(" miles away: ")
                    .append(desc)
                    .append(";");
        }
    }

    private void insertOrUpdate(Credential credential, UserSettings userSettings, TimelineItem timelineItem) throws IOException {
        if (userSettings.getLastNotificationId() == null) {
            TimelineItem inserted = MirrorClient.insertTimelineItem(credential, timelineItem);
            userSettings.setLastNotificationId(inserted.getId());
            Database.persist(userSettings);
        } else {
            try {
                MirrorClient.updateTimelineItem(credential, userSettings.getLastNotificationId(), timelineItem);
            } catch (GoogleJsonResponseException e) {
                LOG.info("Card seems to be deleted, inserting a new one");

                TimelineItem inserted = MirrorClient.insertTimelineItem(credential, timelineItem);
                userSettings.setLastNotificationId(inserted.getId());
                Database.persist(userSettings);
            }
        }
    }

    private List<NearLog> getNearestLogs(Location location) throws IOException {
        GeoLocation loc = GeoLocation.fromDegrees(location.getLatitude(), location.getLongitude());

        GeoLocation[] bounds = loc.boundingCoordinates(DISTANCE_THRESHOLD, EARTH_R);

        StringBuilder req = new StringBuilder();
        req.append("http://www.mapquestapi.com/traffic/v2/incidents?key=")
                .append(MAPQUEST_KEY)
                .append("&boundingBox=").append(toStr(bounds[0])).append(",").append(toStr(bounds[1]))
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
            nearLog.setDistance(toMiles(getDistance(incident.lat, incident.lng, location.getLatitude(), location.getLongitude())));

            logRecord.setLat(incident.lat);
            logRecord.setLon(incident.lng);
            logRecord.setLocationDesc(incident.fullDesc);

            result.add(nearLog);
        }

        Collections.sort(result);

        if (result.isEmpty()) {
            return result;
        }

        return result.subList(0, Math.min(MAX_NEARBY_LOGS, result.size()));
    }

    private String toStr(GeoLocation bound) {
        return bound.getLatitudeInDegrees() + "," + bound.getLongitudeInDegrees();
    }

    private int toMPH(double kmph) {
        return (int) (kmph / 1.60934);
    }

    private float toMiles(double km) {
        return Math.round(km * 10f / 1.60934f) / 10f;
    }

    static double getSpeedKmph(UserLastLocation a, UserLastLocation b) {
        double distance = getDistanceKm(a, b);
        double hours = Math.abs(a.getDate().getTime() - b.getDate().getTime()) / 1000.0 / 3600.0;

        return distance / hours;
    }

    static double getDistanceKm(UserLastLocation a, UserLastLocation b) {
        return getDistance(a.getLat(), a.getLon(), b.getLat(), b.getLon());
    }

    private static double getDistance(double aLat, double aLon, double bLat, double bLon) {
        GeoLocation a = GeoLocation.fromDegrees(aLat, aLon);
        GeoLocation b = GeoLocation.fromDegrees(bLat, bLon);
        return a.distanceTo(b, EARTH_R);
    }

    public StringBuilder getMapLink(Location location, List<NearLog> nearestLogs, HttpServletRequest reqest) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        result.append("http://www.mapquestapi.com/staticmap/v4/getmap")
                .append("?size=640,360")
                .append("&type=map")
                .append("&pois=pcenter,").append(toStr(location));

        if (!nearestLogs.isEmpty()) {
            result.append("&xis=").append(WebUtil.buildUrl(reqest, "/static/images/transparent.png")).append(",").append(nearestLogs.size());

            for (NearLog log : nearestLogs) {
                LogRecord rec = log.getLogRecord();
                result.append(",,").append(toStr(rec)).append(",,,,");
            }
        } else {
            result.append("&center=").append(toStr(location))
                    .append("&zoom=13");
        }

        result.append("&imagetype=png")
                .append("&traffic=1&scalebar=false")
                .append("&key=").append(MAPQUEST_KEY);

        return result;
    }

    private String toStr(LogRecord rec) {
        return rec.getLat() + "," + rec.getLon();
    }

    private String encode(String s) throws UnsupportedEncodingException {
        return URLEncoder.encode(s, "UTF-8");
    }

    private String toStr(Location location) {
        return location.getLatitude() + "," + location.getLongitude();
    }
}
