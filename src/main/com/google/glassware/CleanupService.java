package com.google.glassware;

import com.google.glassware.model.LogRecord;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

public class CleanupService extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(CleanupService.class.getSimpleName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Calendar oldestCal = Calendar.getInstance();
        oldestCal.add(Calendar.DAY_OF_MONTH, -1);
        Date oldest = oldestCal.getTime();

        LOG.info("Starting cleaning up. Deleting records older then " + oldest);

        PersistenceManager pm = PMF.get().getPersistenceManager();

        Query q = pm.newQuery(LogRecord.class);
        q.setFilter("lastUpdated < oldestParam");
        q.declareImports("import java.util.Date");
        q.declareParameters("Date oldestParam");

        long deleted = q.deletePersistentAll(oldest);

        LOG.info(String.format("Cleanup complete. Deleted %d records", deleted));
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOG.info("Cleaning up the entire DB!");

        PersistenceManager pm = PMF.get().getPersistenceManager();

        Query q = pm.newQuery(LogRecord.class);

        long deleted = q.deletePersistentAll();

        LOG.info(String.format("Cleanup complete. Deleted %d records", deleted));
    }
}
