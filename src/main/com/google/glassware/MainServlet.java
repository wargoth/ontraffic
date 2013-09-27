package com.google.glassware;

import com.google.api.client.auth.oauth2.Credential;
import com.google.glassware.model.UserSettings;

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
        String message = "Invalid parameters provided";

        if (req.getParameter("operation").equals("enable")) {
            message = enableNotifications(userId);
        } else if (req.getParameter("operation").equals("disable")) {
            message = disableNotifications(userId);
        }
        WebUtil.setFlash(req, message);
        res.sendRedirect(WebUtil.buildUrl(req, "/profile/"));
    }

    private String disableNotifications(String userId) throws IOException {
        UserSettings userSettings = UserSettings.getUserSettings(userId);
        userSettings.setNotificationEnabled(false);

        Database.persist(userSettings);

        return "Notifications have been disabled";
    }

    private String enableNotifications(String userId) throws IOException {
        UserSettings userSettings = UserSettings.getUserSettings(userId);
        userSettings.setNotificationEnabled(true);

        Database.persist(userSettings);

        return "Notifications have been enabled";
    }

}
