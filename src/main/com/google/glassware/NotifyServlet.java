/*
 * Copyright (C) 2013 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.glassware;

import com.beoui.geocell.GeocellManager;
import com.beoui.geocell.model.GeocellQuery;
import com.beoui.geocell.model.Point;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.mirror.Mirror;
import com.google.api.services.mirror.model.*;
import com.google.glassware.model.LogRecord;
import com.google.glassware.model.NearLog;
import com.google.glassware.model.UserLastLocation;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
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
    public static final int DISTANCE_THRESHOLD = 20; // in km
    public static final int MAX_NEARBY_LOGS = 5;

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

        UserLastLocation lastLocation = Database.getUserLastLocation(notification.getUserToken());
        Database.persist(newLocation);

        if (lastLocation == null) {
            LOG.info("Last location was not set, exiting");
            return;
        }

        double speed = getSpeedKmph(lastLocation, newLocation);


        LOG.info("Current speed is " + speed);

        if (speed > SPEED_THRESHOLD) {
            onDriving(credential, location, speed);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = AuthUtil.getUserId(req);
        Credential credential = AuthUtil.newAuthorizationCodeFlow().loadCredential(userId);

        Location location = new Location();
        location.setLatitude(37.778313);
        location.setLongitude(-122.419333);
        onDriving(credential, location, 0);
    }

    protected void onDriving(Credential credential, Location location, double speed) throws IOException {
        LOG.info("Driving detected");

        List<NearLog> nearestLogs = getNearestLogs(location);
        if (nearestLogs.isEmpty())
            return;

        StringBuilder read = new StringBuilder();

        StringBuilder html = new StringBuilder();

        makePhoto(location, nearestLogs, read, html);

        List<MenuItem> menuItemList = new ArrayList<>();
        // Built in actions
        menuItemList.add(new MenuItem().setAction("READ_ALOUD"));
        menuItemList.add(new MenuItem().setAction("DELETE"));

        TimelineItem timelineItem = new TimelineItem()
                .setHtml(html.toString())
                .setSpeakableText(read.toString())
                .setMenuItems(menuItemList)
                .setNotification(new NotificationConfig().setLevel("DEFAULT")).setLocation(location);
        MirrorClient.insertTimelineItem(credential, timelineItem);
    }


    private void makePhoto(Location location, List<NearLog> nearestLogs, StringBuilder read, StringBuilder html) throws UnsupportedEncodingException {
        html.append("<img src=\"")
                .append(getMapLink(location, nearestLogs, "640x360"))
                .append("\" width=\"100%\" height=\"100%\">");

        for (NearLog logRecord : nearestLogs) {
            float miles = toMiles(logRecord.getDistance());
            String loc = logRecord.getLogRecord().getLocation();
            String desc = logRecord.getLogRecord().getLocationDesc();

            read.append(miles)
                    .append(" miles away: ")
                    .append(loc)
                    .append(": ")
                    .append(desc)
                    .append(";");
        }
        html.append("<footer>")
                .append("<p>on-traffic</p>").append("</footer>").append("</article>");
    }

    private void makeFigure(Location location, List<NearLog> nearestLogs, StringBuilder read, StringBuilder html) throws UnsupportedEncodingException {
        html.append("<article>");

        html.append("<figure>").append("<img src=\"")
                .append(getMapLink(location, nearestLogs, "240x360"))
                .append("\">")
                .append("</figure>");

        html.append("<section>")
                .append("<ul class=\"text-x-small\">");

        char label = 'A';
        for (NearLog logRecord : nearestLogs) {
            float miles = toMiles(logRecord.getDistance());
            String loc = logRecord.getLogRecord().getLocation();
            String desc = logRecord.getLogRecord().getLocationDesc();


            read.append(miles)
                    .append(" miles away: ")
                    .append(loc)
                    .append(": ")
                    .append(desc)
                    .append(";");

            html.append("<li>")
                    .append("<span class=red>").append(Character.toString(label++)).append("</span> ")
                    .append("<span class=yellow>").append(miles).append("mi</span> ")
                    .append(loc)
                    .append(" ")
                    .append(desc)
                    .append("</li>");

        }
        html.append("</ul>" +
                "</section>" +
                "<footer>" +
                "<p>on-traffic</p>" +
                "</footer>" +
                "</article>");
    }

    private List<NearLog> getNearestLogs(Location location) {
        Point center = new Point(location.getLatitude(), location.getLongitude());


        PersistenceManager pm = PMF.get().getPersistenceManager();

        GeocellQuery baseQuery = new GeocellQuery();
        List<LogRecord> logRecords = GeocellManager.proximitySearch(center, MAX_NEARBY_LOGS, DISTANCE_THRESHOLD * 1000, LogRecord.class, baseQuery, pm);

        double aLat = location.getLatitude();
        double aLon = location.getLongitude();

        List<NearLog> result = new ArrayList<>();

        for (LogRecord logRecord : logRecords) {
            double distance = getDistance(aLat, aLon, logRecord.getLat(), logRecord.getLon());

            NearLog nearLog = new NearLog();
            nearLog.setDistance(distance);
            nearLog.setLogRecord(logRecord);
            result.add(nearLog);
        }

        Collections.sort(result);

        return result;
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
        final double R = 6371.0; // km or 3,959 miles

        double dLat = Math.toRadians(bLat - aLat);
        double dLon = Math.toRadians(bLon - aLon);

        double latA = Math.toRadians(aLat);
        double latB = Math.toRadians(bLat);
        double c = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0) +
                Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0) * Math.cos(latA) * Math.cos(latB);

        return R * 2.0 * Math.atan2(Math.sqrt(c), Math.sqrt(1.0 - c));
    }

    public StringBuilder getMapLink(Location location, List<NearLog> nearestLogs, String size) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        result.append("http://maps.googleapis.com/maps/api/staticmap?")
                .append("size=")
                .append(size)
                .append("&sensor=false")
                .append("&markers=")
                .append(encode("color:blue|" + toStr(location)));

        char label = 'A';
        for (NearLog log : nearestLogs) {
            LogRecord rec = log.getLogRecord();
            result.append("&markers=")
                    .append(encode("color:red|label:" + Character.toString(label++) + "|" + toStr(rec)));
        }

        result.append("style=").append(encode("feature:administrative|visibility:simplified"))
                .append("&style=").append(encode("feature:poi|visibility:simplified"))
                .append("&style=").append(encode("feature:road.local|visibility:simplified"))
                .append("&style=").append(encode("feature:transit.line|visibility:off"))
                .append("&style=").append(encode("invert_lightness:true"))
                .append("&style=").append(encode("feature:road.arterial|element:labels|visibility:simplified"));

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
