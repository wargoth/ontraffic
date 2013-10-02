<%@ page import="com.google.api.client.auth.oauth2.Credential" %>
<%@ page import="com.google.api.services.mirror.model.Subscription" %>
<%@ page import="com.google.api.services.mirror.model.TimelineItem" %>
<%@ page import="com.yavalek.ontraffic.MirrorClient" %>
<%@ page import="com.yavalek.ontraffic.WebUtil" %>
<%@ page import="com.yavalek.ontraffic.model.UserSettings" %>
<%@ page import="java.util.List" %>
<%@ page import="com.yavalek.ontraffic.AuthUtil" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String userId = AuthUtil.getUserId(request);
    String appBaseUrl = WebUtil.buildUrl(request, "/");

    Credential credential = AuthUtil.getCredential(userId);

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
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">

    <title>Real-time traffic condition app for Google Glass</title>

    <link href="../static/bootstrap/css/bootstrap.min.css" rel="stylesheet">
    <link href="../static/css/app.css" rel="stylesheet">
</head>

<body>

<div class="navbar navbar-inverse navbar-fixed-top">
    <div class="container">
        <div class="navbar-header">
            <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
            <span class="navbar-brand">Real-time traffic condition app for Google Glass</span>
        </div>
        <div class="navbar-collapse collapse">
            <form class="navbar-form navbar-right" action="/signout" method="post">
                <button type="submit" class="btn">Sign out</button>
            </form>
        </div>
        <!--/.navbar-collapse -->
    </div>
</div>


<div class="jumbotron">
    <div class="container">
        <h1>Your Recent Timeline</h1>

        <% String flash = WebUtil.getClearFlash(request);
            if (flash != null) { %>
        <div class="alert alert-warning">
            <%= flash %>
        </div>
        <% } %>
    </div>
</div>


<div class="container">
    <div class="row">
        <% if (timelineItems != null) {
            for (TimelineItem timelineItem : timelineItems) { %>
        <div class="col-lg-4">
            <p><strong>ID:</strong> <%= timelineItem.getId() %>
            </p>

            <p><strong>Text:</strong> <%= timelineItem.getText() %>
            </p>

            <p><strong>HTML:</strong> <%= timelineItem.getHtml() %>
            </p>

            <p><strong>Speakable text:</strong> <%= timelineItem.getSpeakableText() %>
            </p>
        </div>
        <% }
        } %>
    </div>

    <hr>

    <div class="row">
        <div class="col-lg-4">
            <h2>Timeline</h2>

            <p>

            <form action="<%= pageUrl %>" method="post">
                <input type="hidden" name="operation" value="insertItem">
                <textarea name="message">Hello World!</textarea><br/>
                <button class="btn" type="submit">The above message</button>
            </form>
            </p>

            <p>

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
            </p>
            <p>

            <form action="<%= pageUrl %>" method="post">
                <input type="hidden" name="operation" value="insertItemWithAction">
                <button class="btn" type="submit">A card you can reply to</button>
            </form>
            </p>
            <hr>

            <p>
            <form action="<%= pageUrl %>" method="post">
                <input type="hidden" name="operation" value="insertOntrafficSF">
                <button class="btn" type="submit">Send SF ontraffic card</button>
            </form>
            </p>

            <p>
            <form action="<%= pageUrl %>" method="post">
                <input type="hidden" name="operation" value="insertOntrafficLondon">
                <button class="btn" type="submit">Send London ontraffic card</button>
            </form>
            </p>

            <p>

            <form action="<%= pageUrl %>" method="post">
                <input type="hidden" name="operation" value="cleanup">
                <button class="btn" type="submit">Clean up timeline</button>
            </form>
            </p>
        </div>

        <div class="col-lg-4">
            <h2>Testing mode</h2>

            <p>
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
            </p>
        </div>

        <div class="col-lg-4">
            <h2>Subscriptions</h2>

            <p>By default a subscription is inserted for changes to the
                <code>timeline</code> collection. Learn more about subscriptions
                <a href="https://developers.google.com/glass/subscriptions">here</a></p>

            <p class="label label-info">Note: Subscriptions require SSL. <br>They will
                not work on localhost.</p>

            <p>
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
            </p>

            <p>
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
            </p>
        </div>
    </div>
</div>

<script src="//ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js"></script>
<script src="../static/bootstrap/js/bootstrap.min.js"></script>
</body>
</html>
