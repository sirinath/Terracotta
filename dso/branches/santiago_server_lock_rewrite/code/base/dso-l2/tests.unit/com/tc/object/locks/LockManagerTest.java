/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.locks;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.exception.TCLockUpgradeNotSupportedError;
import com.tc.management.L2LockStatsManager;
import com.tc.management.lock.stats.L2LockStatisticsManagerImpl;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.lockmanager.api.ServerThreadID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.locks.ServerLockContext.State;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.lockmanager.api.DeadlockChain;
import com.tc.objectserver.lockmanager.api.DeadlockResults;
import com.tc.objectserver.lockmanager.api.NullChannelManager;
import com.tc.objectserver.locks.Lock;
import com.tc.objectserver.locks.LockFactory;
import com.tc.objectserver.locks.LockManagerImpl;
import com.tc.objectserver.locks.LockResponseContext;
import com.tc.objectserver.locks.NonGreedyPolicyLock;
import com.tc.text.Banner;
import com.tc.util.concurrent.ThreadUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import junit.framework.TestCase;

/**
 * @author steve
 */
public class LockManagerTest extends TestCase {
  private TestSink               sink;
  private LockManagerImpl        lockManager;
  private final Random           random     = new Random();

  final int                      numLocks   = 30;
  final int                      numThreads = 15;
  private final LockID[]         locks      = makeUniqueLocks(numLocks);
  private final ServerThreadID[] txns       = makeUniqueTxns(numThreads);

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    sink = new TestSink();
    resetLockManager();
  }

  private void resetLockManager() {
    resetLockManager(false);
  }

  private void resetLockManager(boolean start) {
    if (lockManager != null) {
      try {
        lockManager.stop();
      } catch (InterruptedException e) {
        fail();
      }
    }

    lockManager = new LockManagerImpl(sink, L2LockStatsManager.NULL_LOCK_STATS_MANAGER, new NullChannelManager(),
                                      new TestServerLockFactory());
    if (start) {
      lockManager.start();
    }
  }

  @Override
  protected void tearDown() throws Exception {
    assertEquals(0, lockManager.getLockCount());
    super.tearDown();
  }

  public void testLockMBean() {
    final ClientID cid1 = new ClientID(1);
    final ClientID cid2 = new ClientID(2);

    LockID lid1 = new StringLockID("1");
    LockID lid2 = new StringLockID("2");
    LockID lid3 = new StringLockID("3");
    ThreadID tid1 = new ThreadID(1);
    ThreadID tid2 = new ThreadID(2);
    L2LockStatsManager lockStatsManager = new L2LockStatisticsManagerImpl();
    lockManager = new LockManagerImpl(sink, lockStatsManager, new NullChannelManager() {
      @Override
      public String getChannelAddress(NodeID nid) {
        if (cid1.equals(nid)) { return "127.0.0.1:6969"; }
        return "no longer connected";
      }
    }, new TestServerLockFactory());

    lockManager.start();
    lockStatsManager.start(new NullChannelManager(), null);

    lockManager.lock(lid1, cid1, tid1, ServerLockLevel.WRITE); // hold
    lockManager.lock(lid1, cid2, tid2, ServerLockLevel.WRITE); // pending

    lockManager.lock(lid2, cid1, tid1, ServerLockLevel.READ); // hold
    lockManager.lock(lid2, cid2, tid2, ServerLockLevel.READ); // hold
    try {
      lockManager.lock(lid2, cid1, tid1, ServerLockLevel.WRITE); // try upgrade and fail
      throw new AssertionError("Should have thrown an TCLockUpgradeNotSupportedError.");
    } catch (TCLockUpgradeNotSupportedError e) {
      //
    }

    lockManager.lock(lid3, cid1, tid1, ServerLockLevel.WRITE); // hold
    lockManager.wait(lid3, cid1, tid1, Integer.MAX_VALUE); // wait

    // LockMBean[] lockBeans = lockManager.getAllLocks();
    // assertEquals(3, lockBeans.length);
    // sortLocksByID(lockBeans);
    //
    // LockMBean bean1 = lockBeans[0];
    // LockMBean bean2 = lockBeans[1];
    // LockMBean bean3 = lockBeans[2];
    // testSerialize(bean1);
    // testSerialize(bean2);
    // testSerialize(bean3);
    //
    // validateBean1(bean1, start);
    // validateBean2(bean2, start);
    // validateBean3(bean3, start, wait);

    lockManager.clearAllLocksFor(cid1);
    lockManager.clearAllLocksFor(cid2);
  }

  public void testReestablishWait() throws Exception {
    LockID lockID1 = new StringLockID("my lock");
    ClientID nid1 = new ClientID(1);
    ThreadID tx1 = new ThreadID(1);
    ThreadID tx2 = new ThreadID(2);

    try {
      assertEquals(0, lockManager.getLockCount());
      long waitTime = 1000;
      long t0 = System.currentTimeMillis();

      ArrayList<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();
      ClientServerExchangeLockContext context = new ClientServerExchangeLockContext(lockID1, nid1, tx1, State.WAITER,
                                                                                    waitTime);
      contexts.add(context);
      context = new ClientServerExchangeLockContext(lockID1, nid1, tx2, State.WAITER, waitTime);
      contexts.add(context);
      lockManager.reestablishState(nid1, contexts);
      lockManager.start();

      LockResponseContext ctxt = (LockResponseContext) sink.waitForAdd(waitTime * 3);
      assertTrue(ctxt != null);
      assertTrue(System.currentTimeMillis() - t0 >= waitTime);
      assertResponseContext(lockID1, nid1, tx1, ServerLockLevel.WRITE, ctxt);
      assertTrue(ctxt.isLockWaitTimeout());
      ThreadUtil.reallySleep(waitTime * 3);
      assertEquals(3, sink.size()); // 2 wait timeouts and 1 award
      ctxt = (LockResponseContext) sink.take();
      LockResponseContext ctxt1 = (LockResponseContext) sink.take();
      LockResponseContext ctxt2 = (LockResponseContext) sink.take();
      assertTrue((ctxt1.isLockAward() && ctxt2.isLockWaitTimeout())
                 || (ctxt2.isLockAward() && ctxt1.isLockWaitTimeout()));

    } finally {
      lockManager = null;
      resetLockManager();
    }
  }

  public void testReestablishLockAfterReestablishWait() throws Exception {
    LockID lockID1 = new StringLockID("my lock");
    ClientID cid1 = new ClientID(1);
    ThreadID tx1 = new ThreadID(1);
    ThreadID tx2 = new ThreadID(2);
    try {
      assertEquals(0, lockManager.getLockCount());
      ArrayList<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();
      ClientServerExchangeLockContext context = new ClientServerExchangeLockContext(lockID1, cid1, tx1, State.WAITER,
                                                                                    -1);
      contexts.add(context);
      lockManager.reestablishState(cid1, contexts);
      assertEquals(1, lockManager.getLockCount());
      assertEquals(0, sink.getInternalQueue().size());

      // now try to award the lock to the same client-transaction
      try {
        contexts = new ArrayList<ClientServerExchangeLockContext>();
        context = new ClientServerExchangeLockContext(lockID1, cid1, tx1, State.HOLDER_WRITE);
        contexts.add(context);
        lockManager.reestablishState(cid1, contexts);
        fail("Should have thrown an AssertionError.");
      } catch (AssertionError e) {
        // expected
      }
      // now try to reestablish the same lock from a different transaction. It
      // sould succeed
      assertEquals(1, lockManager.getLockCount());
      contexts = new ArrayList<ClientServerExchangeLockContext>();
      context = new ClientServerExchangeLockContext(lockID1, cid1, tx2, State.HOLDER_WRITE);
      contexts.add(context);
      lockManager.reestablishState(cid1, contexts);
    } finally {
      lockManager = null;
      resetLockManager();
    }
  }

  public void testReestablishReadLock() throws Exception {
    LockID lockID1 = new StringLockID("my lock");
    ClientID cid1 = new ClientID(1);
    ThreadID tx1 = new ThreadID(1);
    ThreadID tx2 = new ThreadID(2);
    ThreadID tx3 = new ThreadID(3);

    try {
      assertEquals(0, lockManager.getLockCount());

      ArrayList<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();
      ClientServerExchangeLockContext context = new ClientServerExchangeLockContext(lockID1, cid1, tx1,
                                                                                    State.HOLDER_READ);
      contexts.add(context);
      lockManager.reestablishState(cid1, contexts);
      assertEquals(1, lockManager.getLockCount());

      // now reestablish the same read lock in another transaction. It should
      // succeed.
      contexts = new ArrayList<ClientServerExchangeLockContext>();
      context = new ClientServerExchangeLockContext(lockID1, cid1, tx2, State.HOLDER_READ);
      contexts.add(context);
      lockManager.reestablishState(cid1, contexts);
      assertEquals(1, lockManager.getLockCount());

      // now reestablish the the same write lock. It should fail.
      try {
        contexts = new ArrayList<ClientServerExchangeLockContext>();
        context = new ClientServerExchangeLockContext(lockID1, cid1, tx3, State.HOLDER_WRITE);
        contexts.add(context);
        lockManager.reestablishState(cid1, contexts);

        fail("Should have thrown a LockManagerError.");
      } catch (AssertionError e) {
        //
      }

    } finally {
      // this needs to be done for tearDown() to pass.
      lockManager = null;
      resetLockManager();
    }

    try {
      assertEquals(0, lockManager.getLockCount());
      ArrayList<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();
      ClientServerExchangeLockContext context = new ClientServerExchangeLockContext(lockID1, cid1, tx1,
                                                                                    State.HOLDER_WRITE);
      contexts.add(context);
      lockManager.reestablishState(cid1, contexts);
      assertEquals(1, lockManager.getLockCount());

      // now reestablish a read lock. This should fail.
      try {
        contexts = new ArrayList<ClientServerExchangeLockContext>();
        context = new ClientServerExchangeLockContext(lockID1, cid1, tx2, State.HOLDER_READ);
        contexts.add(context);
        lockManager.reestablishState(cid1, contexts);
        fail("Should have thrown a LockManagerError");
      } catch (AssertionError e) {
        //
      }

    } finally {
      lockManager = null;
      resetLockManager();
    }
  }

  public void testReestablishWriteLock() throws Exception {

    LockID lockID1 = new StringLockID("my lock");
    LockID lockID2 = new StringLockID("my other lock");
    ClientID cid1 = new ClientID(1);
    ClientID cid2 = new ClientID(2);
    ThreadID tx1 = new ThreadID(1);
    ThreadID tx2 = new ThreadID(2);

    try {
      assertEquals(0, lockManager.getLockCount());
      ArrayList<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();
      ClientServerExchangeLockContext context = new ClientServerExchangeLockContext(lockID1, cid1, tx1,
                                                                                    State.HOLDER_WRITE);
      contexts.add(context);
      lockManager.reestablishState(cid1, contexts);
      assertEquals(1, lockManager.getLockCount());

      try {
        contexts = new ArrayList<ClientServerExchangeLockContext>();
        context = new ClientServerExchangeLockContext(lockID1, cid2, tx2, State.HOLDER_WRITE);
        contexts.add(context);
        lockManager.reestablishState(cid2, contexts);
        fail("Expected a LockManagerError!");
      } catch (AssertionError e) {
        //
      }

      // try to reestablish another lock. It should succeed.
      contexts = new ArrayList<ClientServerExchangeLockContext>();
      context = new ClientServerExchangeLockContext(lockID2, cid1, tx1, State.HOLDER_WRITE);
      contexts.add(context);
      lockManager.reestablishState(cid1, contexts);

      lockManager.start();
      // you shouldn't be able to call reestablishLock after the lock manager
      // has started.
      try {
        contexts = new ArrayList<ClientServerExchangeLockContext>();
        context = new ClientServerExchangeLockContext(lockID1, cid1, tx1, State.HOLDER_WRITE);
        contexts.add(context);
        lockManager.reestablishState(cid1, contexts);
        fail("Should have thrown a LockManagerError");
      } catch (Error e) {
        //
      }

    } finally {
      // this needs to be done for tearDown() to pass.
      lockManager = null;
      resetLockManager();
    }
  }

  // private void assertResponseSink(LockID lockID, ChannelID channel, TransactionID tx, int requestedLevel,
  // TestSink responseSink) {
  // assertEquals(1, responseSink.getInternalQueue().size());
  // LockResponseContext ctxt = (LockResponseContext) responseSink.getInternalQueue().get(0);
  // assertResponseContext(lockID, channel, tx, requestedLevel, ctxt);
  // }

  private void assertResponseContext(LockID lockID, NodeID nid, ThreadID tx1, ServerLockLevel requestedLevel,
                                     LockResponseContext ctxt) {
    assertEquals(lockID, ctxt.getLockID());
    assertEquals(nid, ctxt.getNodeID());
    assertEquals(tx1, ctxt.getThreadID());
    assertEquals(requestedLevel, ctxt.getLockLevel());
  }

  public void testWaitTimeoutsIgnoredDuringShutdown() throws InterruptedException {
    ClientID cid = new ClientID(1);
    LockID lockID = new StringLockID("1");
    ThreadID txID = new ThreadID(1);

    lockManager.start();
    lockManager.lock(lockID, cid, txID, ServerLockLevel.WRITE);

    lockManager.wait(lockID, cid, txID, 1000);
    lockManager.stop();

    assertEquals(0, lockManager.getLockCount());

    ThreadUtil.reallySleep(1500);

    assertEquals(0, lockManager.getLockCount());
  }

  public void testOffBlocksUntilNoOutstandingLocksViaWait() throws Exception {
    // this is no longer expected behavior
    if (true) return;
    List queue = sink.getInternalQueue();
    ClientID cid = new ClientID(1);
    LockID lockID = new StringLockID("1");
    ThreadID txID = new ThreadID(1);

    final LinkedQueue shutdownSteps = new LinkedQueue();
    ShutdownThread shutdown = new ShutdownThread(shutdownSteps);

    try {
      lockManager.start();
      lockManager.lock(lockID, cid, txID, ServerLockLevel.WRITE);
      assertEquals(1, queue.size());

      shutdown.start();
      shutdownSteps.take();
      ThreadUtil.reallySleep(500);
      // make sure shutdown didn't complete.
      assertTrue(shutdownSteps.peek() == null);

      // make sure that waiting on an outstanding lock causes the lock to be
      // released and allows shutdown to
      // complete.
      lockManager.wait(lockID, cid, new ThreadID(1), -1);
      shutdownSteps.take();

    } finally {
      lockManager.clearAllLocksFor(cid);
    }
  }

  public void testOffDoesNotBlockUntilNoOutstandingLocksViaUnlock() throws Exception {
    List queue = sink.getInternalQueue();
    ClientID cid1 = new ClientID(1);
    LockID lock1 = new StringLockID("1");
    ThreadID tx1 = new ThreadID(1);

    final LinkedQueue shutdownSteps = new LinkedQueue();
    ShutdownThread shutdown = new ShutdownThread(shutdownSteps);
    try {
      lockManager.start();
      lockManager.lock(lock1, cid1, tx1, ServerLockLevel.WRITE);
      assertEquals(1, queue.size());

      shutdown.start();
      shutdownSteps.take();
      ThreadUtil.reallySleep(1000);
      shutdownSteps.take();
    } finally {
      lockManager = null;
      resetLockManager();
    }
  }

  public void testOffCancelsWaits() throws Exception {
    // implement me.
  }

  public void testOffStopsGrantingNewLocks() throws Exception {
    List queue = sink.getInternalQueue();
    ClientID cid = new ClientID(1);
    LockID lockID = new StringLockID("1");
    ThreadID txID = new ThreadID(1);
    try {
      // Test that the normal case works as expected...
      lockManager.start();
      lockManager.lock(lockID, cid, txID, ServerLockLevel.WRITE);
      assertEquals(1, queue.size());
      queue.clear();
      lockManager.unlock(lockID, cid, txID);
      lockManager.lock(lockID, cid, txID, ServerLockLevel.WRITE);
      assertEquals(1, queue.size());
      lockManager.unlock(lockID, cid, txID);

      // Call shutdown and make sure that the lock isn't granted via the
      // "requestLock" method
      queue.clear();
      lockManager.stop();
      lockManager.lock(lockID, cid, txID, ServerLockLevel.WRITE);
      assertEquals(0, queue.size());
    } finally {
      lockManager.clearAllLocksFor(cid);
    }
  }

  public void testRequestDoesntGrantPendingLocks() throws Exception {
    List queue = sink.getInternalQueue();
    ClientID cid = new ClientID(1);
    LockID lockID = new StringLockID("1");
    ThreadID txID = new ThreadID(1);

    try {
      lockManager.start();
      // now try stacking locks and make sure that calling unlock doesn't grant
      // the pending locks.
      lockManager.lock(lockID, cid, txID, ServerLockLevel.WRITE);
      queue.clear();
      lockManager.lock(lockID, cid, new ThreadID(2), ServerLockLevel.WRITE);
      // the second lock should be pending.
      assertEquals(0, queue.size());
    } finally {
      lockManager = null;
      resetLockManager();
    }
  }

  public void testUnlockIgnoredDuringShutdown() throws Exception {
    List queue = sink.getInternalQueue();
    ClientID cid = new ClientID(1);
    LockID lockID = new StringLockID("1");
    ThreadID txID = new ThreadID(1);
    try {
      lockManager.start();
      // now try stacking locks and make sure that calling unlock doesn't grant
      // the pending locks.
      lockManager.lock(lockID, cid, txID, ServerLockLevel.WRITE);
      queue.clear();
      lockManager.lock(lockID, cid, new ThreadID(2), ServerLockLevel.WRITE);
      // the second lock should be pending.
      assertEquals(0, queue.size());

      lockManager.stop();

      // unlock the first lock
      lockManager.unlock(lockID, cid, txID);
      // the second lock should still be pending
      assertEquals(0, queue.size());

    } finally {
      lockManager = null;
      resetLockManager();
    }
  }

  public void testLockManagerBasics() {
    LockID l1 = new StringLockID("1");
    ClientID c1 = new ClientID(1);
    ThreadID s1 = new ThreadID(0);

    ClientID c2 = new ClientID(2);
    ClientID c3 = new ClientID(3);
    ClientID c4 = new ClientID(4);
    lockManager.start();
    lockManager.lock(l1, c1, s1, ServerLockLevel.WRITE);
    assertTrue(sink.size() == 1);
    System.out.println(sink.getInternalQueue().remove(0));

    lockManager.lock(l1, c2, s1, ServerLockLevel.WRITE);
    assertTrue(sink.size() == 0);
    lockManager.unlock(l1, c1, s1);
    assertTrue(sink.size() == 1);
    System.out.println(sink.getInternalQueue().remove(0));

    lockManager.lock(l1, c3, s1, ServerLockLevel.READ);
    assertTrue(sink.size() == 0);
    assertTrue(lockManager.hasPending(l1));
    lockManager.unlock(l1, c2, s1);
    assertTrue(sink.size() == 1);
    assertFalse(lockManager.hasPending(l1));

    lockManager.lock(l1, c4, s1, ServerLockLevel.WRITE);
    assertTrue(lockManager.hasPending(l1));
    lockManager.unlock(l1, c3, s1);
    assertFalse(lockManager.hasPending(l1));
    lockManager.unlock(l1, c4, s1);
  }

  public void testDeadLock1() {
    if (true) {
      Banner.warnBanner("DEADLOCK tests DISABLED");
      return;
    }

    // A simple deadlock. Thread 1 holds lock1, wants lock2. Thread2 holds
    // lock2, wants lock1

    LockID l1 = new StringLockID("1");
    LockID l2 = new StringLockID("2");
    ClientID c1 = new ClientID(1);

    ThreadID s1 = new ThreadID(1);
    ThreadID s2 = new ThreadID(2);

    ServerThreadID thread1 = new ServerThreadID(c1, s1);
    ServerThreadID thread2 = new ServerThreadID(c1, s2);

    lockManager.start();
    // thread1 gets lock1
    lockManager.lock(l1, c1, s1, ServerLockLevel.WRITE);
    // thread2 gets lock2
    lockManager.lock(l2, c1, s2, ServerLockLevel.WRITE);
    // thread1 trys to get lock2 (blocks)
    lockManager.lock(l2, c1, s1, ServerLockLevel.WRITE);
    // thread2 trys to get lock1 (blocks)
    lockManager.lock(l1, c1, s2, ServerLockLevel.WRITE);

    TestDeadlockResults deadlocks = new TestDeadlockResults();
    lockManager.scanForDeadlocks(deadlocks);

    assertEquals(1, deadlocks.chains.size());
    Map check = new HashMap();
    check.put(thread1, l2);
    check.put(thread2, l1);
    assertSpecificDeadlock((DeadlockChain) deadlocks.chains.get(0), check);

    // test the mgmt interface too
    DeadlockChain[] results = lockManager.scanForDeadlocks();
    assertEquals(1, results.length);
    check = new HashMap();
    check.put(thread1, l2);
    check.put(thread2, l1);
    assertSpecificDeadlock(results[0], check);

    lockManager.clearAllLocksFor(c1);
  }

  public void testDeadLock3() {
    if (true) {
      Banner.warnBanner("DEADLOCK tests DISABLED");
      return;
    }

    // test that includes locks with more than 1 holder

    // contended locks
    LockID l1 = new StringLockID("1");
    LockID l2 = new StringLockID("2");

    // uncontended read locks
    LockID l3 = new StringLockID("3");
    LockID l4 = new StringLockID("4");
    LockID l5 = new StringLockID("5");

    ClientID c1 = new ClientID(1);
    ThreadID s1 = new ThreadID(1);
    ThreadID s2 = new ThreadID(2);

    ServerThreadID thread1 = new ServerThreadID(c1, s1);
    ServerThreadID thread2 = new ServerThreadID(c1, s2);

    lockManager.start();

    // thread1 holds all three read locks, thread2 has 2 of them
    lockManager.lock(l3, c1, s1, ServerLockLevel.READ);
    lockManager.lock(l4, c1, s1, ServerLockLevel.READ);
    lockManager.lock(l5, c1, s1, ServerLockLevel.READ);
    lockManager.lock(l3, c1, s2, ServerLockLevel.READ);
    lockManager.lock(l4, c1, s2, ServerLockLevel.READ);

    // thread1 gets lock1
    lockManager.lock(l1, c1, s1, ServerLockLevel.WRITE);
    // thread2 gets lock2
    lockManager.lock(l2, c1, s2, ServerLockLevel.WRITE);
    // thread1 trys to get lock2 (blocks)
    lockManager.lock(l2, c1, s1, ServerLockLevel.WRITE);
    // thread2 trys to get lock1 (blocks)
    lockManager.lock(l1, c1, s2, ServerLockLevel.WRITE);

    TestDeadlockResults deadlocks = new TestDeadlockResults();
    lockManager.scanForDeadlocks(deadlocks);

    assertEquals(1, deadlocks.chains.size());
    Map check = new HashMap();
    check.put(thread1, l2);
    check.put(thread2, l1);
    assertSpecificDeadlock((DeadlockChain) deadlocks.chains.get(0), check);

    lockManager.clearAllLocksFor(c1);
  }

  public void disableTestUpgradeDeadLock() {
    // Detect deadlock in competing upgrades
    LockID l1 = new StringLockID("L1");

    ClientID c0 = new ClientID(0);
    ThreadID s1 = new ThreadID(1);
    ThreadID s2 = new ThreadID(2);

    ServerThreadID thread1 = new ServerThreadID(c0, s1);
    ServerThreadID thread2 = new ServerThreadID(c0, s2);

    lockManager.start();

    // thread1 gets lock1 (R)
    lockManager.lock(l1, c0, s1, ServerLockLevel.READ);
    // thread2 gets lock1 (R)
    lockManager.lock(l1, c0, s2, ServerLockLevel.READ);

    // thread1 requests upgrade
    lockManager.lock(l1, c0, s1, ServerLockLevel.WRITE);
    // thread2 requests upgrade
    lockManager.lock(l1, c0, s2, ServerLockLevel.WRITE);

    TestDeadlockResults deadlocks = new TestDeadlockResults();
    lockManager.scanForDeadlocks(deadlocks);

    assertEquals(1, deadlocks.chains.size());

    Map check = new HashMap();
    check.put(thread1, l1);
    check.put(thread2, l1);
    assertSpecificDeadlock((DeadlockChain) deadlocks.chains.get(0), check);

    lockManager.clearAllLocksFor(c0);
  }

  public void testLackOfDeadlock() throws InterruptedException {
    if (true) {
      Banner.warnBanner("DEADLOCK tests DISABLED");
      return;
    }

    lockManager.start();
    for (int i = 0; i < 500; i++) {
      internalTestLackofDeadlock(false);
      resetLockManager(true);
      internalTestLackofDeadlock(true);
      resetLockManager(true);
    }
  }

  private void internalTestLackofDeadlock(boolean useRealThreads) throws InterruptedException {
    List threads = new ArrayList();

    for (int t = 0; t < numThreads; t++) {
      NodeID cid = txns[t].getNodeID();
      ThreadID tid = txns[t].getClientThreadID();

      RandomRequest req = new RandomRequest(cid, tid);
      if (useRealThreads) {
        Thread thread = new Thread(req);
        thread.start();
        threads.add(thread);
      } else {
        req.run();
      }
    }

    if (useRealThreads) {
      for (Iterator iter = threads.iterator(); iter.hasNext();) {
        Thread t = (Thread) iter.next();
        t.join();
      }
    }

    TestDeadlockResults results = new TestDeadlockResults();
    lockManager.scanForDeadlocks(results);

    assertEquals(0, results.chains.size());

    for (int i = 0; i < txns.length; i++) {
      lockManager.clearAllLocksFor((ClientID) txns[i].getNodeID());
    }
  }

  private class RandomRequest implements Runnable {
    private final NodeID   cid;
    private final ThreadID tid;

    public RandomRequest(NodeID cid, ThreadID tid) {
      this.cid = cid;
      this.tid = tid;
    }

    public void run() {
      final int start = random.nextInt(numLocks);
      final int howMany = random.nextInt(numLocks - start);

      for (int i = 0; i < howMany; i++) {
        LockID lock = locks[start + i];
        boolean read = random.nextInt(10) < 8; // 80% reads
        ServerLockLevel level = read ? ServerLockLevel.READ : ServerLockLevel.WRITE;
        lockManager.lock(lock, (ClientID) cid, tid, level);
      }
    }
  }

  private ServerThreadID[] makeUniqueTxns(int num) {
    ServerThreadID[] rv = new ServerThreadID[num];
    for (int i = 0; i < num; i++) {
      rv[i] = new ServerThreadID(new ClientID(i), new ThreadID(i));
    }
    return rv;
  }

  private LockID[] makeUniqueLocks(int num) {
    LockID[] rv = new LockID[num];
    for (int i = 0; i < num; i++) {
      rv[i] = new StringLockID("lock-" + i);
    }

    return rv;
  }

  private void assertSpecificDeadlock(DeadlockChain chain, Map check) {
    DeadlockChain start = chain;
    do {
      LockID lock = (LockID) check.remove(chain.getWaiter());
      assertEquals(lock, chain.getWaitingOn());
      chain = chain.getNextLink();
    } while (chain != start);

    assertEquals(0, check.size());
  }

  public void testDeadLock2() {
    if (true) {
      Banner.warnBanner("DEADLOCK tests DISABLED");
      return;
    }

    // A slightly more complicated deadlock:
    // -- Thread1 holds lock1, wants lock2
    // -- Thread2 holds lock2, wants lock3
    // -- Thread3 holds lock3, wants lock1

    LockID l1 = new StringLockID("L1");
    LockID l2 = new StringLockID("L2");
    LockID l3 = new StringLockID("L3");
    ClientID c0 = new ClientID(0);
    ThreadID s1 = new ThreadID(1);
    ThreadID s2 = new ThreadID(2);
    ThreadID s3 = new ThreadID(3);

    ServerThreadID thread1 = new ServerThreadID(c0, s1);
    ServerThreadID thread2 = new ServerThreadID(c0, s2);
    ServerThreadID thread3 = new ServerThreadID(c0, s3);

    lockManager.start();

    // thread1 gets lock1
    lockManager.lock(l1, c0, s1, ServerLockLevel.WRITE);
    // thread2 gets lock2
    lockManager.lock(l2, c0, s2, ServerLockLevel.WRITE);
    // thread3 gets lock3
    lockManager.lock(l3, c0, s3, ServerLockLevel.WRITE);

    // thread1 trys to get lock2 (blocks)
    lockManager.lock(l2, c0, s1, ServerLockLevel.WRITE);
    // thread2 trys to get lock3 (blocks)
    lockManager.lock(l3, c0, s2, ServerLockLevel.WRITE);
    // thread3 trys to get lock1 (blocks)
    lockManager.lock(l1, c0, s3, ServerLockLevel.WRITE);

    TestDeadlockResults deadlocks = new TestDeadlockResults();
    lockManager.scanForDeadlocks(deadlocks);

    assertEquals(1, deadlocks.chains.size());

    Map check = new HashMap();
    check.put(thread1, l2);
    check.put(thread2, l3);
    check.put(thread3, l1);
    assertSpecificDeadlock((DeadlockChain) deadlocks.chains.get(0), check);

    lockManager.clearAllLocksFor(c0);
  }

  private class ShutdownThread extends Thread {
    private final LinkedQueue shutdownSteps;

    private ShutdownThread(LinkedQueue shutdownSteps) {
      this.shutdownSteps = shutdownSteps;
    }

    @Override
    public void run() {
      try {
        shutdownSteps.put(new Object());
        lockManager.stop();
        shutdownSteps.put(new Object());
      } catch (Exception e) {
        e.printStackTrace();
        fail();
      }
    }
  }

  private static class TestDeadlockResults implements DeadlockResults {
    final List chains = new ArrayList();

    public void foundDeadlock(DeadlockChain chain) {
      chains.add(chain);
    }
  }

  public static class TestServerLockFactory implements LockFactory {
    public Lock createLock(LockID lid) {
      return new NonGreedyPolicyLock(lid);
    }
  }

}
