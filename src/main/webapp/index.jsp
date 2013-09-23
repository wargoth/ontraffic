<%@ page import="com.google.api.client.auth.oauth2.Credential" %>
<%@ page import="com.google.api.services.mirror.model.TimelineItem" %>
<%@ page import="com.google.glassware.MirrorClient" %>
<%@ page import="com.google.glassware.WebUtil" %>
<%@ page import="com.google.glassware.model.UserSettings" %>
<%@ page import="java.util.List" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<!doctype html>
<%
    String userId = com.google.glassware.AuthUtil.getUserId(request);

    Credential credential = com.google.glassware.AuthUtil.getCredential(userId);

    List<TimelineItem> timelineItems = MirrorClient.listItems(credential, 3L).getItems();

    UserSettings userSettings = UserSettings.getUserSettings(userId);
%>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Glassware Starter Project</title>
    <link href="/static/bootstrap/css/bootstrap.min.css" rel="stylesheet"
          media="screen">

    <style>
        .tile {
            border-left: 1px solid #444;
            padding: 5px;
            list-style: none;
        }

        .btn {
            width: 100%;
        }
    </style>
</head>
<body>
<div class="navbar navbar-inverse navbar-fixed-top">
    <div class="navbar-inner">
        <div class="container">
            <a class="brand" href="#">Real-time traffic situation notification app for Glass</a>

            <div class="nav-collapse collapse">
                <form class="navbar-form pull-right" action="/signout" method="post">
                    <button type="submit" class="btn">Sign out</button>
                </form>
            </div>
            <!--/.nav-collapse -->
        </div>
    </div>
</div>

<div class="container">

    <!-- Main hero unit for a primary marketing message or call to action -->
    <div class="hero-unit">
        <h1>Your Recent Timeline</h1>
        <% String flash = WebUtil.getClearFlash(request);
            if (flash != null) { %>
        <span class="label label-warning">Message: <%= flash %> </span>
        <% } %>

        <div style="margin-top: 5px;">

            <% if (timelineItems != null) {
                for (TimelineItem timelineItem : timelineItems) { %>
            <ul class="span3 tile">
                <li>
                    <strong>Created: </strong> <%= timelineItem.getCreated() %>
                </li>
                <li>
                    <strong>Text: </strong> <%= timelineItem.getText() %>
                </li>
                <li>
                    <strong>HTML: </strong> <%= timelineItem.getHtml() %>
                </li>
                <li>
                    <strong>Speakable text: </strong> <%= timelineItem.getSpeakableText() %>
                </li>
            </ul>
            <% }
            } %>
        </div>
        <div style="clear:both;"></div>
    </div>

    <!-- Example row of columns -->
    <div class="row">
        <div class="span4">
            <h2>Timeline</h2>

            <form action="<%= WebUtil.buildUrl(request, "/main") %>" method="post">
                <input type="hidden" name="operation" value="cleanup">
                <button class="btn" type="submit">Clean up timeline</button>
            </form>
        </div>

        <div class="span4">
            <h2>Status</h2>

            <% if (!userSettings.isNotificationEnabled()) { %>
            <form class="span3" action="<%= WebUtil.buildUrl(request, "/main") %>"
                  method="post">
                <input type="hidden" name="operation" value="enable"/>
                <button class="btn" type="submit">Enable notifications</button>
            </form>
            <% } else { %>
            <form class="span3" action="<%= WebUtil.buildUrl(request, "/main") %>"
                  method="post">
                <input type="hidden" name="operation" value="disable"/>
                <button class="btn" type="submit">Disable notifications</button>
            </form>
            <% } %>
        </div>

        <div class="span4">
            <h2>Subscriptions</h2>

        </div>
    </div>
</div>

<script src="//ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js"></script>
<script src="/static/bootstrap/js/bootstrap.min.js"></script>
</body>
</html>
