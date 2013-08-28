package com.google.glassware;

import com.google.glassware.model.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URL;

public class TrafficService {
    public void update() throws IOException, ParserConfigurationException, SAXException {
        URL url = new URL("http://www.example.com/atom.xml");

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(url.openStream());

        NodeList logs = doc.getElementsByTagName("Log");

        for (int i = 0; i < logs.getLength(); i++) {
            Node log = logs.item(i);

            Log logRec = new Log();
            logRec.parse(log);
        }
    }
}
