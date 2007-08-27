/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.cache;

import com.tc.config.lock.LockLevel;
import com.tc.exception.TCRuntimeException;
import com.tc.object.bytecode.Clearable;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.TCMap;
import com.tc.util.Assert;
import com.tc.util.DebugUtil;

import java.io.Serializable;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class CacheDataStore implements Serializable {
  private final Map                       store;                  // <Data>
  private final Map                       dtmStore;               // <Timestamp>
  private final String                    cacheName;
  private final long                      maxIdleTimeoutSeconds;
  private final long                      maxTTLSeconds;
  private final long                      invalidatorSleepSeconds;
  private Expirable                       callback;
  private transient int                   hitCount;
  private transient int                   missCountExpired;
  private transient int                   missCountNotFound;
  // private transient CacheInvalidationTimer cacheInvalidationTimer;
  private transient CacheEntryInvalidator cacheInvalidator;

  public CacheDataStore(long invalidatorSleepSeconds, long maxIdleTimeoutSeconds, long maxTTLSeconds, Map store,
                        Map dtmStore, String cacheName, Expirable callback) {
    Assert.assertTrue(store instanceof TCMap);
    Assert.assertTrue(dtmStore instanceof TCMap);
    this.store = store;
    this.dtmStore = dtmStore;
    this.cacheName = cacheName;
    this.maxIdleTimeoutSeconds = maxIdleTimeoutSeconds;
    this.maxTTLSeconds = maxTTLSeconds;
    this.invalidatorSleepSeconds = invalidatorSleepSeconds;
    this.callback = callback;

    this.hitCount = 0;

    ((Clearable) dtmStore).setEvictionEnabled(false);
  }

  public CacheDataStore(long invalidatorSleepSeconds, long maxIdleTimeoutSeconds, Map store, Map dtmStore,
                        String cacheName, Expirable callback) {
    this(invalidatorSleepSeconds, maxIdleTimeoutSeconds, Long.MAX_VALUE, store, dtmStore, cacheName, callback);
  }

  public void initialize() {
    // this.cacheInvalidationTimer = new CacheInvalidationTimer(invalidatorSleepSeconds, cacheName
    // + " invalidation thread",
    // new CacheEntryInvalidator());
    // this.cacheInvalidationTimer.schedule(true);
    //    
    cacheInvalidator = new CacheEntryInvalidator(invalidatorSleepSeconds);
    cacheInvalidator.scheduleNextInvalidation();
  }

  public void stopInvalidatorThread() {
    //cacheInvalidationTimer.cancel();
    cacheInvalidator.cancel();
  }

  public Object put(final Object key, final Object value) {
    Assert.pre(key != null);
    Assert.pre(value != null);

    CacheData cd = new CacheData(value, maxIdleTimeoutSeconds, maxTTLSeconds);
    cd.accessed();
    if (DebugUtil.DEBUG) {
      System.err.println("Client " + ManagerUtil.getClientID() + " putting " + key);
    }
    CacheData rcd = (CacheData) store.put(key, cd);
    dtmStore.put(key, cd.getTimestamp());

    Object rv = (rcd == null) ? null : rcd.getValue();
    return rv;
  }

  public Object get(final Object key) {
    Assert.pre(key != null);

    CacheData cd = null;
    cd = findCacheDataUnlocked(key);
    if (cd != null) {
      if (!cd.isValid()) {
        missCountExpired++;
        invalidate(cd);
        if (DebugUtil.DEBUG) {
          System.err.println("Client " + ManagerUtil.getClientID() + " rv is not valid -- key: " + key + ", value: "
                             + cd.getValue() + " rv.getIdleMillis(): " + cd.getIdleMillis());
        }
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
      ManagerUtil.monitorEnter(store, LockLevel.CONCURRENT);
      try {
        cd.invalidate();
      } finally {
        ManagerUtil.monitorExit(store);
      }
    }
  }

  public boolean isExpired(final Object key) {
    CacheData rv = findCacheDataUnlocked(key);
    if (DebugUtil.DEBUG) {
      System.err.println("Client " + ManagerUtil.getClientID() + " checking isExpired for key: " + key + " rv: " + rv
                         + " rv.isValid: " + ((rv == null) ? false : rv.isValid()));
    }
    return rv == null || !rv.isValid();
  }

  public Object remove(final Object key) {
    CacheData cd = findCacheDataUnlocked(key);
    if (cd == null) return null;
    removeInternal(key);
    return cd.getValue();
  }

  private void removeInternal(final Object key) {
    Assert.pre(key != null);

    ((TCMap) store).__tc_remove_logical(key);
    ((TCMap) dtmStore).__tc_remove_logical(key);
    // store.remove(key);
    // dtmStore.remove(key);
  }

  public void expire(Object key) {
    removeInternal(key);
    callback.expire(key);
  }

  public void clear() {
    store.clear();
    dtmStore.clear();
  }

  public Map getStore() {
    return store;
  }

  public long getMaxIdleTimeoutSeconds() {
    return maxIdleTimeoutSeconds;
  }

  public long getMaxTTLSeconds() {
    return maxTTLSeconds;
  }

  void updateTimestampIfNeeded(CacheData rv) {
    if (maxIdleTimeoutSeconds <= 0) { return; }

    Assert.pre(rv != null);
    final long now = System.currentTimeMillis();
    final Timestamp t = rv.getTimestamp();
    final long expiredTimeMillis = t.getExpiredTimeMillis();
    if (needsUpdate(rv)) {
      ManagerUtil.monitorEnter(store, LockLevel.CONCURRENT);
      try {
        if (DebugUtil.DEBUG) {
          System.err.println("Client " + ManagerUtil.getClientID() + " expiredTimeMillis before monitorEnter: "
                             + expiredTimeMillis + " expiredTimeMillis after monitorEnter: " + t.getExpiredTimeMillis()
                             + " setting ExpiredTimeMillis: " + (now + rv.getMaxInactiveMillis()));
        }
        t.setExpiredTimeMillis(now + rv.getMaxInactiveMillis());
      } finally {
        ManagerUtil.monitorExit(store);
      }
    }
  }

  boolean needsUpdate(CacheData rv) {
    final long now = System.currentTimeMillis();
    final Timestamp t = rv.getTimestamp();
    final long diff = t.getExpiredTimeMillis() - now;
    return (diff < (rv.getMaxInactiveMillis() / 2) || diff > (rv.getMaxInactiveMillis()));
  }

  Timestamp findTimestampUnlocked(final Map.Entry timeStampEntry) {
    return (Timestamp) timeStampEntry.getValue();
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

  private Object[] getAllLocalEntries() {
    if (DebugUtil.DEBUG) {
      System.err.println("Client " + ManagerUtil.getClientID() + " getting All entries");
    }
    return ((TCMap) dtmStore).__tc_getAllLocalEntriesSnapshot();
  }

  public void evictExpiredElements() {
    invalidateLocalCacheEntries();
  }

  private void invalidateLocalCacheEntries() {
    final Object[] localEntries = getAllLocalEntries();
    int totalCnt = 0;
    int evaled = 0;
    int notEvaled = 0;
    int errors = 0;

    if (DebugUtil.DEBUG) {
      for (int i = 0; i < localEntries.length; i++) {
        System.err.println("Client " + ManagerUtil.getClientID() + " invalidateCacheEntries -- keys: "
                           + localEntries[i]);
      }
    }

    for (int i = 0; i < localEntries.length; i++) {
      final Map.Entry timestampEntry = (Map.Entry) localEntries[i];
      try {
        final Timestamp dtm = findTimestampUnlocked(timestampEntry);
        if (dtm == null) continue;
        totalCnt++;
        if (DebugUtil.DEBUG) {
          System.err.println("Client id: " + ManagerUtil.getClientID() + " key: " + timestampEntry.getKey()
                             + " InvalidateCacheEntries [dtm.getMillis]: " + dtm.getInvalidatedTimeMillis()
                             + " [currentMillis]: " + System.currentTimeMillis());
        }
        if (dtm.getInvalidatedTimeMillis() < System.currentTimeMillis()) {
          evaled++;
          if (DebugUtil.DEBUG) {
            System.err.println("Client id: " + ManagerUtil.getClientID() + " exipring key: " + timestampEntry.getKey());
          }
          expire(timestampEntry.getKey());
          // if (evaluateCacheEntry(dtm, key)) invalCnt++;
        } else {
          notEvaled++;
        }
      } catch (Throwable t) {
        errors++;
        t.printStackTrace(System.err);
        // logger.error("Unhandled exception inspecting session " + key + " for invalidation", t);
      }
    }
    System.err.println("Client " + ManagerUtil.getClientID() + " finish evicting " + evaled + " cache entries.");
  }

  // private boolean evaluateCacheEntry(final Timestamp dtm, final Object key) {
  // Assert.pre(key != null);
  //
  // boolean rv = false;
  //
  // final CacheData sd = findCacheDataUnlocked(key);
  // if (sd == null) return rv;
  // if (!sd.isValid()) {
  // if (DebugUtil.DEBUG) {
  // System.err.println("Client " + ManagerUtil.getClientID() + " expiring " + key);
  // }
  // expire(key, sd);
  // rv = true;
  // // } else {
  // // updateTimestampIfNeeded(sd);
  // }
  // return rv;
  // }

  private class CacheEntryInvalidator extends TimerTask {
    private final Lock  globalInvalidationLock;
    private final Lock  localInvalidationLock;
    private boolean     isGlobalInvalidator = false;
    private final long  sleepMillis;
    private final Timer timer;

    public CacheEntryInvalidator(final long sleepSeconds) {
      this.sleepMillis = sleepSeconds * 1000;
      this.timer = new Timer(true);
      String localEvictionLockName = "tc:local_time_expiry_cache_invalidator_lock_" + cacheName;
      String globalEvictionLockName = "tc:global_time_expiry_cache_invalidator_lock_" + cacheName;
      localInvalidationLock = new Lock(localEvictionLockName);
      globalInvalidationLock = new Lock(globalEvictionLockName);
    }

    public void scheduleNextInvalidation() {
      if (DebugUtil.DEBUG) {
        System.err.println("Client " + ManagerUtil.getClientID() + " schedule next invalidation " + this.sleepMillis);
      }
      // If sleepMillis is <= 0, there will be no eviction, honoring the native ehcache semantics.
      if (this.sleepMillis <= 0) { return; }
      timer.schedule(this, this.sleepMillis, this.sleepMillis);
    }

    public void run() {
      try {
        if (DebugUtil.DEBUG) {
          System.err.println("Client " + ManagerUtil.getClientID() + " running evictExpiredElements");
        }
        localInvalidationLock.writeLock();
        try {
          evictExpiredElements();
        } finally {
          localInvalidationLock.commitLock();
        }
      } catch (Throwable t) {
        t.printStackTrace(System.err);
        throw new TCRuntimeException(t);
      }
    }
  }

  // private class CacheEntryInvalidator implements Runnable {
  // private final TCProperties globalEnvictionProperties;
  // private final Lock globalInvalidationLock;
  // private final Lock localInvalidationLock;
  // private boolean isGlobalInvalidator = false;
  //
  // public CacheEntryInvalidator() {
  // String localEvictionLockName = "tc:local_time_expiry_cache_invalidator_lock_" + cacheName;
  // String globalEvictionLockName = "tc:global_time_expiry_cache_invalidator_lock_" + cacheName;
  // localInvalidationLock = new Lock(localEvictionLockName);
  // globalInvalidationLock = new Lock(globalEvictionLockName);
  // globalEnvictionProperties = TCPropertiesImpl.getProperties().getPropertiesFor("ehcache.global.eviction");
  // }
  //
  // public void run() {
  // try {
  // //System.err.println("Client " + ManagerUtil.getClientID() + " running evictExpiredElements");
  // //tryToBeGlobalInvalidator();
  //
  // localInvalidationLock.writeLock();
  // try {
  // evictExpiredElements();
  // if (isTimeForGlobalInvalidation()) {
  // //
  // }
  // } finally {
  // localInvalidationLock.commitLock();
  // }
  // } catch (Throwable t) {
  // t.printStackTrace(System.err);
  // throw new TCRuntimeException(t);
  // }
  // }
  //
  // public void postRun() {
  // if (isGlobalInvalidator) {
  // globalInvalidationLock.commitLock();
  // isGlobalInvalidator = false;
  // }
  // }
  //
  // private boolean isTimeForGlobalInvalidation() {
  // return false;
  // }
  //    
  // private boolean isGlobalEnvictionEnabled() {
  // return globalEnvictionProperties.getBoolean("enable");
  // }
  //
  // private void tryToBeGlobalInvalidator() {
  // if (isGlobalEnvictionEnabled() && !isGlobalInvalidator) {
  // if (globalInvalidationLock.tryWriteLock()) {
  // isGlobalInvalidator = true;
  // }
  // }
  // }
  // }
  //
  // private class CacheInvalidationTimer implements Runnable {
  // private final long delayMillis;
  // private boolean recurring = true;
  // private final String timerName;
  // private final CacheEntryInvalidator invalidationTask;
  //
  // public CacheInvalidationTimer(final long delayInSecs, final String timerName,
  // final CacheEntryInvalidator invalidationTask) {
  // this.timerName = timerName;
  // this.invalidationTask = invalidationTask;
  // this.delayMillis = delayInSecs * 1000;
  // }
  //
  // public void schedule(boolean recurring) {
  // if (delayMillis <= 0) { return; }
  //      
  // this.recurring = recurring;
  // Thread t = new Thread(this, timerName);
  // t.setDaemon(true);
  // t.start();
  // }
  //
  // public void run() {
  // long nextDelay = delayMillis;
  // try {
  // do {
  // if (DebugUtil.DEBUG) {
  // System.err.println("Client " + ManagerUtil.getClientID() + " going to sleep for " + nextDelay);
  // }
  // sleep(nextDelay);
  // invalidationTask.run();
  // } while (recurring);
  // } finally {
  // invalidationTask.postRun();
  // }
  // }
  //
  // private void sleep(long l) {
  // try {
  // Thread.sleep(l);
  // } catch (InterruptedException ignore) {
  // // nothing to do
  // }
  // }
  //
  // public void cancel() {
  // recurring = false;
  // }
  // }
}
