package com.yavalek.ontraffic;

import javax.jdo.PersistenceManager;
import java.util.Collection;

public class Database {

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
}
