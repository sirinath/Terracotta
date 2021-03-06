/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import org.mockito.Mockito;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.AddPredicate;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.exception.ImplementMe;
import com.tc.exception.TCNotRunningException;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.TCObjectSelf;
import com.tc.object.TCObjectSelfCallback;
import com.tc.object.context.LocksToRecallContext;
import com.tc.object.handler.LockRecallHandler;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LocksRecallService;
import com.tc.object.locks.LocksRecallServiceImpl;
import com.tc.object.locks.LongLockID;
import com.tc.object.locks.MockClientLockManager;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.LocalCacheStoreEventualValue;
import com.tc.object.servermap.localcache.MapOperationType;
import com.tc.object.servermap.localcache.PinnedEntryFaultCallback;
import com.tc.object.servermap.localcache.ServerMapLocalCache;
import com.tc.stats.Stats;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;
import junit.framework.TestCase;

public class L1ServerMapLocalCacheManagerImplTest extends TestCase {
  private L1ServerMapLocalCacheManagerImpl l1LocalCacheManagerImpl;
  private MockClientLockManager            clientLockManager;
  private MySink                           testSink;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    clientLockManager = new MockClientLockManager();

    ClientConfigurationContext configurationContext = Mockito.mock(ClientConfigurationContext.class);
    Mockito.when(configurationContext.getLockManager()).thenReturn(clientLockManager);

    LockRecallHandler lockRecallHandler = new LockRecallHandler();
    lockRecallHandler.initialize(configurationContext);

    testSink = new MySink(new MyHandler(lockRecallHandler, new L1ServerMapCapacityEvictionHandler()));
    Stage lockRecallStage = Mockito.mock(Stage.class);
    Mockito.when(lockRecallStage.getSink()).thenReturn(testSink);

