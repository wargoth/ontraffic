package com.google.glassware.model;

import com.google.glassware.PMF;

import javax.jdo.PersistenceManager;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import java.util.Date;

@PersistenceCapable
public class UserSettings {
    @PrimaryKey
    private String userToken;

    @Persistent
    private boolean notificationEnabled = true;

    @Persistent
    private boolean testingAccount = false;

    @Persistent
    private Date updated = new Date();

    public UserSettings() {
    }

    public UserSettings(String userToken) {
        this.userToken = userToken;
    }

    public String getUserToken() {
        return userToken;
    }

    public void setUserToken(String userToken) {
        this.userToken = userToken;
    }

    public boolean isNotificationEnabled() {
        return notificationEnabled;
    }

    public void setNotificationEnabled(boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
    }

    public boolean isTestingAccount() {
        return testingAccount;
    }

    public void setTestingAccount(boolean testingAccount) {
        this.testingAccount = testingAccount;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public static UserSettings getUserSettings(String userToken) {
        PersistenceManager pm = PMF.get().getPersistenceManager();

        try {
            UserSettings userSettings = pm.getObjectById(UserSettings.class, userToken);
            if (userSettings == null) {
                userSettings = new UserSettings(userToken);
            }
            return userSettings;
        } catch (RuntimeException e) {
            return new UserSettings(userToken);
        } finally {
            pm.close();
        }
    }
}
