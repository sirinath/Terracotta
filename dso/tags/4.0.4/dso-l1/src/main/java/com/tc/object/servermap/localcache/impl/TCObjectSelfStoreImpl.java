/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.exception.PlatformRejoinException;
import com.tc.exception.TCNotRunningException;
import com.tc.invalidation.Invalidations;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.TCObjectSelf;
import com.tc.object.TCObjectSelfCallback;
import com.tc.object.TCObjectSelfStore;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.PinnedEntryFaultCallback;
import com.tc.object.servermap.localcache.ServerMapLocalCache;
import com.tc.util.ObjectIDSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TCObjectSelfStoreImpl implements TCObjectSelfStore {
  private final TCObjectSelfStoreObjectIDSet                                     tcObjectSelfStoreOids = new TCObjectSelfStoreObjectIDSet();
  private final ReentrantReadWriteLock                                           tcObjectStoreLock     = new ReentrantReadWriteLock();
  private volatile TCObjectSelfCallback                                          tcObjectSelfRemovedFromStoreCallback;
  private final Map<ObjectID, TCObjectSelf>                                      tcObjectSelfTempCache = new HashMap<ObjectID, TCObjectSelf>();

  private final ConcurrentHashMap<ServerMapLocalCache, PinnedEntryFaultCallback> localCaches;

  private static final TCLogger                                                  logger                = TCLogging
                                                                                                           .getLogger(TCObjectSelfStoreImpl.class);

  private volatile boolean                                                       isShutdown            = false;
  private volatile boolean                                                       isRejoinInProgress    = false;

  public TCObjectSelfStoreImpl(ConcurrentHashMap<ServerMapLocalCache, PinnedEntryFaultCallback> localCaches) {
    this.localCaches = localCaches;
  }

  @Override
  public void cleanup() {
    synchronized (tcObjectSelfRemovedFromStoreCallback) {
      tcObjectSelfRemovedFromStoreCallback.notifyAll();
      tcObjectStoreLock.writeLock().lock();
      try {
        tcObjectSelfStoreOids.clear();
        tcObjectSelfTempCache.clear();
      } finally {
        tcObjectStoreLock.writeLock().unlock();
      }
    }
  }

  private void isShutdownThenException() {
    if (isShutdown) { throw new TCNotRunningException("TCObjectSelfStore already shutdown"); }
  }

  @Override
  public void removeObjectById(ObjectID oid) {
    isShutdownThenException();

    synchronized (tcObjectSelfRemovedFromStoreCallback) {
      tcObjectStoreLock.writeLock().lock();
      try {
        if (!tcObjectSelfStoreOids.contains(oid)) { return; }
      } finally {
        tcObjectStoreLock.writeLock().unlock();
      }
    }

    for (ServerMapLocalCache cache : localCaches.keySet()) {
      cache.removeEntriesForObjectId(oid);
    }
  }

  @Override
  public Object getById(ObjectID oid) {
    isShutdownThenException();

    long timePrev = System.currentTimeMillis();
    long startTime = timePrev;
    boolean interrupted = false;
    try {
      while (true) {
        Object rv = null;
        tcObjectStoreLock.readLock().lock();
        try {
          TCObjectSelf self = tcObjectSelfTempCache.get(oid);
          if (self != null) { return self; }

          if (!tcObjectSelfStoreOids.contains(oid)) {
            if (logger.isDebugEnabled()) {
              logger.debug("XXX GetById failed at TCObjectSelfStoreIDs, ObjectID=" + oid);
            }
            return null;
          }

          for (ServerMapLocalCache localCache : this.localCaches.keySet()) {
            Object key = localCache.getMappingUnlocked(oid);
            if (key == null) {
              continue;
            }

            AbstractLocalCacheStoreValue localCacheStoreValue = (AbstractLocalCacheStoreValue) localCache
                .getMappingUnlocked(key);
            rv = localCacheStoreValue == null ? null : localCacheStoreValue.getValueObject();
            initTCObjectSelfIfRequired(rv);

            if (rv == null && logger.isDebugEnabled()) {
              logger.debug("XXX GetById failed when localCacheStoreValue was null for eventual, ObjectID=" + oid);
            }
            break;
          }

          if (logger.isDebugEnabled()) {
            logger.debug("XXX GetById failed when it couldn't find in any stores, ObjectID=" + oid);
          }
        } finally {
          tcObjectStoreLock.readLock().unlock();
        }

        if (rv != null) { return rv; }

        long currTime = System.currentTimeMillis();
        if ((currTime - timePrev) > (15 * 1000)) {
          timePrev = currTime;
          logger.warn("Still waiting to get the Object from local cache, ObjectID=" + oid + " , times spent="
                      + ((currTime - startTime) / 1000) + "seconds");
        }
        try {
          waitUntilNotified();
        } catch (InterruptedException e) {
          interrupted = true;
        }
        // Retry to get the object id.
        // interrupt outside while loop for ENG-263, for not entering a tight loop.
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void waitUntilNotified() throws InterruptedException {
    isShutdownThenException();
    if (isRejoinInProgress) { throw new PlatformRejoinException(); }
    try {
      // since i know I am going to wait, let me wait on client lock manager instead of this condition
      synchronized (tcObjectSelfRemovedFromStoreCallback) {
        tcObjectSelfRemovedFromStoreCallback.wait(1000);
      }
    } finally {
      isShutdownThenException();
      if (isRejoinInProgress) { throw new PlatformRejoinException(); }
    }
  }

  private void initTCObjectSelfIfRequired(Object rv) {
    if (rv instanceof TCObjectSelf) {
      initializeTCObjectSelfIfRequired((TCObjectSelf) rv);
    }
  }

  @Override
  public void initializeTCObjectSelfIfRequired(TCObjectSelf tcoSelf) {
    isShutdownThenException();

    if (tcoSelf != null) {
      tcObjectSelfRemovedFromStoreCallback.initializeTCClazzIfRequired(tcoSelf);
    }
  }

  @Override
  public void addTCObjectSelfTemp(TCObjectSelf tcObjectSelf) {
    isShutdownThenException();

    tcObjectStoreLock.writeLock().lock();
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("XXX Adding TCObjectSelf to temp cache, ObjectID=" + tcObjectSelf.getObjectID());
      }
      this.tcObjectSelfTempCache.put(tcObjectSelf.getObjectID(), tcObjectSelf);
    } finally {
      tcObjectStoreLock.writeLock().unlock();
    }
  }

  @Override
  public boolean addTCObjectSelf(L1ServerMapLocalCacheStore store, AbstractLocalCacheStoreValue localStoreValue,
                                 Object tcoself, final boolean isNew) {
    isShutdownThenException();

    synchronized (tcObjectSelfRemovedFromStoreCallback) {
      tcObjectStoreLock.writeLock().lock();
      try {
        if (tcoself instanceof TCObject) {
          // no need of instanceof check if tcoself is declared as TCObject only... skipping for tests.. refactor later
          ObjectID oid = ((TCObject) tcoself).getObjectID();
          if (logger.isDebugEnabled()) {
            logger.debug("XXX Adding TCObjectSelf to Store if necessary, ObjectID=" + oid);
          }

          if (isNew || (tcObjectSelfTempCache.containsKey(oid) && !tcObjectSelfStoreOids.contains(oid))) {
            tcObjectSelfStoreOids.add(localStoreValue.isEventualConsistentValue(), oid);
            removeTCObjectSelfTemp((TCObjectSelf) tcoself, false);
            return true;
          } else {
            return false;
          }
        }
      } finally {
        tcObjectStoreLock.writeLock().unlock();
      }
    }

    return true;
  }

  @Override
  public void removeTCObjectSelfTemp(TCObjectSelf objectSelf, boolean notifyServer) {
    isShutdownThenException();

    if (objectSelf == null) { return; }

    synchronized (tcObjectSelfRemovedFromStoreCallback) {
      tcObjectStoreLock.writeLock().lock();
      try {
        Object removedValue = tcObjectSelfTempCache.remove(objectSelf.getObjectID());
        if (removedValue != null) {
          if (notifyServer) {
            if (logger.isDebugEnabled()) {
              logger.debug("XXX Removing TCObjectSelf from temp cache, ObjectID=" + objectSelf.getObjectID());
            }
            tcObjectSelfRemovedFromStoreCallback.removedTCObjectSelfFromStore(objectSelf);
          }
        }
      } finally {
        tcObjectStoreLock.writeLock().unlock();
      }
    }

  }

  @Override
  public void removeTCObjectSelf(AbstractLocalCacheStoreValue localStoreValue) {
    isShutdownThenException();

    synchronized (tcObjectSelfRemovedFromStoreCallback) {
      tcObjectStoreLock.writeLock().lock();

      try {
        if (!(localStoreValue.getValueObject() instanceof TCObjectSelf)) { return; }
        TCObjectSelf self = (TCObjectSelf) localStoreValue.getValueObject();
        ObjectID valueOid = self.getObjectID();

        if (ObjectID.NULL_ID.equals(valueOid) || !tcObjectSelfStoreOids.contains(valueOid)) {
          if (logger.isDebugEnabled()) {
            logger.debug("XXX Removing TCObjectSelf from Store Failed, ObjectID=" + valueOid
                         + " , TCObjectSelfStore contains it = " + tcObjectSelfStoreOids.contains(valueOid));
          }
          return;
        }

        tcObjectSelfRemovedFromStoreCallback.removedTCObjectSelfFromStore(self);
        tcObjectSelfStoreOids.remove(localStoreValue.isEventualConsistentValue(), valueOid);
      } finally {
        tcObjectStoreLock.writeLock().unlock();
      }
      this.tcObjectSelfRemovedFromStoreCallback.notifyAll();
    }
  }

  @Override
  public void removeTCObjectSelf(TCObjectSelf self) {
    isShutdownThenException();

    synchronized (tcObjectSelfRemovedFromStoreCallback) {
      tcObjectStoreLock.writeLock().lock();

      try {
        ObjectID valueOid = self.getObjectID();

        if (ObjectID.NULL_ID.equals(valueOid) || !tcObjectSelfStoreOids.contains(valueOid)) {
          if (logger.isDebugEnabled()) {
            logger.debug("XXX Removing TCObjectSelf from Store Failed, ObjectID=" + valueOid
                         + " , TCObjectSelfStore contains it = " + tcObjectSelfStoreOids.contains(valueOid));
          }
          return;
        }

        tcObjectSelfRemovedFromStoreCallback.removedTCObjectSelfFromStore(self);
        tcObjectSelfStoreOids.remove(valueOid);
      } finally {
        tcObjectStoreLock.writeLock().unlock();
      }
      this.tcObjectSelfRemovedFromStoreCallback.notifyAll();
    }
  }

  @Override
  public void addAllObjectIDsToValidate(Invalidations invalidations, NodeID remoteNode) {
    isShutdownThenException();

    tcObjectStoreLock.writeLock().lock();
    try {
      tcObjectSelfStoreOids.addAllObjectIDsToValidate(invalidations, remoteNode);
      int grpID = ((GroupID) remoteNode).toInt();
      for (ObjectID id : tcObjectSelfTempCache.keySet()) {
        if (id.getGroupID() == grpID) {
          invalidations.add(ObjectID.NULL_ID, id);
        }
      }
    } finally {
      tcObjectStoreLock.writeLock().unlock();
    }
  }

  @Override
  public int size() {
    isShutdownThenException();

    tcObjectStoreLock.readLock().lock();
    try {
      return tcObjectSelfStoreOids.size();
    } finally {
      tcObjectStoreLock.readLock().unlock();
    }
  }

  @Override
  public void addAllObjectIDs(Set oids) {
    isShutdownThenException();

    tcObjectStoreLock.readLock().lock();
    try {
      tcObjectSelfStoreOids.addAll(oids);
      oids.addAll(tcObjectSelfTempCache.keySet());
    } finally {
      tcObjectStoreLock.readLock().unlock();
    }
  }

  @Override
  public boolean contains(ObjectID objectID) {
    isShutdownThenException();

    tcObjectStoreLock.readLock().lock();
    try {
      return this.tcObjectSelfTempCache.containsKey(objectID) || this.tcObjectSelfStoreOids.contains(objectID);
    } finally {
      tcObjectStoreLock.readLock().unlock();
    }
  }

  @Override
  public void initializeTCObjectSelfStore(TCObjectSelfCallback callback) {
    this.tcObjectSelfRemovedFromStoreCallback = callback;
  }

  @Override
  public void rejoinInProgress(boolean rejoinInProgress) {
    this.isRejoinInProgress = rejoinInProgress;
  }

  @Override
  public void shutdown(boolean fromShutdownHook) {
    this.isShutdown = true;
  }

  private static class TCObjectSelfStoreObjectIDSet {
    private final ObjectIDSet nonEventualIds = new ObjectIDSet();
    private final ObjectIDSet eventualIds    = new ObjectIDSet();

    public void clear() {
      nonEventualIds.clear();
      eventualIds.clear();
    }

    public void add(boolean isEventual, ObjectID id) {
      if (isEventual) {
        eventualIds.add(id);
      } else {
        nonEventualIds.add(id);
      }
    }

    public void remove(ObjectID id) {
      if (!eventualIds.remove(id)) {
        nonEventualIds.remove(id);
      }
    }

    public void remove(boolean isEventual, ObjectID id) {
      if (isEventual) {
        eventualIds.remove(id);
      } else {
        nonEventualIds.remove(id);
      }
    }

    public int size() {
      return eventualIds.size() + nonEventualIds.size();
    }

    public boolean contains(ObjectID id) {
      return eventualIds.contains(id) || nonEventualIds.contains(id);
    }

    public void addAllObjectIDsToValidate(Invalidations invalidations, NodeID remoteNode) {
      int grpID = ((GroupID) remoteNode).toInt();
      for (ObjectID id : eventualIds) {
        if (id.getGroupID() == grpID) {
          invalidations.add(ObjectID.NULL_ID, id);
        }
      }
    }

    public void addAll(Set oids) {
      oids.addAll(eventualIds);
      oids.addAll(nonEventualIds);
    }
  }

}
