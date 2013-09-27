<%@ page import="com.yavalek.ontraffic.WebUtil" %>
<%@ page import="com.yavalek.ontraffic.model.UserSettings" %>
<%@ page import="com.yavalek.ontraffic.AuthUtil" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%
    String userId = AuthUtil.getUserId(request);

    UserSettings userSettings = UserSettings.getUserSettings(userId);

    String pageUrl = WebUtil.buildUrl(request, "/main");
%>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">

    <title>Real-time traffic situation app for Google Glass</title>

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
            <span class="navbar-brand">Real-time traffic situation app for Google Glass</span>
        </div>
        <div class="navbar-collapse collapse">
            <form class="navbar-form navbar-right" action="/signout" method="post">
                <button type="submit" class="btn">Sign out</button>
            </form>
        </div>
    </div>
</div>

<div class="jumbotron">
    <div class="container">
        <h1>Profile</h1>

        <p>This is not much to set up here, the application is already on duty. There might be more settings to come as
            application evolves.</p>

        <p>
                <% if (!userSettings.isNotificationEnabled()) { %>

        <form class="span3" action="<%= pageUrl %>" method="post">
            <input type="hidden" name="operation" value="enable"/>
            <button class="btn btn-lg btn-success" type="submit">Enable notifications</button>
        </form>
        <% } else { %>
        <form class="span3" action="<%= pageUrl %>" method="post">
            <input type="hidden" name="operation" value="disable"/>
            <button type="submit" class="btn btn-lg btn-warning">Disable notifications</button>
        </form>
        <% } %>
        </p>
    </div>
</div>

<div class="container">
    <h2>If you love this app, donate!</h2>

    <form action="https://www.paypal.com/cgi-bin/webscr" method="post" target="_top">
        <input type="hidden" name="cmd" value="_s-xclick">
        <input type="hidden" name="hosted_button_id" value="HALCPL2TTMFU4">
        <input type="image" src="https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif" border="0" name="submit"
               alt="PayPal - The safer, easier way to pay online!">
        <img alt="" border="0" src="https://www.paypalobjects.com/en_US/i/scr/pixel.gif" width="1" height="1">
    </form>

</div>
<!-- /container -->


<!-- Bootstrap core JavaScript
================================================== -->
<!-- Placed at the end of the document so the pages load faster -->

<script src="//ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js"></script>
<script src="../static/bootstrap/js/bootstrap.min.js"></script>
</body>
</html>
