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
import com.google.api.services.mirror.model.MenuItem;
import com.google.api.services.mirror.model.NotificationConfig;
import com.google.api.services.mirror.model.Subscription;
import com.google.api.services.mirror.model.TimelineItem;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class NewUserBootstrapper {
    private static final Logger LOG = Logger.getLogger(NewUserBootstrapper.class.getSimpleName());

    public static void bootstrapNewUser(HttpServletRequest req, String userId) throws IOException {
        Credential credential = AuthUtil.newAuthorizationCodeFlow().loadCredential(userId);


        List<Subscription> subscriptions = MirrorClient.listSubscriptions(credential).getItems();
        boolean timelineSubscriptionExists = false;
        boolean locationSubscriptionExists = false;

        if (subscriptions != null) {
            for (Subscription subscription : subscriptions) {
                if (subscription.getId().equals("timeline")) {
                    timelineSubscriptionExists = true;
                }
                if (subscription.getId().equals("locations")) {
                    locationSubscriptionExists = true;
                }
            }
        }

        // check if the app have already been subscribed to all necessary notifications
        if (timelineSubscriptionExists && locationSubscriptionExists)
            return;

        try {
            // Subscribe to timeline updates
            MirrorClient.insertSubscription(credential, WebUtil.buildUrl(req, "/notify"), userId, "timeline");
            MirrorClient.insertSubscription(credential, WebUtil.buildUrl(req, "/notify"), userId, "locations");
        } catch (GoogleJsonResponseException e) {
            LOG.warning("Failed to create a subscription. Might be running on "
                    + "localhost. Details:" + e.getDetails().toPrettyString());
        }

        // Send welcome timeline item
        TimelineItem timelineItem = new TimelineItem();
        timelineItem.setText("Welcome to the Real-time traffic situation notification app for Glass");
        timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));

        List<MenuItem> menuItemList = MirrorClient.getDefaultMenuItems(req);

        timelineItem.setMenuItems(menuItemList);

        TimelineItem insertedItem = MirrorClient.insertTimelineItem(credential, timelineItem);
        LOG.info("Bootstrapper inserted welcome message " + insertedItem.getId() + " for user " + userId);
    }
}
