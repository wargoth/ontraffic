package com.google.glassware;

import com.google.glassware.model.UserLastLocation;

import javax.jdo.PersistenceManager;

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

    public static void saveUserLastLocation(UserLastLocation newLocation) {
        PersistenceManager pm = PMF.get().getPersistenceManager();

        try {
            pm.makePersistent(newLocation);
        } finally {
            pm.close();
        }
    }
}
