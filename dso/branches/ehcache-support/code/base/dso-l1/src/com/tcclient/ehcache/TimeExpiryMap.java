/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.ehcache;

import com.tcclient.cache.CacheDataStore;
import com.tcclient.cache.Expirable;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Up to now, TimeExpiryMap is only used in the context of Ehcache which is synchronized and auto-locked in the
 * MemoryStore class; therefore, no synchronization is needed here. TimeExpiryMap will be shared when a Ehcache is
 * clustered. That is why we do not need to make store and dtmStore as root like what Session does.
 */
public class TimeExpiryMap implements Map, Expirable, Cloneable, Serializable {
  protected final CacheDataStore timeExpiryDataStore;

  public TimeExpiryMap(long invalidatorSleepSeconds, long maxIdleTimeoutSeconds, String cacheName) {
    timeExpiryDataStore = new CacheDataStore(invalidatorSleepSeconds, maxIdleTimeoutSeconds, new HashMap(),
                                             new HashMap(), "CacheInvalidator - " + cacheName, this);
    timeExpiryDataStore.initialize();
  }

  public Object put(Object key, Object value) {
    return timeExpiryDataStore.put(key, value);
  }

  public void expire(Object key, Object value) {
    processExpired(key, value);
  }

  protected void processExpired(Object key, Object value) {
    //
  }

  public void clear() {
    timeExpiryDataStore.clear();
  }

  public boolean containsKey(Object key) {
    return timeExpiryDataStore.getStore().containsKey(key);
  }

  public boolean containsValue(Object value) {
    return timeExpiryDataStore.getStore().containsValue(value);
  }

  public Set entrySet() {
    return timeExpiryDataStore.getStore().entrySet();
  }

  public Object get(Object key) {
    return timeExpiryDataStore.get(key);
  }

  public boolean isEmpty() {
    return timeExpiryDataStore.getStore().isEmpty();
  }

  public Set keySet() {
    return timeExpiryDataStore.getStore().keySet();
  }

  public void putAll(Map map) {
    for (Iterator i = map.entrySet().iterator(); i.hasNext();) {
      Map.Entry entry = (Map.Entry) i.next();
      timeExpiryDataStore.put(entry.getKey(), entry.getValue());
    }
  }

  public Object remove(Object key) {
    return timeExpiryDataStore.remove(key);
  }

  public int size() {
    return timeExpiryDataStore.getStore().size();
  }

  public Collection values() {
    return timeExpiryDataStore.getStore().values();
  }

  public int getHitCount() {
    return timeExpiryDataStore.getHitCount();
  }

  public int getMissCountExpired() {
    return timeExpiryDataStore.getMissCountExpired();
  }

  public int getMissCountNotFound() {
    return timeExpiryDataStore.getMissCountNotFound();
  }

  public boolean isExpired(final Object key) {
    return timeExpiryDataStore.isExpired(key);
  }

  public void clearStatistics() {
    timeExpiryDataStore.clearStatistics();
  }
}
