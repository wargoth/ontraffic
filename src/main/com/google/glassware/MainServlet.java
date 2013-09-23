package com.google.glassware;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.mirror.model.Contact;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(MainServlet.class.getSimpleName());
    public static final String APPLICATION_NAME = "Real-time traffic situation notification app for Glass";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {

        String userId = AuthUtil.getUserId(req);
        Credential credential = AuthUtil.newAuthorizationCodeFlow().loadCredential(userId);
        String message = "Invalid parameters provided";

        if (req.getParameter("operation").equals("signup")) {
            message = insertContact(req, credential);
            message = insertSubscriptions(req, userId, credential);
            message = "Successfully signed up";
        } else if (req.getParameter("operation").equals("cleanup")) {
            message = cleanAllMessages(credential);
        } else if (req.getParameter("operation").equals("leave")) {
            message = deleteContact(credential);
            message = deleteSubscriptions(credential);
            message = "Successfully left";
        }
        WebUtil.setFlash(req, message);
        res.sendRedirect(WebUtil.buildUrl(req, "/"));
    }

    private String deleteContact(Credential credential) throws IOException {
        String message;// Insert a contact
        LOG.fine("Deleting contact Item");
        MirrorClient.deleteContact(credential, NewUserBootstrapper.CONTACT_ID);

        message = "Contact has been deleted.";
        return message;
    }

    private String insertContact(HttpServletRequest req, Credential credential) throws IOException {
        LOG.fine("Inserting contact Item");
        Contact contact = NewUserBootstrapper.getAppContact(req);
        MirrorClient.insertContact(credential, contact);

        return "Inserted contact: " + contact.getDisplayName();
    }

    private String deleteSubscriptions(Credential credential) throws IOException {
        MirrorClient.deleteSubscription(credential, "timeline");
        MirrorClient.deleteSubscription(credential, "locations");

        return "Application has been unsubscribed.";
    }

    private String insertSubscriptions(HttpServletRequest req, String userId, Credential credential) throws IOException {
        try {
            MirrorClient.insertSubscription(credential, WebUtil.buildUrl(req, "/notify"), userId, "timeline");
            MirrorClient.insertSubscription(credential, WebUtil.buildUrl(req, "/notify"), userId, "locations");
        } catch (GoogleJsonResponseException ignore) {
            LOG.log(Level.SEVERE, ignore.getMessage(), ignore);
        }
        
        return "Application is now subscribed to updates.";
    }

    private String cleanAllMessages(Credential credential) throws IOException {
        int count = MirrorClient.cleanUpTimeline(credential);
        return String.format("Timeline successfully cleaned up. Removed %d timeline items", count);
    }
}
