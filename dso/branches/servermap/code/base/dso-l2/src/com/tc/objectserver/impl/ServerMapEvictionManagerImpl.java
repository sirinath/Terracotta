/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictableObject;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ServerMapEvictionManager;
import com.tc.objectserver.context.ServerMapEvictionContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.api.PersistentCollectionsUtil;
import com.tc.util.ObjectIDSet;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerMapEvictionManagerImpl implements ServerMapEvictionManager {

  private static final TCLogger                logger             = TCLogging
                                                                      .getLogger(ServerMapEvictionManagerImpl.class);

  // 15 Minutes
  public static final long                     DEFAULT_SLEEP_TIME = 1 * 60000;

  private final ObjectManager                  objectManager;
  private final ManagedObjectStore             objectStore;
  private final ClientStateManager             clientStateManager;
  private final PersistenceTransactionProvider transactionStorePTP;
  private final long                           evictionSleepTime;
  private final AtomicBoolean                  isStarted          = new AtomicBoolean(false);
  private final Timer                          evictor            = new Timer("Server Map Evictor", true);

  private Sink                                 evictorSink;

  public ServerMapEvictionManagerImpl(final ObjectManager objectManager, final ManagedObjectStore objectStore,
                                      final ClientStateManager clientStateManager, final long evictionSleepTime,
                                      final PersistenceTransactionProvider transactionStorePTP) {
    this.objectManager = objectManager;
    this.objectStore = objectStore;
    this.clientStateManager = clientStateManager;
    this.evictionSleepTime = 60000;
    this.transactionStorePTP = transactionStorePTP;
  }

  public void initializeContext(final ConfigurationContext context) {
    this.evictorSink = context.getStage(ServerConfigurationContext.SERVER_MAP_EVICTION_PROCESSOR_STAGE).getSink();
  }

  public void startEvictor() {
    if (!this.isStarted.getAndSet(true)) {
      logger.info("Server Map Eviction : Evictor will run every " + this.evictionSleepTime + " ms");
      this.evictor.schedule(new EvictorTask(this), this.evictionSleepTime, this.evictionSleepTime);
    }
  }

  public void runEvictor() {
    logger.info("Server Map Eviction  : Started ");
    final ObjectIDSet evictableObjects = this.objectStore.getAllEvictableObjectIDs();
    logger.info("Server Map Eviction  : Number of Evictable : " + evictableObjects.size());
    final ObjectIDSet faultedInClients = new ObjectIDSet();
    this.clientStateManager.addAllReferencedIdsTo(faultedInClients);
    logger.info("Server Map Eviction  : Number of Objects faulted in L1 : " + faultedInClients.size());
    for (final ObjectID mapID : evictableObjects) {
      doEvictionOn(mapID, faultedInClients);
    }
  }

  public void doEvictionOn(final ObjectID oid, final SortedSet<ObjectID> faultedInClients) {
    final ManagedObject mo = this.objectManager.getObjectByID(oid);
    try {
      final EvictableObject ev = getEvictableObjectFrom(mo);
      doEviction(oid, ev, faultedInClients);
    } finally {
      this.objectManager.releaseReadOnly(mo);
    }
  }

  private EvictableObject getEvictableObjectFrom(final ManagedObject mo) {
    final ManagedObjectState state = mo.getManagedObjectState();
    if (!PersistentCollectionsUtil.isEvictableObjectType(state.getType())) { throw new AssertionError(
                                                                                                      "Received wrong object thats not evictable : "
                                                                                                          + mo.getID()
                                                                                                          + " : " + mo); }
    return (EvictableObject) state;
  }

  private void doEviction(final ObjectID oid, final EvictableObject ev, final SortedSet<ObjectID> faultedInClients) {
    final int targetMaxTotalCount = ev.getMaxTotalCount();
    final int currentSize = ev.getSize();
    if (targetMaxTotalCount <= 0 || currentSize <= targetMaxTotalCount) { return; }
    final int overshoot = currentSize - targetMaxTotalCount;
    logger.info("Server Map Eviction  : Trying to evict : " + oid + " overshoot : " + overshoot + " : current Size : "
                + currentSize + " : target max : " + targetMaxTotalCount);

    final int ttl = ev.getTTLSeconds();
    final int tti = ev.getTTISeconds();
    final Map samples = ev.getRandomSamples((int) (overshoot * 1.5), faultedInClients);

    logger
        .info("Server Map Eviction  : Got Random samples to evict : " + oid + " : Random Samples : " + samples.size());

    final ServerMapEvictionContext context = new ServerMapEvictionContext(oid, targetMaxTotalCount, tti, ttl, samples,
                                                                          overshoot);
    this.evictorSink.add(context);
  }

  public void evict(final ObjectID oid, final Map samples, final int targetMaxTotalCount, final int ttiSeconds,
                    final int ttlSeconds, final int overshoot) {
    final HashMap candidates = new HashMap();
    int cantEvict = 0;
    for (final Iterator iterator = samples.entrySet().iterator(); candidates.size() < overshoot && iterator.hasNext();) {
      final Entry e = (Entry) iterator.next();
      if (canEvict(e.getValue(), ttiSeconds, ttlSeconds)) {
        candidates.put(e.getKey(), e.getValue());
      } else {
        if (++cantEvict % 1000 == 0) {
          logger.info("Server Map Eviction  : Can't Evict " + cantEvict + " Candidates so far : " + candidates.size()
                      + " Samples : " + samples.size());
        }
      }
    }
    evictFrom(oid, candidates);
  }

  private void evictFrom(final ObjectID oid, final HashMap candidates) {
    logger.info("Server Map Eviction  : Evicting " + oid + " Candidates : " + candidates.size());
    final ManagedObject mo = this.objectManager.getObjectByID(oid);
    try {
      final EvictableObject ev = getEvictableObjectFrom(mo);
      ev.evict(candidates);
    } finally {
      releaseAndCommit(mo);
    }
    logger.info("Server Map Eviction  : Evicted " + candidates.size() + " from " + oid);

  }

  private void releaseAndCommit(final ManagedObject mo) {
    final PersistenceTransaction txn = this.transactionStorePTP.newTransaction();
    this.objectManager.release(txn, mo);
    txn.commit();
  }

  // TODO:: check TTI and TTL
  private boolean canEvict(final Object value, final int ttiSeconds, final int ttlSeconds) {
    return true;
  }

  private static class EvictorTask extends TimerTask {
    private final ServerMapEvictionManager serverMapEvictionMgr;

    public EvictorTask(final ServerMapEvictionManager serverMapEvictionMgr) {
      this.serverMapEvictionMgr = serverMapEvictionMgr;
    }

    @Override
    public void run() {
      this.serverMapEvictionMgr.runEvictor();
    }
  }
}
