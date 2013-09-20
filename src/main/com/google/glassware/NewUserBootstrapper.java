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
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.mirror.model.*;
import com.google.common.collect.Lists;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class NewUserBootstrapper {
    public static final String CONTACT_NAME = "Real-time traffic situation app";
    public static final String CONTACT_ID = "ontraffic.appspot.com";
    private static final Logger LOG = Logger.getLogger(NewUserBootstrapper.class.getSimpleName());

  public static void bootstrapNewUser(HttpServletRequest req, String userId) throws IOException {
    Credential credential = AuthUtil.newAuthorizationCodeFlow().loadCredential(userId);

      Contact existingContact = MirrorClient.getContact(credential, CONTACT_ID);
      if(existingContact != null)
          return;

      // Create contact
      Contact starterProjectContact = getAppContact(req);
    Contact insertedContact = MirrorClient.insertContact(credential, starterProjectContact);
    LOG.info("Bootstrapper inserted contact " + insertedContact.getId() + " for user " + userId);

    try {
      // Subscribe to timeline updates
      Subscription subscription =
          MirrorClient.insertSubscription(credential, WebUtil.buildUrl(req, "/notify"), userId,
              "timeline");
      LOG.info("Bootstrapper inserted timeline subscription " + subscription.getId() + " for user " + userId);

        MirrorClient.insertSubscription(credential, WebUtil.buildUrl(req, "/notify"), userId, "locations");

        LOG.info("Bootstrapper inserted locations subscription " + subscription.getId() + " for user " + userId);
    } catch (GoogleJsonResponseException e) {
      LOG.warning("Failed to create a subscription. Might be running on "
          + "localhost. Details:" + e.getDetails().toPrettyString());
    }

    // Send welcome timeline item
    TimelineItem timelineItem = new TimelineItem();
    timelineItem.setText("Welcome to the Real-time traffic situation notification app for Glass");
    timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

      List<MenuItem> menuItemList = new ArrayList<MenuItem>();
      menuItemList.add(new MenuItem().setAction("DELETE"));

      timelineItem.setMenuItems(menuItemList);

      TimelineItem insertedItem = MirrorClient.insertTimelineItem(credential, timelineItem);
    LOG.info("Bootstrapper inserted welcome message " + insertedItem.getId() + " for user "
        + userId);
  }

    public static Contact getAppContact(HttpServletRequest req) {
        Contact starterProjectContact = new Contact();
        starterProjectContact.setId(CONTACT_ID);
        starterProjectContact.setDisplayName(CONTACT_NAME);
        starterProjectContact.setImageUrls(Lists.newArrayList(WebUtil.buildUrl(req, "/static/images/chipotle-tube-640x360.jpg")));
        return starterProjectContact;
    }
}
