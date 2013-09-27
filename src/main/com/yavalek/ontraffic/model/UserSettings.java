package com.yavalek.ontraffic.model;

import com.yavalek.ontraffic.PMF;

import javax.jdo.PersistenceManager;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable
public class UserSettings {
    @PrimaryKey
    private String userToken;

    @Persistent
    private boolean notificationEnabled = true;

    @Persistent
    private boolean testingAccount = false;

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
