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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.mirror.Mirror;
import com.google.api.services.mirror.model.*;
import com.google.common.collect.Lists;
import com.google.glassware.model.MyLocation;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.logging.Logger;

/**
 * Handles the notifications sent back from subscriptions
 *
 * @author Jenny Murphy - http://google.com/+JennyMurphy
 */
public class NotifyServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(MainServlet.class.getSimpleName());

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

        MyLocation newLocation = new MyLocation(location);

        Gson gson = new Gson();
        String locationJson = (String) request.getSession().getAttribute("lastLocation");
        MyLocation lastLocation = gson.fromJson(locationJson, MyLocation.class);
        request.getSession().setAttribute("lastLocation", gson.toJson(newLocation));

        if (lastLocation == null) {
            return;
        }

        double speed = getSpeedKmph(lastLocation, newLocation);


        LOG.info("Current speed is " + speed);

        if (speed > 30) {
            LOG.info("Driving detected");

            MirrorClient.insertTimelineItem(
                    credential,
                    new TimelineItem()
                            .setText("You are driving! Your speed: " + toMPH(speed))
                            .setNotification(new NotificationConfig().setLevel("DEFAULT")).setLocation(location));
        }

    }

    private int toMPH(double kmph) {
        return (int) (kmph * 1.60934);
    }

    private double getSpeedKmph(MyLocation a, MyLocation b) {
        final int R = 6371; // km or 3,959 miles
        double distance = Math.acos(Math.sin(a.lat) * Math.sin(b.lat) +
                Math.cos(a.lat) * Math.cos(b.lat) *
                        Math.cos(b.lon - a.lon)) * R;
        long time = Math.abs(a.date.getTime() - b.date.getTime());

        return distance / time;
    }
}
