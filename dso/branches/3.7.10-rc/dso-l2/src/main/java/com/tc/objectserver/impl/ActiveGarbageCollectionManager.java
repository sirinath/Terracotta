/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.Sink;
import com.tc.l2.context.StateChangedEvent;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.GarbageCollectionManager;
import com.tc.objectserver.context.GarbageCollectContext;
import com.tc.objectserver.context.InlineGCContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSetChangedListener;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TxnsInSystemCompletionListener;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.ObjectIDSet;
import com.tc.util.TCCollections;

import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

public class ActiveGarbageCollectionManager implements GarbageCollectionManager {
  private static final TCLogger                 logger                 = TCLogging
                                                                           .getLogger(GarbageCollectionManager.class);
  private static final int                      OBJECT_RETRY_THRESHOLD = 100000;
  private static final long                     INLINE_GC_INTERVAL     = SECONDS
                                                                           .toNanos(TCPropertiesImpl
                                                                               .getProperties()
                                                                               .getLong(TCPropertiesConsts.L2_OBJECTMANAGER_DGC_INLINE_INTERVAL_SECONDS,
                                                                                        10));
  private static final long                     MAX_INLINE_GC_OBJECTS  = TCPropertiesImpl
                                                                           .getProperties()
                                                                           .getLong(TCPropertiesConsts.L2_OBJECTMANAGER_DGC_INLINE_MAX_OBJECTS,
                                                                                    10000);
  private static final long                     INLINE_DGC_MIN_DELAY   = TCPropertiesImpl
                                                                           .getProperties()
                                                                           .getLong(TCPropertiesConsts.L2_OBJECTMANAGER_DGC_INLINE_DELETE_DELAY_SECONDS,
                                                                                    0);
  public static final InlineGCContext           INLINE_GC_CONTEXT      = new InlineGCContext();

  private final DelayedReleaseObjectIDSetHolder objectsToDelete        = new DelayedReleaseObjectIDSetHolder(
                                                                                                             INLINE_DGC_MIN_DELAY,
                                                                                                             TimeUnit.SECONDS);
  private ObjectIDSet                           objectsToRetry         = new ObjectIDSet();
  private long                                  lastInlineGCTime       = System.nanoTime();
  private final Sink                            garbageCollectSink;
  private final ClientObjectReferenceSet        clientObjectReferenceSet;

  private ServerTransactionManager              transactionManager;
  private GarbageCollector                      garbageCollector;

  public ActiveGarbageCollectionManager(final Sink garbageCollectSink,
                                        final ClientObjectReferenceSet clientObjectReferenceSet) {
    this.garbageCollectSink = garbageCollectSink;
    this.clientObjectReferenceSet = clientObjectReferenceSet;
    clientObjectReferenceSet.addReferenceSetChangeListener(new ClientObjectReferenceSetChangedListener() {
      public void notifyReferenceSetChanged() {
        retryDeletingReferencedObjects();
      }
    });
  }

  public void deleteObjects(SortedSet<ObjectID> objects) {
    if (!objects.isEmpty()) {
      synchronized (this) {
        objectsToDelete.addAll(objects);
        scheduleInlineGarbageCollectionIfNecessary();
      }
    }
  }

  public synchronized ObjectIDSet nextObjectsToDelete() {
    ObjectIDSet deleteNow = objectsToDelete.get();
    if (deleteNow.isEmpty()) { return TCCollections.EMPTY_OBJECT_ID_SET; }
    Iterator<ObjectID> oidIterator = deleteNow.iterator();
    int objectsRetried = 0;
    while (oidIterator.hasNext()) {
      ObjectID oid = oidIterator.next();
      if (clientObjectReferenceSet.contains(oid)) {
        objectsRetried++;
        objectsToRetry.add(oid);
        oidIterator.remove();
      }
    }
    if (objectsRetried > OBJECT_RETRY_THRESHOLD) {
      logger.warn("Large number of referenced objects requiring retry (" + objectsRetried + ").");
    }
    return deleteNow;
  }

  public synchronized void scheduleInlineGarbageCollectionIfNecessary() {
    if (!objectsToDelete.isEmpty() && System.nanoTime() - lastInlineGCTime > INLINE_GC_INTERVAL
        || objectsToDelete.currentBatchSize() >= MAX_INLINE_GC_OBJECTS) {
      if (garbageCollectSink.addLossy(INLINE_GC_CONTEXT)) {
        lastInlineGCTime = System.nanoTime();
      }
    }
  }

  public void scheduleGarbageCollection(final GCType type, final long delay) {
    transactionManager.callBackOnResentTxnsInSystemCompletion(new TxnsInSystemCompletionListener() {
      public void onCompletion() {
        garbageCollectSink.add(new GarbageCollectContext(type, delay));
      }
    });
  }

  public void doGarbageCollection(GCType type) {
    GarbageCollectContext gcc = new GarbageCollectContext(type);
    scheduleGarbageCollection(type);
    gcc.waitForCompletion();
  }

  public void scheduleGarbageCollection(final GCType type) {
    transactionManager.callBackOnResentTxnsInSystemCompletion(new TxnsInSystemCompletionListener() {
      public void onCompletion() {
        garbageCollectSink.add(new GarbageCollectContext(type));
      }
    });
  }

  private synchronized void retryDeletingReferencedObjects() {
    if (!objectsToRetry.isEmpty()) {
      deleteObjects(objectsToRetry);
      objectsToRetry = new ObjectIDSet();
    }
  }

  public void initializeContext(ConfigurationContext context) {
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    transactionManager = scc.getTransactionManager();
    garbageCollector = scc.getObjectManager().getGarbageCollector();
  }

  public void scheduleInlineCleanupIfNecessary() {
    if (!garbageCollector.isPeriodicEnabled()
        && TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.L2_OBJECTMANAGER_DGC_INLINE_ENABLED, true)) {
      scheduleGarbageCollection(GCType.INLINE_CLEANUP_GC);
    }
  }

  public void l2StateChanged(StateChangedEvent sce) {
    // Do nothing
  }

  private static class DelayedReleaseObjectIDSetHolder {
    private final long     delay;
    private final TimeUnit unit;

    private long           lastReleaseTime;
    private ObjectIDSet    out;
    private ObjectIDSet    hold;
    private ObjectIDSet    in;

    DelayedReleaseObjectIDSetHolder(long delay, TimeUnit unit) {
      this.delay = delay;
      this.unit = unit;
      lastReleaseTime = System.nanoTime();
      out = new ObjectIDSet();
      hold = new ObjectIDSet();
      in = new ObjectIDSet();
    }

    int currentBatchSize() {
      return inputSet().size();
    }

    boolean isEmpty() {
      return out.isEmpty() && hold.isEmpty() && in.isEmpty();
    }

    ObjectIDSet get() {
      ObjectIDSet r = new ObjectIDSet(out);
      if (System.nanoTime() - lastReleaseTime > unit.toNanos(delay)) {
        lastReleaseTime = System.nanoTime();
        out = hold;
        hold = in;
        in = new ObjectIDSet();
      } else {
        out.clear();
      }
      return r;
    }

    void addAll(Collection<ObjectID> ids) {
      inputSet().addAll(ids);
    }

    ObjectIDSet inputSet() {
      if (delay <= 0) {
        return out;
      } else {
        return in;
      }
    }
  }
}
