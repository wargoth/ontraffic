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
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.services.mirror.model.Contact;
import com.google.api.services.mirror.model.Location;
import com.google.api.services.mirror.model.MenuItem;
import com.google.api.services.mirror.model.MenuValue;
import com.google.api.services.mirror.model.NotificationConfig;
import com.google.api.services.mirror.model.TimelineItem;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Handles POST requests from index.jsp
 *
 * @author Jenny Murphy - http://google.com/+JennyMurphy
 */
public class AdminServlet extends HttpServlet {

  /**
   * Private class to process batch request results.
   *
   * For more information, see
   * https://code.google.com/p/google-api-java-client/wiki/Batch.
   */
  private final class BatchCallback extends JsonBatchCallback<TimelineItem> {
    private int success = 0;
    private int failure = 0;

    @Override
    public void onSuccess(TimelineItem item, HttpHeaders headers) throws IOException {
      ++success;
    }

    @Override
    public void onFailure(GoogleJsonError error, HttpHeaders headers) throws IOException {
      ++failure;
      LOG.info("Failed to insert item: " + error.getMessage());
    }
  }

  private static final Logger LOG = Logger.getLogger(AdminServlet.class.getSimpleName());

  /**
   * Do stuff when buttons on index.jsp are clicked
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {

    String userId = AuthUtil.getUserId(req);
    Credential credential = AuthUtil.newAuthorizationCodeFlow().loadCredential(userId);
    String message = "";

    if (req.getParameter("operation").equals("insertSubscription")) {

      // subscribe (only works deployed to production)
        message = insertSubscribtion(req, userId, credential);

    } else if (req.getParameter("operation").equals("deleteSubscription")) {
        message = deleteSubscription(req, credential);


    } else if (req.getParameter("operation").equals("insertItem")) {
        message = insertItem(req, credential);

    } else if (req.getParameter("operation").equals("insertItemWithAction")) {
        message = insertItemWithAction(req, credential);

    } else if (req.getParameter("operation").equals("insertContact")) {
        message = insertContact(req, credential);

    } else if (req.getParameter("operation").equals("deleteContact")) {
        message = deleteContact(req, credential);


    } else if (req.getParameter("operation").equals("insertItemAllUsers")) {
        message = insertItemAllUsers(req);



    } else if (req.getParameter("operation").equals("insertOntraffic")) {
        message = "OK";

        Location location = new Location();
        location.setLatitude(37.778313);
        location.setLongitude(-122.419333);

        new NotifyServlet().onDriving(req, credential, location, 0);
    } else {
        message = nop(req);
    }
    WebUtil.setFlash(req, message);
    res.sendRedirect(WebUtil.buildUrl(req, "/"));
  }

    private String nop(HttpServletRequest req) {
        String message;
        String operation = req.getParameter("operation");
        LOG.warning("Unknown operation specified " + operation);
        message = "I don't know how to do that";
        return message;
    }

    private String insertItemAllUsers(HttpServletRequest req) throws IOException {
        String message;
        if (req.getServerName().contains("glass-java-starter-demo.appspot.com")) {
          message = "This function is disabled on the demo instance.";
        }

        // Insert a contact
        List<String> users = AuthUtil.getAllUserIds();
        LOG.info("found " + users.size() + " users");
        if (users.size() > 10) {
          // We wouldn't want you to run out of quota on your first day!
          message =
              "Total user count is " + users.size() + ". Aborting broadcast " + "to save your quota.";
        } else {
          TimelineItem allUsersItem = new TimelineItem();
          allUsersItem.setText("Hello Everyone!");

          BatchRequest batch = MirrorClient.getMirror(null).batch();
          BatchCallback callback = new BatchCallback();

          // TODO: add a picture of a cat
          for (String user : users) {
            Credential userCredential = AuthUtil.getCredential(user);
            MirrorClient.getMirror(userCredential).timeline().insert(allUsersItem)
                .queue(batch, callback);
          }

          batch.execute();
          message =
              "Successfully sent cards to " + callback.success + " users (" + callback.failure
                  + " failed).";
        }
        return message;
    }

    private String deleteContact(HttpServletRequest req, Credential credential) throws IOException {
        String message;// Insert a contact
        LOG.fine("Deleting contact Item");
        MirrorClient.deleteContact(credential, req.getParameter("id"));

        message = "Contact has been deleted.";
        return message;
    }

    private String insertContact(HttpServletRequest req, Credential credential) throws IOException {
        LOG.fine("Inserting contact Item");
        Contact contact = NewUserBootstrapper.getAppContact(req);
        MirrorClient.insertContact(credential, contact);

        return "Inserted contact: " + req.getParameter("name");
    }

    private String insertItemWithAction(HttpServletRequest req, Credential credential) throws IOException {
        String message;
        LOG.fine("Inserting Timeline Item");
        TimelineItem timelineItem = new TimelineItem();
        timelineItem.setText("Tell me what you had for lunch :)");

        List<MenuItem> menuItemList = new ArrayList<MenuItem>();
        // Built in actions
        menuItemList.add(new MenuItem().setAction("REPLY"));
        menuItemList.add(new MenuItem().setAction("READ_ALOUD"));

        // And custom actions
        List<MenuValue> menuValues = new ArrayList<MenuValue>();
        menuValues.add(new MenuValue().setIconUrl(WebUtil.buildUrl(req, "/static/images/drill.png"))
            .setDisplayName("Drill In"));
        menuItemList.add(new MenuItem().setValues(menuValues).setId("drill").setAction("CUSTOM"));

        timelineItem.setMenuItems(menuItemList);
        timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

        MirrorClient.insertTimelineItem(credential, timelineItem);

        message = "A timeline item with actions has been inserted.";
        return message;
    }

    private String insertItem(HttpServletRequest req, Credential credential) throws IOException {
        String message;
        LOG.fine("Inserting Timeline Item");
        TimelineItem timelineItem = new TimelineItem();

        if (req.getParameter("message") != null) {
          timelineItem.setText(req.getParameter("message"));
        }

        // Triggers an audible tone when the timeline item is received
        timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

        if (req.getParameter("imageUrl") != null) {
          // Attach an image, if we have one
          URL url = new URL(req.getParameter("imageUrl"));
          String contentType = req.getParameter("contentType");
          MirrorClient.insertTimelineItem(credential, timelineItem, contentType, url.openStream());
        } else {
          MirrorClient.insertTimelineItem(credential, timelineItem);
        }

        message = "A timeline item has been inserted.";
        return message;
    }

    private String deleteSubscription(HttpServletRequest req, Credential credential) throws IOException {
        String message;// subscribe (only works deployed to production)
        MirrorClient.deleteSubscription(credential, req.getParameter("subscriptionId"));

        message = "Application has been unsubscribed.";
        return message;
    }

    private String insertSubscribtion(HttpServletRequest req, String userId, Credential credential) throws IOException {
        String message;
        try {
          MirrorClient.insertSubscription(credential, WebUtil.buildUrl(req, "/notify"), userId,
                  req.getParameter("collection"));
          message = "Application is now subscribed to updates.";
        } catch (GoogleJsonResponseException e) {
          LOG.warning("Could not subscribe " + WebUtil.buildUrl(req, "/notify") + " because "
              + e.getDetails().toPrettyString());
          message = "Failed to subscribe. Check your log for details";
        }
        return message;
    }
}
