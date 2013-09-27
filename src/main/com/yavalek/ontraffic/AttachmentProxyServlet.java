package com.yavalek.ontraffic;

import com.google.api.client.auth.oauth2.Credential;
import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Allows logged in users to view their timeline item attachments by proxying
 * their app engine session to their OAuth session.
 * 
 * @author Jenny Murphy - http://google.com/+JennyMurphy
 */
public class AttachmentProxyServlet extends HttpServlet {
  private static final Logger LOG = Logger.getLogger(AttachmentProxyServlet.class.getSimpleName());

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    String attachmentId = req.getParameter("attachment");
    String timelineItemId = req.getParameter("timelineItem");
    if (attachmentId == null || timelineItemId == null) {
      LOG.warning("attempted to load image attachment with missing IDs");
      resp.sendError(400);
    }
    // identify the viewing user
    Credential credential = AuthUtil.getCredential(req);

    // Get the content type
    String contentType =
        MirrorClient.getAttachmentContentType(credential, timelineItemId, attachmentId);

    // Get the attachment bytes
    InputStream attachmentInputStream =
        MirrorClient.getAttachmentInputStream(credential, timelineItemId, attachmentId);

    // Write it out
    resp.setContentType(contentType);
    ByteStreams.copy(attachmentInputStream, resp.getOutputStream());
  }
}
