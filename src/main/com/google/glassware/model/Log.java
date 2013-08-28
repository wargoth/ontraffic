package com.google.glassware.model;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {
    public final static SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy h:mmaa");


    private Date logTime;
    private String logType;
    private String location;
    private String locationDesc;
    private String area;
    private double lat;
    private double lon;
    private String details;

    public void parse(Node log) throws ParseException {
        NodeList childNodes = log.getChildNodes();

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);

            String nodeName = item.getNodeName().toLowerCase();
            if (nodeName.equals("LogTime")) {
                String textContent = item.getTextContent();
                textContent = textContent.replace("\"", "");
                logTime = dateFormat.parse(textContent);
            }
        }
    }
}