    LocksRecallService locksRecallHelper = new LocksRecallServiceImpl(lockRecallHandler, lockRecallStage);
    this.l1LocalCacheManagerImpl = new L1ServerMapLocalCacheManagerImpl(locksRecallHelper, testSink,
                                                                        new TxnCompleteSink(), Mockito.mock(Sink.class));
    this.l1LocalCacheManagerImpl.setLockManager(clientLockManager);
  }

  public void testTCNotRunning() {
    this.l1LocalCacheManagerImpl.initializeTCObjectSelfStore(Mockito.mock(TCObjectSelfCallback.class));
    ObjectID mapId = new ObjectID(1000);
    ObjectID oid = new ObjectID(1001);
    L1ServerMapLocalCacheStoreHashMap serverMapLocalStore = new L1ServerMapLocalCacheStoreHashMap();
    this.l1LocalCacheManagerImpl.getOrCreateLocalCache(mapId, Mockito.mock(ClientObjectManager.class), null, true,
                                                       serverMapLocalStore,
                                                       Mockito.mock(PinnedEntryFaultCallback.class));
    TCObjectSelf self = Mockito.mock(TCObjectSelf.class);
    Mockito.when(self.getObjectID()).thenReturn(oid);
    AbstractLocalCacheStoreValue localStoreValue = new LocalCacheStoreEventualValue(oid, self);

    this.l1LocalCacheManagerImpl.addTCObjectSelf(serverMapLocalStore, localStoreValue, self, true);
    serverMapLocalStore.put(oid, "my-key");

    this.l1LocalCacheManagerImpl.shutdown();

    boolean expectedException = false;
    try {
      this.l1LocalCacheManagerImpl.getById(oid);
    } catch (TCNotRunningException e) {
      expectedException = true;
    }

    Assert.assertTrue(expectedException);
  }

  public void testInitiateRecall() {
    LockID lockID = new LongLockID(500);
    Set<LockID> lockIDs = Collections.singleton(lockID);

    l1LocalCacheManagerImpl.recallLocks(lockIDs);
    testSink.waitUntilContextsAddedEqualsAndCompletedEquals(1, 1);

    Assert.assertEquals(1, clientLockManager.getRecallList().size());
    Assert.assertEquals(lockID, clientLockManager.getRecallList().get(0));
  }

  public void testInlineRecall() {
    LockID lockID = new LongLockID(500);
    Set<LockID> lockIDs = Collections.singleton(lockID);

    l1LocalCacheManagerImpl.recallLocksInline(lockIDs);

    Assert.assertEquals(1, clientLockManager.getRecallList().size());
    Assert.assertEquals(lockID, clientLockManager.getRecallList().get(0));
  }

  public void testRememberMapIDToLockID() {
    L1ServerMapLocalCacheStore store = new L1ServerMapLocalCacheStoreHashMap();
    ObjectID mapID = new ObjectID(100);
    ServerMapLocalCache localCache = this.l1LocalCacheManagerImpl.getOrCreateLocalCache(mapID, Mockito
        .mock(ClientObjectManager.class), null, true, store, Mockito.mock(PinnedEntryFaultCallback.class));
    localCache.setLocalCacheEnabled(true);

    MockTCObjectSelfCallback tcObjectSelfCallback = new MockTCObjectSelfCallback();
    this.l1LocalCacheManagerImpl.initializeTCObjectSelfStore(tcObjectSelfCallback);

    LockID lockID = new LongLockID(100);

    MockModesAdd.addStrongValueToCache(localCache, l1LocalCacheManagerImpl, "key", lockID,
                                       MockModesAdd.createMockSerializedEntry(12345), mapID, MapOperationType.GET);

    this.l1LocalCacheManagerImpl.removeEntriesForLockId(lockID);

    System.err.println("Object IDs removed = " + tcObjectSelfCallback.getRemovedSet());

    Assert.assertEquals(0, store.size());
    Assert.assertEquals(0, localCache.size());
  }

  private static class MySink implements Sink {
    private final LinkedBlockingQueue<EventContext> q                 = new LinkedBlockingQueue<EventContext>();
    private final AtomicInteger                     contextsAdded     = new AtomicInteger(0);
    private final AtomicInteger                     contextsCompleted = new AtomicInteger(0);

    public MySink(final AbstractEventHandler handler) {
      Runnable runnable = new Runnable() {
        @Override
        public void run() {
          while (true) {
            EventContext context;
            try {
              context = q.take();
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
            try {
              handler.handleEvent(context);
            } catch (EventHandlerException e) {
              throw new RuntimeException(e);
            }
            contextsCompleted.incrementAndGet();
          }
        }
      };

      Thread t1 = new Thread(runnable, "MySink thread");
      t1.start();
    }

    @Override
    public void add(EventContext context) {
      try {
        q.put(context);
        contextsAdded.incrementAndGet();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    public void waitUntilContextsAddedEqualsAndCompletedEquals(int expectedContextsAdded, int expectedContextsCompleted) {
      waitUntilAtomicIntegerReaches("expectedContextsAdded", contextsAdded, expectedContextsAdded);
      waitUntilAtomicIntegerReaches("expectedContextsCompleted", contextsCompleted, expectedContextsCompleted);

      Assert.assertEquals(expectedContextsAdded, contextsAdded.get());
      Assert.assertEquals(expectedContextsCompleted, contextsCompleted.get());
    }

    private void waitUntilAtomicIntegerReaches(String integerString, AtomicInteger integer, int expected) {
      while (integer.get() != expected) {
        System.err.println("Sleep for 1 seconds for " + integerString + " Current values = " + integer.get()
                           + " expected = " + expected);
        ThreadUtil.reallySleep(1 * 1000);
      }
    }

    @Override
    public boolean addLossy(EventContext context) {
      throw new ImplementMe();
    }

    @Override
    public void addMany(Collection contexts) {
      throw new ImplementMe();
    }

    @Override
    public void clear() {
      throw new ImplementMe();
    }

    @Override
    public AddPredicate getPredicate() {
      throw new ImplementMe();
    }

    @Override
    public void setAddPredicate(AddPredicate predicate) {
      throw new ImplementMe();
    }

    @Override
    public int size() {
      throw new ImplementMe();
    }

    @Override
    public void enableStatsCollection(boolean enable) {
      throw new ImplementMe();
    }

    @Override
    public Stats getStats(long frequency) {
      throw new ImplementMe();
    }

    @Override
    public Stats getStatsAndReset(long frequency) {
      throw new ImplementMe();
    }

    @Override
    public boolean isStatsCollectionEnabled() {
      throw new ImplementMe();
    }

    @Override
    public void resetStats() {
      throw new ImplementMe();
    }

  }

  private static class MyHandler extends AbstractEventHandler {

    private final LockRecallHandler lockRecallHandler;

    public MyHandler(LockRecallHandler lockRecallHandler,
                     L1ServerMapCapacityEvictionHandler l1ServerMapCapacityEvictionHandler) {
      this.lockRecallHandler = lockRecallHandler;
    }

    @Override
    public void handleEvent(EventContext context) {
      if (context instanceof LocksToRecallContext) {
        lockRecallHandler.handleEvent(context);
      }
    }

  }

  private static class TxnCompleteSink implements Sink {
    L1ServerMapTransactionCompletionHandler completionHandler = new L1ServerMapTransactionCompletionHandler();

    @Override
    public void add(EventContext context) {
      completionHandler.handleEvent(context);
    }

    @Override
    public boolean addLossy(EventContext context) {
      throw new ImplementMe();
    }

    @Override
    public void addMany(Collection contexts) {
      throw new ImplementMe();

    }

    @Override
    public void clear() {
      throw new ImplementMe();

    }

    @Override
    public AddPredicate getPredicate() {
      throw new ImplementMe();
    }

    @Override
    public void setAddPredicate(AddPredicate predicate) {
      throw new ImplementMe();

    }

    @Override
    public int size() {
      throw new ImplementMe();
    }

    @Override
    public void enableStatsCollection(boolean enable) {
      throw new ImplementMe();

    }

    @Override
    public Stats getStats(long frequency) {
      throw new ImplementMe();
    }

    @Override
    public Stats getStatsAndReset(long frequency) {
      throw new ImplementMe();
    }

    @Override
    public boolean isStatsCollectionEnabled() {
      throw new ImplementMe();
    }

    @Override
    public void resetStats() {
      throw new ImplementMe();

    }

  }
}
