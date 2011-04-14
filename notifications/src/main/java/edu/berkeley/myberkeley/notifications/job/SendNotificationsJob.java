/*

  * Licensed to the Sakai Foundation (SF) under one
  * or more contributor license agreements. See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership. The SF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License. You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied. See the License for the
  * specific language governing permissions and limitations under the License.

 */

package edu.berkeley.myberkeley.notifications.job;

import edu.berkeley.myberkeley.caldav.BadRequestException;
import edu.berkeley.myberkeley.caldav.CalDavConnector;
import edu.berkeley.myberkeley.caldav.CalDavConnectorProvider;
import edu.berkeley.myberkeley.caldav.CalDavException;
import edu.berkeley.myberkeley.caldav.CalendarURI;
import edu.berkeley.myberkeley.caldav.CalendarWrapper;
import edu.berkeley.myberkeley.notifications.Notification;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.scheduler.Job;
import org.apache.sling.commons.scheduler.JobContext;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.util.ISO8601Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SendNotificationsJob implements Job {

  private final Logger LOGGER = LoggerFactory.getLogger(SendNotificationsJob.class);

  final Repository repository;

  final NotificationEmailSender emailSender;

  final CalDavConnectorProvider calDavConnectorProvider;

  public SendNotificationsJob(Repository repository, NotificationEmailSender emailSender, CalDavConnectorProvider calDavConnectorProvider) {
    this.repository = repository;
    this.emailSender = emailSender;
    this.calDavConnectorProvider = calDavConnectorProvider;
  }

  public void execute(JobContext jobContext) {
    long startMillis = System.currentTimeMillis();

    Session adminSession = null;

    try {
      adminSession = repository.loginAdministrative();
      Iterable<Content> results = getQueuedNotifications(adminSession);
      processResults(results, adminSession.getContentManager());
    } catch (AccessDeniedException e) {
      LOGGER.error("SendNotificationsJob failed", e);
    } catch (StorageClientException e) {
      LOGGER.error("SendNotificationsJob failed", e);
    } catch (IOException e) {
      LOGGER.error("SendNotificationsJob failed", e);
    } finally {
      if (adminSession != null) {
        try {
          adminSession.logout();
        } catch (ClientPoolException e) {
          LOGGER.error("SendNotificationsJob failed to log out of admin session", e);
        }
      }
      long endMillis = System.currentTimeMillis();
      LOGGER.info("SendNotificationsJob executed in {} ms ", (endMillis - startMillis));
    }
  }

  private Iterable<Content> getQueuedNotifications(Session adminSession) throws StorageClientException, AccessDeniedException {
    ContentManager cm = adminSession.getContentManager();
    Map<String, Object> props = new HashMap<String, Object>();
    props.put("sakai:messagebox", Notification.MESSAGEBOX.queue.toString());
    props.put("sling:resourceType", Notification.RESOURCETYPE);
    return cm.find(props);
  }

  private void processResults(Iterable<Content> results, ContentManager contentManager) throws IOException {
    Date now = new Date();
    int resultCount = 0;
    int eligibleCount = 0;
    for (Content result : results) {
      resultCount++;
      if (eligibleForSending(result, now)) {
        eligibleCount++;
        LOGGER.debug("The time has come to send notification at path " + result.getPath());
        sendNotification(result, contentManager);
      }
    }
    LOGGER.info("Found " + resultCount + " results, of which " + eligibleCount + " were eligible for sending");
  }

  private boolean eligibleForSending(Content result, Date now) {
    Object sendDateValue = result.getProperty(Notification.JSON_PROPERTIES.sendDate.toString());
    Date sendDate = new ISO8601Date(sendDateValue.toString()).getTime();
    Notification.MESSAGEBOX box = Notification.MESSAGEBOX.valueOf((String) result.getProperty(Notification.JSON_PROPERTIES.messageBox.toString()));
    Notification.SEND_STATE sendState = Notification.SEND_STATE.valueOf((String) result.getProperty(Notification.JSON_PROPERTIES.sendState.toString()));
    return Notification.MESSAGEBOX.queue.equals(box) && Notification.SEND_STATE.pending.equals(sendState) && now.compareTo(sendDate) >= 0;
  }

  private void sendNotification(Content result, ContentManager contentManager) throws IOException {

    boolean success = false;
    Notification notification;

    try {
      notification = new Notification(result);
    } catch (JSONException e) {
      LOGGER.error("Notification at path " + result.getPath() + " has invalid JSON for calendarWrapper", e);
      return;
    } catch (CalDavException e) {
      LOGGER.error("Notification at path " + result.getPath() + " has invalid calendar data", e);
      return;
    }

    JSONObject recipientToCalendarURIMap = notification.getRecipientToCalendarURIMap();

    try {
      // save notification in bedework server
      // TODO get list of users based on dynamicListID and loop over them -- one connector per user
      String userID = "vbede";

      boolean needsCalendarEntry;
      boolean needsEmail = notification.getEmailMessageID() == null;
      try {
        needsCalendarEntry = recipientToCalendarURIMap.getJSONObject(userID) == null;
      } catch (JSONException ignored) {
        needsCalendarEntry = true;
      }

      if (needsCalendarEntry) {
        CalDavConnector connector = this.calDavConnectorProvider.getCalDavConnector();
        notification.getWrapper().generateNewUID();
        CalendarURI uri = connector.putCalendar(notification.getWrapper().getCalendar(), userID);
        recipientToCalendarURIMap.put(userID, uri.toJSON());
      }

      // send email
      if (needsEmail) {
        String messageID = this.emailSender.send(notification, Arrays.asList("904715"));
        if (messageID != null) {
          result.setProperty(Notification.JSON_PROPERTIES.emailMessageID.toString(), messageID);
        }
      }

      success = true;
      LOGGER.info("Successfully sent notification; local path " + result.getPath() + "; recipientToCalendarURIMap = "
              + recipientToCalendarURIMap.toString(2));

    } catch (JSONException e) {
      LOGGER.error("Notification at path " + result.getPath() + " has invalid JSON for calendarWrapper", e);
    } catch (BadRequestException e) {
      LOGGER.error("Got bad request from CalDav server trying to post notification at path " + result.getPath(), e);
    } catch (CalDavException e) {
      LOGGER.error("Notification at path " + result.getPath() + " has invalid calendar data", e);
    }

    result.setProperty(Notification.JSON_PROPERTIES.recipientToCalendarURIMap.toString(), recipientToCalendarURIMap.toString());

    // mark the notification as archived in our repo if all went well
    if (success) {
      result.setProperty(Notification.JSON_PROPERTIES.messageBox.toString(), Notification.MESSAGEBOX.archive.toString());
      result.setProperty(Notification.JSON_PROPERTIES.sendState.toString(), Notification.SEND_STATE.sent.toString());
    }

    try {
      contentManager.update(result);
    } catch (AccessDeniedException e) {
      LOGGER.error("Got access denied saving notification at path " + result.getPath(), e);
    } catch (StorageClientException e) {
      LOGGER.error("Got storage client exception saving notification at path " + result.getPath(), e);
    }

  }

}
