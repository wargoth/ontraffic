package com.google.glassware;

import com.google.glassware.model.LogRecord;
import com.google.glassware.model.UserLastLocation;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import java.util.Collection;
import java.util.List;

public class Database {
    public static UserLastLocation getUserLastLocation(String userToken) {
        PersistenceManager pm = PMF.get().getPersistenceManager();

        try {
            return pm.getObjectById(UserLastLocation.class, userToken);
        } catch (RuntimeException e) {
            return null;
        } finally {
            pm.close();
        }
    }

    public static void persist(Object someObject) {
        PersistenceManager pm = PMF.get().getPersistenceManager();

        try {
            pm.makePersistent(someObject);
        } finally {
            pm.close();
        }
    }

    public static void persistAll(Collection collection) {
        if (collection.isEmpty())
            return;

        PersistenceManager pm = PMF.get().getPersistenceManager();

        try {
            pm.makePersistentAll(collection);
        } finally {
            pm.close();
        }
    }

    public static void deleteAll(Collection collection) {
        if (collection.isEmpty())
            return;

        PersistenceManager pm = PMF.get().getPersistenceManager();

        try {
            pm.deletePersistentAll(collection);
        } finally {
            pm.close();
        }
    }

    public static List<LogRecord> getLogRecords() {
        PersistenceManager pm = PMF.get().getPersistenceManager();

        Query q = pm.newQuery(LogRecord.class);

        return (List<LogRecord>) q.execute();
    }
}
