package com.yavalek.ontraffic;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialStore;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * A new credential store for App Engine. It's exactly the same as
 * com.google.api
 * .client.extensions.appengine.auth.oauth2.AppEngineCredentialStore except it
 * has the added ability to list all of the users.
 * 
 * @author Jenny Murphy - http://google.com/+JennyMurphy
 */
public class ListableAppEngineCredentialStore implements CredentialStore {

  private static final String KIND = ListableAppEngineCredentialStore.class.getName();

  public List<String> listAllUsers() {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Query userQuery = new Query(KIND);
    Iterable<Entity> userEntities = datastore.prepare(userQuery).asIterable();

    List<String> userIds = new ArrayList<String>();
    for (Entity userEntity : userEntities) {
      userIds.add(userEntity.getKey().getName());
    }
    return userIds;
  }


  @Override
  public void store(String userId, Credential credential) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity entity = new Entity(KIND, userId);
    entity.setProperty("accessToken", credential.getAccessToken());
    entity.setProperty("refreshToken", credential.getRefreshToken());
    entity.setProperty("expirationTimeMillis", credential.getExpirationTimeMilliseconds());
    datastore.put(entity);
  }

  @Override
  public void delete(String userId, Credential credential) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Key key = KeyFactory.createKey(KIND, userId);
    datastore.delete(key);
  }

  @Override
  public boolean load(String userId, Credential credential) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Key key = KeyFactory.createKey(KIND, userId);
    try {
      Entity entity = datastore.get(key);
      credential.setAccessToken((String) entity.getProperty("accessToken"));
      credential.setRefreshToken((String) entity.getProperty("refreshToken"));
      credential.setExpirationTimeMilliseconds((Long) entity.getProperty("expirationTimeMillis"));
      return true;
    } catch (EntityNotFoundException exception) {
      return false;
    }
  }
}
