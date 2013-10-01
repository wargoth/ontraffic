package com.yavalek.ontraffic;

import com.google.api.client.http.GenericUrl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @author Jenny Murphy - http://google.com/+JennyMurphy
 */
public class WebUtil {
    public static final String APPSPOT_DOMAIN = "ontraffic.appspot.com";

    /**
     * Builds a URL relative to this app's root.
     */
    public static String buildUrl(HttpServletRequest req, String relativePath) {
        GenericUrl url = new GenericUrl(req.getRequestURL().toString());
        url.setRawPath(relativePath);
        return url.build();
    }

    /**
     * A simple flash implementation for text messages across requests
     *
     * @param request
     * @return
     */
    public static String getClearFlash(HttpServletRequest request) {
        HttpSession session = request.getSession();
        String flash = (String) session.getAttribute("flash");
        session.removeAttribute("flash");
        return flash;
    }

    public static void setFlash(HttpServletRequest request, String flash) {
        HttpSession session = request.getSession();
        session.setAttribute("flash", flash);
    }

    public static String httpsUrl(HttpServletRequest req, String relativePath) {
        GenericUrl url = new GenericUrl(req.getRequestURL().toString());
        url.setRawPath(relativePath);
        if (url.getPort() != 8080) { // test mode
            url.setHost(APPSPOT_DOMAIN);
            url.setScheme("https");
        }
        return url.build();
    }
}
