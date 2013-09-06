package com.google.glassware;

import com.google.glassware.model.LogRecord;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class CleanupService extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(CleanupService.class.getSimpleName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOG.info("Starting cleaning up");

        PersistenceManager pm = PMF.get().getPersistenceManager();

        Query q = pm.newQuery(LogRecord.class);
        q.setFilter("lastUpdated = null OR lastUpdated < lastUpdatedParam");
        q.declareParameters("Date lastUpdatedParam");

        List<LogRecord> logRecords = (List<LogRecord>) q.execute();

        List<LogRecord> toDelete = new ArrayList<>(100);

        Calendar oldestCal = Calendar.getInstance();
        oldestCal.add(Calendar.DAY_OF_MONTH, -1);
        Date oldest = oldestCal.getTime();

        int cleanedUp = 0;

        for (LogRecord logRecord : logRecords) {
            Date lastUpdated = logRecord.getLastUpdated();

            if (lastUpdated == null || lastUpdated.before(oldest)) {
                toDelete.add(logRecord);
                cleanedUp++;

                if (toDelete.size() == 100) {
                    Database.deleteAll(toDelete);
                    toDelete.clear();
                }
            }

        }

        LOG.info(String.format("Cleanup complete. Deleted %d records out of %d", cleanedUp, logRecords.size()));
    }
}
