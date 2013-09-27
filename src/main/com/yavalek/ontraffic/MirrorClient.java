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
package com.yavalek.ontraffic;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.mirror.Mirror;
import com.google.api.services.mirror.model.Attachment;
import com.google.api.services.mirror.model.Contact;
import com.google.api.services.mirror.model.ContactsListResponse;
import com.google.api.services.mirror.model.MenuItem;
import com.google.api.services.mirror.model.MenuValue;
import com.google.api.services.mirror.model.Subscription;
import com.google.api.services.mirror.model.SubscriptionsListResponse;
import com.google.api.services.mirror.model.TimelineItem;
import com.google.api.services.mirror.model.TimelineListResponse;
import com.google.common.io.ByteStreams;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A facade for easier access to basic API operations
 * 
 * @author Jenny Murphy - http://google.com/+JennyMurphy
 */
public class MirrorClient {
  private static final Logger LOG = Logger.getLogger(MirrorClient.class.getSimpleName());

  public static Mirror getMirror(Credential credential) {
    return new Mirror.Builder(new UrlFetchTransport(), new JacksonFactory(), credential)
        .setApplicationName(MainServlet.APPLICATION_NAME).build();
  }

  public static Contact insertContact(Credential credential, Contact contact) throws IOException {
    Mirror.Contacts contacts = getMirror(credential).contacts();
    return contacts.insert(contact).execute();
  }

  public static void deleteContact(Credential credential, String contactId) throws IOException {
    Mirror.Contacts contacts = getMirror(credential).contacts();
    contacts.delete(contactId).execute();
  }

  public static ContactsListResponse listContacts(Credential credential) throws IOException {
    Mirror.Contacts contacts = getMirror(credential).contacts();
    return contacts.list().execute();
  }

  public static Contact getContact(Credential credential, String id) throws IOException {
    try {
      Mirror.Contacts contacts = getMirror(credential).contacts();
      return contacts.get(id).execute();
    } catch (GoogleJsonResponseException e) {
      return null;
    }
  }


  public static TimelineListResponse listItems(Credential credential, long count)
      throws IOException {
    Mirror.Timeline timelineItems = getMirror(credential).timeline();
    Mirror.Timeline.List list = timelineItems.list();
    list.setMaxResults(count);
    return list.execute();
  }

    public static int cleanUpTimeline(Credential credential) throws IOException {
        Mirror.Timeline timelineItems = getMirror(credential).timeline();
        Mirror.Timeline.List list = timelineItems.list();

        TimelineListResponse execute = list.execute();

        class BatchCallback extends JsonBatchCallback<Void> {
            private int success = 0;

            @Override
            public void onFailure(GoogleJsonError error, HttpHeaders headers) throws IOException {
                LOG.info("Failed to delete item: " + error.getMessage());
            }

            @Override
            public void onSuccess(Void aVoid, HttpHeaders responseHeaders) throws IOException {
                ++success;
            }
        }

        BatchRequest batch = MirrorClient.getMirror(null).batch();
        BatchCallback callback = new BatchCallback();

        for (TimelineItem timelineItem : execute.getItems()) {
            getMirror(credential).timeline().delete(timelineItem.getId())
                    .queue(batch, callback);
        }

        batch.execute();

        return callback.success;
    }


  /**
   * Subscribes to notifications on the user's timeline.
   */
  public static Subscription insertSubscription(Credential credential, String callbackUrl,
      String userId, String collection) throws IOException {

    // Rewrite "appspot.com" to "Appspot.com" as a workaround for
    // http://b/6909300.
    callbackUrl = callbackUrl.replace("appspot.com", "Appspot.com");

    Subscription subscription = new Subscription();
    // Alternatively, subscribe to "locations"
    subscription.setCollection(collection);
    subscription.setCallbackUrl(callbackUrl);
    subscription.setUserToken(userId);

      Subscription execute = getMirror(credential).subscriptions().insert(subscription).execute();

      LOG.info("Timeline subscription " + subscription.getId() + " for user " + userId + " successfully inserted");

      return execute;
  }

  /**
   * Subscribes to notifications on the user's timeline.
   */
  public static void deleteSubscription(Credential credential, String id) throws IOException {
    getMirror(credential).subscriptions().delete(id).execute();
  }

  public static SubscriptionsListResponse listSubscriptions(Credential credential)
      throws IOException {
    Mirror.Subscriptions subscriptions = getMirror(credential).subscriptions();
    return subscriptions.list().execute();
  }

  /**
   * Inserts a simple timeline item.
   * 
   * @param credential the user's credential
   * @param item the item to insert
   */
  public static TimelineItem insertTimelineItem(Credential credential, TimelineItem item)
      throws IOException {
    return getMirror(credential).timeline().insert(item).execute();
  }

    public static List<MenuItem> getDefaultMenuItems(HttpServletRequest req) {
        List<MenuItem> menuItemList = new ArrayList<>();
        // Built in actions
        menuItemList.add(new MenuItem().setAction("DELETE"));

//        menuItemList.add(getSubscribeAction(req));

        return menuItemList;
    }

    private static MenuItem getSubscribeAction(HttpServletRequest req) {
        // Unsubscribe action
        List<MenuValue> menuValues = new ArrayList<>();
        menuValues.add(new MenuValue().setIconUrl(WebUtil.buildUrl(req, "/static/images/drill.png")).setDisplayName("Drill In"));
        return new MenuItem().setValues(menuValues).setId("drill").setAction("CUSTOM");
    }

    /**
   * Inserts an item with an attachment provided as a byte array.
   * 
   * @param credential the user's credential
   * @param item the item to insert
   * @param attachmentContentType the MIME type of the attachment (or null if
   *        none)
   * @param attachmentData data for the attachment (or null if none)
   */
  public static void insertTimelineItem(Credential credential, TimelineItem item,
      String attachmentContentType, byte[] attachmentData) throws IOException {
    Mirror.Timeline timeline = getMirror(credential).timeline();
    timeline.insert(item, new ByteArrayContent(attachmentContentType, attachmentData)).execute();

  }

  /**
   * Inserts an item with an attachment provided as an input stream.
   * 
   * @param credential the user's credential
   * @param item the item to insert
   * @param attachmentContentType the MIME type of the attachment (or null if
   *        none)
   * @param attachmentInputStream input stream for the attachment (or null if
   *        none)
   */
  public static void insertTimelineItem(Credential credential, TimelineItem item,
      String attachmentContentType, InputStream attachmentInputStream) throws IOException {
    insertTimelineItem(credential, item, attachmentContentType,
        ByteStreams.toByteArray(attachmentInputStream));
  }

  public static InputStream getAttachmentInputStream(Credential credential, String timelineItemId,
      String attachmentId) throws IOException {
    Mirror mirrorService = getMirror(credential);
    Mirror.Timeline.Attachments attachments = mirrorService.timeline().attachments();
    Attachment attachmentMetadata = attachments.get(timelineItemId, attachmentId).execute();
    HttpResponse resp =
        mirrorService.getRequestFactory()
            .buildGetRequest(new GenericUrl(attachmentMetadata.getContentUrl())).execute();
    return resp.getContent();
  }

  public static String getAttachmentContentType(Credential credential, String timelineItemId,
      String attachmentId) throws IOException {
    Mirror.Timeline.Attachments attachments = getMirror(credential).timeline().attachments();
    Attachment attachmentMetadata = attachments.get(timelineItemId, attachmentId).execute();
    return attachmentMetadata.getContentType();
  }
}
