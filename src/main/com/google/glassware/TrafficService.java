package com.google.glassware;

import com.google.glassware.model.LogRecord;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TrafficService extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(TrafficService.class.getSimpleName());

    private static final long serialVersionUID = 447409731553098042L;
    public static final String DATA_URL = "http://media.chp.ca.gov/sa_xml/sa.xml";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            LOG.log(Level.INFO, "Updating traffic information");

            List<LogRecord> logRecords = update();

            LOG.info("Traffic records collected: " + logRecords.size());

            Database.persistAll(logRecords);
        } catch (ParserConfigurationException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (SAXException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (ParseException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public List<LogRecord> update() throws IOException, ParserConfigurationException, SAXException, ParseException {
        URL url = new URL(DATA_URL);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(false);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(url.openStream());

        NodeList logs = doc.getElementsByTagName("Log");

        List<LogRecord> logRecords = new ArrayList<LogRecord>();

        for (int i = 0; i < logs.getLength(); i++) {
            Node log = logs.item(i);

            LogRecord logRec = new LogRecord();
            logRec.parse(log);

            logRecords.add(logRec);
        }

        return logRecords;
    }
}
