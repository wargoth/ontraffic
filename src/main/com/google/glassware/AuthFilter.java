package com.google.glassware;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * A filter which ensures that prevents unauthenticated users from accessing the
 * web app
 *
 * @author Jenny Murphy - http://google.com/+JennyMurphy
 */
public class AuthFilter implements Filter {
    private static final Logger LOG = Logger.getLogger(MainServlet.class.getSimpleName());

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        if (!(response instanceof HttpServletResponse) || !(request instanceof HttpServletRequest)) {
            LOG.warning("Unexpected non HTTP servlet response. Proceeding anyway.");
            filterChain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (httpRequest.getRequestURI().startsWith("/cron/")) {
            LOG.info("Skipping auth check for cron tasks");
            filterChain.doFilter(request, response);
            return;
        }

        // Redirect to https when on App Engine since subscriptions only work over
        // https
        if (httpRequest.getServerName().contains("appspot.com")
                && httpRequest.getScheme().equals("http")) {

            LOG.info("Redirecting to https");

            httpResponse.sendRedirect(httpRequest.getRequestURL().toString()
                    .replaceFirst("http", "https"));
            return;
        }

        // Are we in the middle of an auth flow? IF so skip check.
        if (httpRequest.getRequestURI().equals("/oauth2callback")) {
            LOG.info("Skipping auth check during auth flow");
            filterChain.doFilter(request, response);
            return;
        }

        // Is this a robot visit to the notify servlet? If so skip check
        if (httpRequest.getRequestURI().equals("/notify")) {
            LOG.info("Skipping auth check for notify servlet");
            filterChain.doFilter(request, response);
            return;
        }

        LOG.fine("Checking to see if anyone is logged in");
        if (AuthUtil.getUserId(httpRequest) == null
                || AuthUtil.getCredential(AuthUtil.getUserId(httpRequest)) == null
                || AuthUtil.getCredential(AuthUtil.getUserId(httpRequest)).getAccessToken() == null) {
            // redirect to auth flow
            httpResponse.sendRedirect(WebUtil.buildUrl(httpRequest, "/oauth2callback"));
            return;
        }

        // Things checked out OK :)
        filterChain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
