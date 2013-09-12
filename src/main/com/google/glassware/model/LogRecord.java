package com.google.glassware.model;

import com.beoui.geocell.GeocellManager;
import com.beoui.geocell.annotations.Geocells;
import com.beoui.geocell.annotations.Latitude;
import com.beoui.geocell.annotations.Longitude;
import com.beoui.geocell.model.Point;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@PersistenceCapable
public class LogRecord {
    public final static SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy h:mmaa");

    @PrimaryKey
    private String id;

    @Persistent
    private Date logTime;

    @Persistent
    private String logType;

    @Persistent
    private String location;

    @Persistent
    private String locationDesc;

    @Persistent
    private String area;

    @Persistent
    @Latitude
    private double lat;

    @Persistent
    @Longitude
    private double lon;

    @Persistent
    private String details;

    @Persistent
    private Date lastUpdated = new Date();

    @Persistent
    @Geocells
    private List<String> geocells;

    public void parse(Node logRecord) throws ParseException {
        NodeList childNodes = logRecord.getChildNodes();
        Element childNodesE = (Element) childNodes;
        id = childNodesE.getAttribute("ID");

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);

            final String nodeName = item.getNodeName().toLowerCase();
            if (nodeName.equals("logtime")) {
                String textContent = item.getTextContent();
                textContent = removeQuotes(textContent);
                logTime = dateFormat.parse(textContent);
            } else if (nodeName.equals("logtype")) {
                String textContent = item.getTextContent();
                logType = removeQuotes(textContent);
            } else if (nodeName.equals("location")) {
                String textContent = item.getTextContent();
                location = removeQuotes(textContent);
            } else if (nodeName.equals("locationdesc")) {
                String textContent = item.getTextContent();
                locationDesc = removeQuotes(textContent);
            } else if (nodeName.equals("area")) {
                String textContent = item.getTextContent();
                area = removeQuotes(textContent);
            } else if (nodeName.equals("latlon")) {
                String textContent = item.getTextContent();
                String latlon = removeQuotes(textContent);
                String[] split = latlon.split(":");
                lat = Double.parseDouble(split[0]) / 1000000d;
                lon = -Double.parseDouble(split[1]) / 1000000d;

                Point p = new Point(lat, lon);
                geocells = GeocellManager.generateGeoCell(p);
            }
        }
    }

    private String removeQuotes(String textContent) {
        return textContent.substring(1, textContent.length() - 1);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getLogTime() {
        return logTime;
    }

    public void setLogTime(Date logTime) {
        this.logTime = logTime;
    }

    public String getLogType() {
        return logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocationDesc() {
        return locationDesc;
    }

    public void setLocationDesc(String locationDesc) {
        this.locationDesc = locationDesc;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public List<String> getGeocells() {
        return geocells;
    }

    public void setGeocells(List<String> geocells) {
        this.geocells = geocells;
    }
}
