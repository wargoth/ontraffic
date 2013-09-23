<%@ page import="com.google.api.client.auth.oauth2.Credential" %>
<%@ page import="com.google.api.services.mirror.model.Attachment" %>
<%@ page import="com.google.api.services.mirror.model.Subscription" %>
<%@ page import="com.google.api.services.mirror.model.TimelineItem" %>
<%@ page import="com.google.glassware.MirrorClient" %>
<%@ page import="com.google.glassware.WebUtil" %>
<%@ page import="com.google.glassware.model.UserSettings" %>
<%@ page import="java.util.List" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<!doctype html>
<%
    String userId = com.google.glassware.AuthUtil.getUserId(request);
    String appBaseUrl = WebUtil.buildUrl(request, "/");

    Credential credential = com.google.glassware.AuthUtil.getCredential(userId);

    List<TimelineItem> timelineItems = MirrorClient.listItems(credential, 3L).getItems();


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

    UserSettings userSettings = UserSettings.getUserSettings(userId);
    String pageUrl = WebUtil.buildUrl(request, "/admin/do");

%>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Glassware Starter Project</title>
    <link href="/static/bootstrap/css/bootstrap.min.css" rel="stylesheet"
          media="screen">

    <style>
        .button-icon {
            max-width: 75px;
        }

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
                <li><strong>ID: </strong> <%= timelineItem.getId() %>
                </li>
                <li>
                    <strong>Text: </strong> <%= timelineItem.getText() %>
                </li>
                <li>
                    <strong>HTML: </strong> <%= timelineItem.getHtml() %>
                </li>
                <li>
                    <strong>Attachments: </strong>
                    <%
                        if (timelineItem.getAttachments() != null) {
                            for (Attachment attachment : timelineItem.getAttachments()) {
                                if (MirrorClient.getAttachmentContentType(credential, timelineItem.getId(), attachment.getId()).startsWith("image")) { %>
                    <img src="<%= appBaseUrl + "attachmentproxy?attachment=" + attachment.getId() + "&timelineItem=" + timelineItem.getId() %>">
                    <% } else { %>
                    <a href="<%= appBaseUrl + "attachmentproxy?attachment=" + attachment.getId() + "&timelineItem=" + timelineItem.getId() %>">Download</a>
                    <% }
                    }
                    } %>
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

            <p>When you first sign in, this Glassware inserts a welcome message. Use
                these controls to insert more items into your timeline. Learn more about
                the timeline APIs
                <a href="https://developers.google.com/glass/timeline">here</a></p>


            <form action="<%= pageUrl %>" method="post">
                <input type="hidden" name="operation" value="insertItem">
                <textarea name="message">Hello World!</textarea><br/>
                <button class="btn" type="submit">The above message</button>
            </form>

            <form action="<%= pageUrl %>" method="post">
                <input type="hidden" name="operation" value="insertItem">
                <input type="hidden" name="message" value="Chipotle says 'hi'!">
                <input type="hidden" name="imageUrl" value="<%= appBaseUrl +
               "static/images/chipotle-tube-640x360.jpg" %>">
                <input type="hidden" name="contentType" value="image/jpeg">

                <button class="btn" type="submit">A picture
                    <img class="button-icon" src="<%= appBaseUrl +
               "static/images/chipotle-tube-640x360.jpg" %>">
                </button>
            </form>
            <form action="<%= pageUrl %>" method="post">
                <input type="hidden" name="operation" value="insertItemWithAction">
                <button class="btn" type="submit">A card you can reply to</button>
            </form>
            <hr>
            <form action="<%= pageUrl %>" method="post">
                <input type="hidden" name="operation" value="insertItemAllUsers">
                <button class="btn" type="submit">A card to all users</button>
            </form>

            <form action="<%= pageUrl %>" method="post">
                <input type="hidden" name="operation" value="insertOntraffic">
                <button class="btn" type="submit">Send ontraffic card</button>
            </form>
        </div>

        <div class="span4">
            <h2>Testing mode</h2>

            <% if (!userSettings.isTestingAccount()) { %>
            <form class="span3" action="<%= pageUrl %>"
                  method="post">
                <input type="hidden" name="operation" value="testing-enable"/>
                <button class="btn" type="submit">Enable testing mode</button>
            </form>
            <% } else { %>
            <form class="span3" action="<%= pageUrl %>"
                  method="post">
                <input type="hidden" name="operation" value="testing-disable"/>
                <button class="btn" type="submit">Disable testing mode</button>
            </form>
            <% } %>
        </div>

        <div class="span4">
            <h2>Subscriptions</h2>

            <p>By default a subscription is inserted for changes to the
                <code>timeline</code> collection. Learn more about subscriptions
                <a href="https://developers.google.com/glass/subscriptions">here</a></p>

            <p class="label label-info">Note: Subscriptions require SSL. <br>They will
                not work on localhost.</p>

            <% if (timelineSubscriptionExists) { %>
            <form action="<%= pageUrl %>"
                  method="post">
                <input type="hidden" name="subscriptionId" value="timeline">
                <input type="hidden" name="operation" value="deleteSubscription">
                <button class="btn" type="submit" class="delete">Unsubscribe from
                    timeline updates
                </button>
            </form>
            <% } else { %>
            <form action="<%= pageUrl %>" method="post">
                <input type="hidden" name="operation" value="insertSubscription">
                <input type="hidden" name="collection" value="timeline">
                <button class="btn" type="submit">Subscribe to timeline updates</button>
            </form>
            <% }%>

            <% if (locationSubscriptionExists) { %>
            <form action="<%= pageUrl %>"
                  method="post">
                <input type="hidden" name="subscriptionId" value="locations">
                <input type="hidden" name="operation" value="deleteSubscription">
                <button class="btn" type="submit" class="delete">Unsubscribe from
                    location updates
                </button>
            </form>
            <% } else { %>
            <form action="<%= pageUrl %>" method="post">
                <input type="hidden" name="operation" value="insertSubscription">
                <input type="hidden" name="collection" value="locations">
                <button class="btn" type="submit">Subscribe to location updates</button>
            </form>
            <% }%>
        </div>
    </div>
</div>

<script src="//ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js"></script>
<script src="/static/bootstrap/js/bootstrap.min.js"></script>
</body>
</html>