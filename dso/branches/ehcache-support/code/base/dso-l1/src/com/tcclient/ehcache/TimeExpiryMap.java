/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcclient.ehcache;

import com.tc.exception.ImplementMe;
import com.tcclient.cache.CacheDataStore;
import com.tcclient.cache.Expirable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Up to now, TimeExpiryMap is only used in the context of Ehcache which is synchronized and auto-locked
 * in the MemoryStore class; therefore, no synchronization is needed here.
 *
 * TimeExpiryMap will be shared when a Ehcache is clustered. That is why we do not need to make store
 * and dtmStore as root like what Session does.
 */
public class TimeExpiryMap implements Map, Expirable, Cloneable, Externalizable {
  protected final CacheDataStore timeExpiryDataStore;
  private final Map store;
  private final Map dtmStore;
  
  public TimeExpiryMap(long invalidatorSleepSeconds, long maxIdleTimeoutSeconds, String cacheName) {
    this.store = new HashMap();
    this.dtmStore = new HashMap();
    timeExpiryDataStore = new CacheDataStore(invalidatorSleepSeconds, maxIdleTimeoutSeconds, store, dtmStore, "CacheInvalidator - "+cacheName, this);
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
    return store.containsKey(key);
  }

  public boolean containsValue(Object value) {
    return store.containsValue(value);
  }

  public Set entrySet() {
    return store.entrySet();
  }

  public Object get(Object key) {
    return timeExpiryDataStore.get(key);
  }

  public boolean isEmpty() {
    return store.isEmpty();
  }

  public Set keySet() {
    return store.keySet();
  }

  public void putAll(Map map) {
    for (Iterator i=map.entrySet().iterator(); i.hasNext(); ) {
      Map.Entry entry = (Map.Entry)i.next();
      timeExpiryDataStore.put(entry.getKey(), entry.getValue());
    }
  }

  public Object remove(Object key) {
    return timeExpiryDataStore.remove(key);
  }

  public int size() {
    return store.size();
  }

  public Collection values() {
    return store.values();
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

  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    throw new ImplementMe();
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    throw new ImplementMe();
  }
  
  
}
