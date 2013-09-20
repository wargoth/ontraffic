package com.google.glassware;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.mirror.model.Contact;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

public class MainServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(MainServlet.class.getSimpleName());
    public static final String APPLICATION_NAME = "Real-time traffic situation notification app for Glass";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {

        String userId = AuthUtil.getUserId(req);
        Credential credential = AuthUtil.newAuthorizationCodeFlow().loadCredential(userId);
        String message = null;

        if (req.getParameter("operation").equals("signup")) {
            message = insertContact(req, credential);
            message = insertSubscription(req, userId, credential);
            message = "Successfully signed up";
        } else if (req.getParameter("operation").equals("cleanup")) {
            message = cleanAllMessages(req, credential);
        } else if (req.getParameter("operation").equals("leave")) {
            message = deleteContact(req, credential);
            message = deleteSubscription(req, credential);
            message = "Successfully left";
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

    private String deleteContact(HttpServletRequest req, Credential credential) throws IOException {
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

        return "Inserted contact: " + req.getParameter("name");
    }

    private String deleteSubscription(HttpServletRequest req, Credential credential) throws IOException {
        MirrorClient.deleteSubscription(credential, req.getParameter("subscriptionId"));

        return "Application has been unsubscribed.";
    }

    private String insertSubscription(HttpServletRequest req, String userId, Credential credential) throws IOException {
        String message;
        MirrorClient.insertSubscription(credential, WebUtil.buildUrl(req, "/notify"), userId,
                req.getParameter("collection"));
        message = "Application is now subscribed to updates.";
        return message;
    }

    private String cleanAllMessages(HttpServletRequest req, Credential credential) throws IOException {
        int count = MirrorClient.cleanUpTimeline(credential);
        return String.format("Timeline successfully cleaned up. Removed %d timeline items", count);
    }
}
