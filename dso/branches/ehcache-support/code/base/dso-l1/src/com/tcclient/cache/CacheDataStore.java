/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.cache;

import com.tc.config.lock.LockLevel;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.util.Assert;

import java.util.Map;
import java.util.Set;

public class CacheDataStore {
  private final Map     store;                  // <Data>
  private final Map     dtmStore;               // <Timestamp>
  private final String  cacheName;
  private final long    maxIdleTimeoutSeconds;
  private final long    invalidatorSleepSeconds;
  private Expirable     callback;
  private transient int hitCount;
  private transient int missCountExpired;
  private transient int missCountNotFound;

  public CacheDataStore(long invalidatorSleepSeconds, long maxIdleTimeoutSeconds, Map store, Map dtmStore,
                        String cacheName, Expirable callback) {
    this.store = store;
    this.dtmStore = dtmStore;
    this.cacheName = cacheName;
    this.maxIdleTimeoutSeconds = maxIdleTimeoutSeconds;
    this.invalidatorSleepSeconds = invalidatorSleepSeconds;
    this.callback = callback;

    this.hitCount = 0;
  }

  public void initialize() {
    Thread invalidator = new Thread(new CacheEntryInvalidator(invalidatorSleepSeconds), cacheName);
    invalidator.setDaemon(true);
    invalidator.start();
    Assert.post(invalidator.isAlive());
  }

  public Object put(final Object key, final Object value) {
    Assert.pre(key != null);
    Assert.pre(value != null);
    CacheData cd = (CacheData) store.get(key);
    Object rv = (cd == null) ? null : cd.getValue();

    cd = new CacheData(value, maxIdleTimeoutSeconds);
    ManagerUtil.monitorEnter(store, LockLevel.WRITE);
    try {
      cd.accessed();
      cd.start();
      store.put(key, cd);
      dtmStore.put(key, cd.getTimestamp());
    } finally {
      ManagerUtil.monitorExit(store);
    }
    return rv;
  }

  public Object get(final Object key) {
    Assert.pre(key != null);

    CacheData cd = null;
    cd = (CacheData) store.get(key);
    if (cd != null) {
      if (!cd.isValid()) {
        missCountExpired++;
        invalidate(cd);
        System.err.println("rv is not valid -- key: " + key + ", value: " + cd.getValue() + " rv.getIdleMillis(): "
                           + cd.getIdleMillis());
        return null;
      } else {
        hitCount++;
        cd.accessed();
        updateTimestampIfNeeded(cd);
      }
      return cd.getValue();
    }
    missCountNotFound++;
    return null;
  }

  private void invalidate(CacheData cd) {
    if (!cd.isInvalidated()) {
      ManagerUtil.monitorEnter(store, LockLevel.WRITE);
      try {
        cd.invalidate();
      } finally {
        ManagerUtil.monitorExit(store);
      }
    }
  }

  public boolean isExpired(final Object key) {
    CacheData rv = (CacheData) store.get(key);
    return rv != null && rv.isValid();
  }

  public Object remove(final Object key) {
    Assert.pre(key != null);
    CacheData cd = (CacheData) store.get(key);
    if (cd == null) return null;

    ManagerUtil.monitorEnter(store, LockLevel.WRITE);
    try {
      store.remove(key);
      dtmStore.remove(key);
    } finally {
      ManagerUtil.monitorExit(store);
    }
    return cd.getValue();
  }

  public void expire(Object key, CacheData sd) {
    remove(key);
    callback.expire(key, sd.getValue());
  }

  public void clear() {
    store.clear();
    dtmStore.clear();
  }
  
  public Map getStore() {
    return store;
  }

  void updateTimestampIfNeeded(CacheData rv) {
    Assert.pre(rv != null);
    final long now = System.currentTimeMillis();
    final Timestamp t = rv.getTimestamp();
    final long diff = t.getMillis() - now;
    if (diff < (rv.getMaxInactiveMillis() / 2) || diff > (rv.getMaxInactiveMillis())) {
      ManagerUtil.monitorEnter(store, LockLevel.WRITE);
      try {
        t.setMillis(now + rv.getMaxInactiveMillis());
      } finally {
        ManagerUtil.monitorExit(store);
      }
    }
  }

  Timestamp findTimestampUnlocked(final Object key) {
    return (Timestamp) dtmStore.get(key);
  }

  CacheData findCacheDataUnlocked(final Object key) {
    final CacheData rv = (CacheData) store.get(key);
    return rv;
  }

  public int getHitCount() {
    return hitCount;
  }

  public int getMissCountExpired() {
    return missCountExpired;
  }

  public int getMissCountNotFound() {
    return missCountNotFound;
  }

  public void clearStatistics() {
    this.hitCount = 0;
    this.missCountExpired = 0;
    this.missCountNotFound = 0;
  }

  private String[] getAllKeys() {
    String[] rv;
    synchronized (dtmStore) {
      Set keys = dtmStore.keySet();
      rv = (String[]) keys.toArray(new String[keys.size()]);
    }
    Assert.post(rv != null);
    return rv;
  }

  public void evictExpiredElements() {
    final String invalidatorLock = "tc:time_expiry_cache_invalidator_lock_" + cacheName;

    final Lock lock = new Lock(invalidatorLock);
    lock.tryWriteLock();
    if (!lock.isLocked()) return;

    try {
      invalidateCacheEntries();
    } finally {
      lock.commitLock();
    }
  }

  private void invalidateCacheEntries() {
    final String keys[] = getAllKeys();
    int totalCnt = 0;
    int invalCnt = 0;
    int evaled = 0;
    int notEvaled = 0;
    int errors = 0;

    for (int i = 0; i < keys.length; i++) {
      final String key = keys[i];
      try {
        final Timestamp dtm = findTimestampUnlocked(key);
        if (dtm == null) continue;
        totalCnt++;
        if (dtm.getMillis() < System.currentTimeMillis()) {
          evaled++;
          if (evaluateCacheEntry(dtm, key)) invalCnt++;
        } else {
          notEvaled++;
        }
      } catch (Throwable t) {
        errors++;
        t.printStackTrace(System.err);
        // logger.error("Unhandled exception inspecting session " + key + " for invalidation", t);
      }
    }
  }

  private boolean evaluateCacheEntry(final Timestamp dtm, final Object key) {
    Assert.pre(key != null);

    boolean rv = false;

    final CacheData sd = findCacheDataUnlocked(key);
    if (sd == null) return rv;
    if (!sd.isValid()) {
      expire(key, sd);
      rv = true;
    } else {
      updateTimestampIfNeeded(sd);
    }
    return rv;
  }

  private class CacheEntryInvalidator implements Runnable {
    private final long sleepMillis;

    public CacheEntryInvalidator(final long sleepSeconds) {
      this.sleepMillis = sleepSeconds * 1000;
    }

    public void run() {
      while (true) {
        sleep(sleepMillis);
        if (Thread.interrupted()) {
          break;
        } else {
          try {
            evictExpiredElements();
          } catch (Throwable t) {
            // logger.error("Unhandled exception occurred during session invalidation", t);
          }
        }
      }
    }

    private void sleep(long l) {
      try {
        Thread.sleep(l);
      } catch (InterruptedException ignore) {
        // nothing to do
      }
    }
  }
}
