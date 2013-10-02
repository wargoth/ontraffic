package com.yavalek.ontraffic;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.mirror.Mirror;
import com.google.api.services.mirror.model.Location;
import com.google.api.services.mirror.model.MenuItem;
import com.google.api.services.mirror.model.Notification;
import com.google.api.services.mirror.model.NotificationConfig;
import com.google.api.services.mirror.model.TimelineItem;
import com.google.api.services.mirror.model.UserAction;
import com.yavalek.ontraffic.model.NearLog;
import com.yavalek.ontraffic.model.UserLastLocation;
import com.yavalek.ontraffic.model.UserSettings;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.List;
import java.util.logging.Level;
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

        double speed = Utils.getSpeedKmph(lastLocation, newLocation);

        LOG.info("Current speed is " + speed);

        if (speed > SPEED_THRESHOLD || userSettings.isTestingAccount()) {
            onDriving(request, credential, location, speed, userSettings);
        }
    }

    protected void onDriving(HttpServletRequest req, Credential credential, Location location,
                             double speed, UserSettings userSettings) throws IOException {
        LOG.info("Driving detected");

        TrafficServiceProvider trafficProvider = getProvider(location);

        List<NearLog> nearestLogs = trafficProvider.getNearestLogs(location);

        StringBuilder read = new StringBuilder();
        StringBuilder html = new StringBuilder();

        html.append("<article class=\"photo\" style=\"left: 0px; visibility: visible;\">");
        html.append("<img src=\"").append(trafficProvider.getMapLink(location, nearestLogs, req)).append("\" width=\"100%\" height=\"100%\">");
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

    private TrafficServiceProvider getProvider(Location location) {
        try {
            String country = BingProvider.getCountry(location);
            if (country.equalsIgnoreCase("United States") || country.equalsIgnoreCase("Canada"))
                return new MapquestProvider();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }

        return new BingProvider();
    }

    private void populateSpeakableText(List<NearLog> nearestLogs, StringBuilder read) {
        if (nearestLogs.isEmpty()) {
            read.append("Traffic seems to be usual.");
            return;
        }
        for (NearLog logRecord : nearestLogs) {
            float miles = Utils.toMiles(logRecord.getDistance());
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
}
