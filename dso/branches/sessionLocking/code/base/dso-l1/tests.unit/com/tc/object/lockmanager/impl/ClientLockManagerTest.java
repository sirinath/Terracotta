/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import EDU.oswego.cs.dl.util.concurrent.BrokenBarrierException;
import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;
import EDU.oswego.cs.dl.util.concurrent.Latch;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

import com.tc.config.lock.LockContextInfo;
import com.tc.exception.TCLockUpgradeNotSupportedError;
import com.tc.exception.TCRuntimeException;
import com.tc.handler.LockInfoDumpHandler;
import com.tc.logging.NullTCLogger;
import com.tc.logging.TCLogger;
import com.tc.management.ClientLockStatManager;
import com.tc.management.L1Info;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.lockmanager.api.ClientLockManagerConfig;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.LockRequest;
import com.tc.object.lockmanager.api.NullClientLockManagerConfig;
import com.tc.object.lockmanager.api.RemoteLockManager;
import com.tc.object.lockmanager.api.TestRemoteLockManager;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.api.ThreadLockManager;
import com.tc.object.lockmanager.api.WaitListener;
import com.tc.object.lockmanager.api.WaitLockRequest;
import com.tc.object.lockmanager.api.TestRemoteLockManager.LockResponder;
import com.tc.object.msg.TestClientHandshakeMessage;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.object.session.SessionProvider;
import com.tc.object.session.TestSessionManager;
import com.tc.object.tx.TimerSpec;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.LockInfoByThreadID;
import com.tc.util.runtime.ThreadIDMap;
import com.tc.util.runtime.ThreadIDMapUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ClientLockManagerTest extends TCTestCase {
  private ClientLockManagerImpl lockManager;
  private TestRemoteLockManager rmtLockManager;
  private TestSessionManager    sessionManager;
  private final GroupID         gid = new GroupID(0);

  public ClientLockManagerTest() {
    //
  }

  protected void setUp() throws Exception {
    super.setUp();
    sessionManager = new TestSessionManager();
    rmtLockManager = new TestRemoteLockManager(sessionManager);

    lockManager = new ClientLockManagerImpl(new NullTCLogger(), rmtLockManager, sessionManager,
                                            ClientLockStatManager.NULL_CLIENT_LOCK_STAT_MANAGER,
                                            new NullClientLockManagerConfig());
    rmtLockManager.setClientLockManager(lockManager);
  }

  public void testRunGC() {
    NullClientLockManagerConfig testClientLockManagerConfig = new NullClientLockManagerConfig(100);

    final ClientLockManagerImpl clientLockManagerImpl = new ClientLockManagerImpl(
                                                                                  new NullTCLogger(),
                                                                                  rmtLockManager,
                                                                                  sessionManager,
                                                                                  ClientLockStatManager.NULL_CLIENT_LOCK_STAT_MANAGER,
                                                                                  testClientLockManagerConfig);
    rmtLockManager.setClientLockManager(clientLockManagerImpl);

    final LockID lockID1 = new LockID("1");
    final ThreadID threadID1 = new ThreadID(1);

    rmtLockManager.lockResponder = new LockResponder() {

      public void respondToLockRequest(LockRequest request) {

        clientLockManagerImpl.awardLock(gid, sessionManager.getSessionID(gid), request.lockID(), ThreadID.VM_ID, LockLevel
            .makeGreedy(request.lockLevel()));
      }
    };

    clientLockManagerImpl.lock(lockID1, threadID1, LockLevel.WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);

    clientLockManagerImpl.unlock(lockID1, threadID1);

    ThreadUtil.reallySleep(200);
    clientLockManagerImpl.runGC();

    assertEquals(0, clientLockManagerImpl.getLocksByIDSize());

    // now change the timeout to a much higher number
    testClientLockManagerConfig.setTimeoutInterval(Long.MAX_VALUE);

    clientLockManagerImpl.lock(lockID1, threadID1, LockLevel.WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);

    clientLockManagerImpl.unlock(lockID1, threadID1);

    clientLockManagerImpl.runGC();
    assertEquals(1, clientLockManagerImpl.getLocksByIDSize());

  }

  public void testRunGCWithAHeldLock() {
    NullClientLockManagerConfig testClientLockManagerConfig = new NullClientLockManagerConfig(100);

    final ClientLockManagerImpl clientLockManagerImpl = new ClientLockManagerImpl(
                                                                                  new NullTCLogger(),
                                                                                  rmtLockManager,
                                                                                  sessionManager,
                                                                                  ClientLockStatManager.NULL_CLIENT_LOCK_STAT_MANAGER,
                                                                                  testClientLockManagerConfig);
    rmtLockManager.setClientLockManager(clientLockManagerImpl);

    final LockID lockID1 = new LockID("1");
    final LockID lockID2 = new LockID("2");
    final ThreadID threadID1 = new ThreadID(1);

    rmtLockManager.lockResponder = new LockResponder() {

      public void respondToLockRequest(LockRequest request) {

        clientLockManagerImpl.awardLock(gid, sessionManager.getSessionID(gid), request.lockID(), ThreadID.VM_ID, LockLevel
            .makeGreedy(request.lockLevel()));
      }
    };

    // Hold lock 1
    clientLockManagerImpl.lock(lockID1, threadID1, LockLevel.WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);

    // Grab and release lock 2
    clientLockManagerImpl.lock(lockID2, threadID1, LockLevel.WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    clientLockManagerImpl.unlock(lockID2, threadID1);

    ThreadUtil.reallySleep(200);
    clientLockManagerImpl.runGC();

    // One lock should be GCed
    assertEquals(1, clientLockManagerImpl.getLocksByIDSize());

    // now unlock lock 1
    clientLockManagerImpl.unlock(lockID1, threadID1);

    ThreadUtil.reallySleep(200);
    clientLockManagerImpl.runGC();

    // Both should be GCed
    assertEquals(0, clientLockManagerImpl.getLocksByIDSize());

  }

  /**
   * testing accessOrder for LinkedHashMap which ClientHashMap extends
   */
  public void testClientHashMap() {
    LinkedHashMap linkedHashMap = new LinkedHashMap(4, 0.75f, true);

    linkedHashMap.put("key1", "value1");
    linkedHashMap.put("key2", "value2");
    linkedHashMap.put("key3", "value3");
    linkedHashMap.put("key4", "value4");

    // do two reads
    linkedHashMap.get("key1");
    linkedHashMap.get("key2");

    Iterator iter = linkedHashMap.values().iterator();
    assertEquals((String) iter.next(), "value3");
    assertEquals((String) iter.next(), "value4");
    assertEquals((String) iter.next(), "value1");
    assertEquals((String) iter.next(), "value2");

    linkedHashMap = new LinkedHashMap(4, 0.75f, false);
    linkedHashMap.put("key1", "value1");
    linkedHashMap.put("key2", "value2");
    linkedHashMap.put("key3", "value3");
    linkedHashMap.put("key4", "value4");

    // do two reads
    linkedHashMap.get("key1");
    linkedHashMap.get("key2");

    iter = linkedHashMap.values().iterator();
    assertEquals((String) iter.next(), "value1");
    assertEquals((String) iter.next(), "value2");
    assertEquals((String) iter.next(), "value3");
    assertEquals((String) iter.next(), "value4");

  }

  public void testNestedSynchronousWrite() {
    final LockID lockID_1 = new LockID("1");
    final LockID lockID_2 = new LockID("2");
    final ThreadID threadID_1 = new ThreadID(1);
    final ThreadID threadID_2 = new ThreadID(2);

    rmtLockManager.resetFlushCount();

    assertEquals(0, rmtLockManager.getFlushCount());
    lockManager.lock(lockID_1, threadID_1, LockLevel.WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    lockManager.lock(lockID_1, threadID_1, LockLevel.READ, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    lockManager.lock(lockID_1, threadID_1, LockLevel.SYNCHRONOUS_WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    lockManager.lock(lockID_1, threadID_1, LockLevel.WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    lockManager.lock(lockID_1, threadID_1, LockLevel.SYNCHRONOUS_WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    assertEquals(0, rmtLockManager.getFlushCount());
    lockManager.unlock(lockID_1, threadID_1);
    assertEquals(1, rmtLockManager.getFlushCount());
    lockManager.unlock(lockID_1, threadID_1);
    assertEquals(1, rmtLockManager.getFlushCount());
    lockManager.unlock(lockID_1, threadID_1);
    assertEquals(2, rmtLockManager.getFlushCount());
    lockManager.unlock(lockID_1, threadID_1);
    assertEquals(2, rmtLockManager.getFlushCount());
    lockManager.unlock(lockID_1, threadID_1);
    assertEquals(3, rmtLockManager.getFlushCount());

    rmtLockManager.resetFlushCount();
    rmtLockManager.makeLocksGreedy();

    assertEquals(0, rmtLockManager.getFlushCount());
    lockManager.lock(lockID_2, threadID_2, LockLevel.WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    lockManager.lock(lockID_2, threadID_2, LockLevel.READ, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    lockManager.lock(lockID_2, threadID_2, LockLevel.SYNCHRONOUS_WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    lockManager.lock(lockID_2, threadID_2, LockLevel.WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    lockManager.lock(lockID_2, threadID_2, LockLevel.SYNCHRONOUS_WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    assertEquals(0, rmtLockManager.getFlushCount());
    lockManager.unlock(lockID_2, threadID_2);
    assertEquals(1, rmtLockManager.getFlushCount());
    lockManager.unlock(lockID_2, threadID_2);
    assertEquals(1, rmtLockManager.getFlushCount());
    lockManager.unlock(lockID_2, threadID_2);
    assertEquals(2, rmtLockManager.getFlushCount());
    lockManager.unlock(lockID_2, threadID_2);
    lockManager.unlock(lockID_2, threadID_2);
    assertEquals(2, rmtLockManager.getFlushCount());
    rmtLockManager.resetFlushCount();
    rmtLockManager.makeLocksNotGreedy();
  }

  public void testSynchronousWriteUnlock() {
    final LockID lockID_1 = new LockID("1");
    final LockID lockID_2 = new LockID("2");
    final ThreadID threadID_1 = new ThreadID(1);
    final ThreadID threadID_2 = new ThreadID(2);

    rmtLockManager.resetFlushCount();

    assertEquals(0, rmtLockManager.getFlushCount());
    lockManager.lock(lockID_1, threadID_1, LockLevel.SYNCHRONOUS_WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    assertEquals(0, rmtLockManager.getFlushCount());
    lockManager.unlock(lockID_1, threadID_1);
    assertEquals(1, rmtLockManager.getFlushCount());

    rmtLockManager.makeLocksGreedy();

    lockManager.lock(lockID_2, threadID_2, LockLevel.SYNCHRONOUS_WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    assertEquals(1, rmtLockManager.getFlushCount());
    lockManager.unlock(lockID_2, threadID_2);
    assertEquals(2, rmtLockManager.getFlushCount());

    rmtLockManager.resetFlushCount();
    rmtLockManager.makeLocksNotGreedy();
  }

  public void testSynchronousWriteWait() {

    final LockID lockID_1 = new LockID("1");
    final LockID lockID_2 = new LockID("2");
    final ThreadID threadID_1 = new ThreadID(1);
    final ThreadID threadID_2 = new ThreadID(2);

    rmtLockManager.resetFlushCount();

    assertEquals(0, rmtLockManager.getFlushCount());
    lockManager.lock(lockID_1, threadID_1, LockLevel.SYNCHRONOUS_WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    assertEquals(0, rmtLockManager.getFlushCount());

    TimerSpec waitInvocation = new TimerSpec();
    NoExceptionLinkedQueue barrier = new NoExceptionLinkedQueue();
    WaitLockRequest waitLockRequest = new WaitLockRequest(lockID_1, threadID_1, LockLevel.SYNCHRONOUS_WRITE,
                                                          String.class.getName(), waitInvocation);
    LockWaiter waiterThread = new LockWaiter(barrier, waitLockRequest, new Object());
    waiterThread.start();
    Object o = barrier.take();
    assertNotNull(o);

    assertEquals(1, rmtLockManager.getFlushCount());

    rmtLockManager.makeLocksGreedy();

    lockManager.lock(lockID_2, threadID_2, LockLevel.SYNCHRONOUS_WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    assertEquals(1, rmtLockManager.getFlushCount());

    waitInvocation = new TimerSpec();
    waitLockRequest = new WaitLockRequest(lockID_2, threadID_2, LockLevel.SYNCHRONOUS_WRITE, String.class.getName(),
                                          waitInvocation);
    waiterThread = new LockWaiter(barrier, waitLockRequest, new Object());
    waiterThread.start();
    o = barrier.take();
    assertNotNull(o);

    assertEquals(2, rmtLockManager.getFlushCount());

    rmtLockManager.resetFlushCount();
    rmtLockManager.makeLocksNotGreedy();
  }

  public void testTryLock() {
    class TryLockRemoteLockManager extends TestRemoteLockManager {
      private CyclicBarrier requestBarrier;
      private CyclicBarrier awardBarrier;

      public TryLockRemoteLockManager(SessionProvider sessionProvider, CyclicBarrier requestBarrier,
                                      CyclicBarrier awardBarrier) {
        super(sessionProvider);
        this.requestBarrier = requestBarrier;
        this.awardBarrier = awardBarrier;
      }

      public void tryRequestLock(LockID lockID, ThreadID threadID, TimerSpec timeout, int type, String lockType) {
        try {
          requestBarrier.barrier();
          awardBarrier.barrier();
        } catch (BrokenBarrierException e) {
          throw new TCRuntimeException(e);
        } catch (InterruptedException e) {
          throw new TCRuntimeException(e);
        }
      }
    }

    class TryLockClientLockManager extends ClientLockManagerImpl {
      private CyclicBarrier awardBarrier;

      public TryLockClientLockManager(TCLogger logger, RemoteLockManager remoteLockManager,
                                      SessionManager sessionManager, CyclicBarrier awardBarrier,
                                      ClientLockManagerConfig config) {
        super(logger, remoteLockManager, sessionManager, ClientLockStatManager.NULL_CLIENT_LOCK_STAT_MANAGER, config);
        this.awardBarrier = awardBarrier;
      }

      public void awardLock(NodeID nid, SessionID sessionID, LockID lockID, ThreadID threadID, int level) {
        try {
          awardBarrier.barrier();
          super.awardLock(nid, sessionID, lockID, threadID, level);
        } catch (BrokenBarrierException e) {
          throw new TCRuntimeException(e);
        } catch (InterruptedException e) {
          throw new TCRuntimeException(e);
        }
      }
    }

    final CyclicBarrier requestBarrier = new CyclicBarrier(2);
    final CyclicBarrier awardBarrier = new CyclicBarrier(2);

    rmtLockManager = new TryLockRemoteLockManager(sessionManager, requestBarrier, awardBarrier);

    lockManager = new TryLockClientLockManager(new NullTCLogger(), rmtLockManager, sessionManager, awardBarrier,
                                               new NullClientLockManagerConfig());

    final LockID lockID1 = new LockID("1");
    final ThreadID txID = new ThreadID(1);

    Thread t1 = new Thread(new Runnable() {
      public void run() {
        try {
          requestBarrier.barrier();
          lockManager.awardLock(gid, sessionManager.getSessionID(gid), lockID1, ThreadID.VM_ID, LockLevel
              .makeGreedy(LockLevel.WRITE));
        } catch (BrokenBarrierException e) {
          throw new TCRuntimeException(e);
        } catch (InterruptedException e) {
          throw new TCRuntimeException(e);
        }
      }
    });
    t1.start();

    lockManager.tryLock(lockID1, txID, new TimerSpec(0), LockLevel.WRITE, LockContextInfo.NULL_LOCK_OBJECT_TYPE);

  }

  public void testGreedyLockRequest() {
    final LockID lockID1 = new LockID("1");
    final ThreadID tx1 = new ThreadID(1);
    final ThreadID tx2 = new ThreadID(2);
    final NoExceptionLinkedQueue queue = new NoExceptionLinkedQueue();

    rmtLockManager.lockResponder = new LockResponder() {

      public void respondToLockRequest(LockRequest request) {
        queue.put(request);
        lockManager.awardLock(gid, sessionManager.getSessionID(gid), request.lockID(), ThreadID.VM_ID, LockLevel
            .makeGreedy(request.lockLevel()));
      }
    };

    lockManager.lock(lockID1, tx1, LockLevel.WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO); // Goes to
    // RemoteLockManager

    LockRequest request = (LockRequest) queue.poll(1000l);
    assertNotNull(request);
    assertEquals(tx1, request.threadID());
    assertEquals(lockID1, request.lockID());
    assertEquals(LockLevel.WRITE, request.lockLevel());

    // None of these should end up in RemoteLockManager
    lockManager.lock(lockID1, tx1, LockLevel.READ, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    lockManager.unlock(lockID1, tx1);
    lockManager.unlock(lockID1, tx1);
    lockManager.lock(lockID1, tx1, LockLevel.READ, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    lockManager.lock(lockID1, tx2, LockLevel.READ, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);

    assertNull(queue.poll(1000l));
  }

  public void testNotified() throws Exception {
    final LockID lockID1 = new LockID("1");
    final LockID lockID2 = new LockID("2");
    final ThreadID tx1 = new ThreadID(1);
    final ThreadID tx2 = new ThreadID(2);
    final Set heldLocks = new HashSet();
    final Set waiters = new HashSet();

    heldLocks.add(new LockRequest(lockID1, tx1, LockLevel.WRITE));
    lockManager.lock(lockID1, tx1, LockLevel.WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    assertNotNull(rmtLockManager.lockRequestCalls.poll(1));

    NoExceptionLinkedQueue barrier = new NoExceptionLinkedQueue();
    TimerSpec waitInvocation = new TimerSpec();

    // In order to wait on a lock, we must first request and be granted the
    // write lock. The TestRemoteLockManager
    // takes care of awarding the lock when we ask for it.
    //
    // We don't add this lock request to the set of held locks because the
    // call to wait moves it to being not
    // held anymore.
    lockManager.lock(lockID2, tx2, LockLevel.WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    assertNotNull(rmtLockManager.lockRequestCalls.poll(1));

    WaitLockRequest waitLockRequest = new WaitLockRequest(lockID2, tx2, LockLevel.WRITE, String.class.getName(),
                                                          waitInvocation);
    waiters.add(waitLockRequest);
    final LockWaiter waiterThread = new LockWaiter(barrier, waitLockRequest, new Object());
    waiterThread.start();

    barrier.take();
    assertTrue(barrier.isEmpty());
    if (!waiterThread.exceptions.isEmpty()) {
      for (Iterator i = waiterThread.exceptions.iterator(); i.hasNext();) {
        ((Throwable) i.next()).printStackTrace();
      }
      fail("Waiter thread had exceptions!");
    }

    Set s = new HashSet();
    lockManager.addAllHeldLocksTo(s);
    assertEquals(heldLocks, s);

    s.clear();
    lockManager.addAllWaitersTo(s);
    assertEquals(waiters, s);
    s.clear();

    lockManager.addAllPendingLockRequestsTo(s);
    assertTrue(s.size() == 0);

    // Make sure there are no pending lock requests
    rmtLockManager.lockResponder = rmtLockManager.NULL_LOCK_RESPONDER;
    assertTrue(rmtLockManager.lockRequestCalls.isEmpty());

    // now call notified() and make sure that the appropriate waits become
    // pending requests
    lockManager.notified(waitLockRequest.lockID(), waitLockRequest.threadID());

    // The held locks should be the same
    s.clear();
    lockManager.addAllHeldLocksTo(s);
    assertEquals(heldLocks, s);

    // the lock waits should be empty
    s.clear();
    lockManager.addAllWaitersTo(s);
    assertEquals(Collections.EMPTY_SET, s);

    lockManager.addAllPendingLockRequestsTo(s);
    assertTrue(s.size() == 1);
    LockRequest lr = (LockRequest) s.iterator().next();
    assertNotNull(lr);
    assertEquals(waitLockRequest.lockID(), lr.lockID());
    assertEquals(waitLockRequest.threadID(), lr.threadID());
    assertTrue(waitLockRequest.lockLevel() == lr.lockLevel());

    // now make sure that if you award the lock, the right stuff happens
    lockManager.awardLock(gid, sessionManager.getSessionID(gid), waitLockRequest.lockID(), waitLockRequest.threadID(),
                          waitLockRequest.lockLevel());
    heldLocks.add(waitLockRequest);

    // the held locks should contain the newly awarded, previously notified
    // lock.
    s.clear();
    lockManager.addAllHeldLocksTo(s);
    assertEquals(heldLocks, s);

    // there should still be no waiters
    s.clear();
    lockManager.addAllWaitersTo(s);
    assertEquals(Collections.EMPTY_SET, s);

    // the lock should have been awarded and no longer pending
    assertTrue(rmtLockManager.lockRequestCalls.isEmpty());
    lockManager.addAllPendingLockRequestsTo(null);
    assertTrue(rmtLockManager.lockRequestCalls.isEmpty());
  }

  public void testAddAllOutstandingLocksTo() throws Exception {

    // XXX: The current TestRemoteLockManager doesn't handle multiple
    // read-locks by different transactions properly,
    // so this test doesn't test that case.
    final LockID lockID = new LockID("my lock");
    final ThreadID tx1 = new ThreadID(1);
    final int writeLockLevel = LockLevel.WRITE;

    final LockID readLock = new LockID("my read lock");
    final ThreadID tx2 = new ThreadID(2);
    final int readLockLevel = LockLevel.READ;

    // final LockID synchWriteLock = new LockID("my synch write lock");
    // final ThreadID tx3 = new ThreadID(3);
    // final int synchWriteLockLevel = LockLevel.SYNCHRONOUS_WRITE;

    Set lockRequests = new HashSet();
    lockRequests.add(new LockRequest(lockID, tx1, writeLockLevel));
    lockRequests.add(new LockRequest(readLock, tx2, readLockLevel));
    // lockRequests.add(new LockRequest(synchWriteLock, tx3, synchWriteLockLevel));

    lockManager.lock(lockID, tx1, writeLockLevel, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    lockManager.lock(readLock, tx2, readLockLevel, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    // lockManager.lock(synchWriteLock, tx3, synchWriteLockLevel);

    Set s = new HashSet();
    lockManager.addAllHeldLocksTo(s);
    assertEquals(lockRequests.size(), s.size());
    assertEquals(lockRequests, s);

    lockManager.unlock(lockID, tx1);
    lockManager.unlock(readLock, tx2);
    // lockManager.unlock(synchWriteLock, tx3);
    assertEquals(0, lockManager.addAllHeldLocksTo(new HashSet()).size());
  }

  public void testAddAllOutstandingWaitersTo() throws Exception {

    final ThreadIDMap threadIDMap = ThreadIDMapUtil.getInstance();
    final ThreadLockManager threadLockManager = new ThreadLockManagerImpl(lockManager, threadIDMap);
    final LockInfoDumpHandler lockInfoDumpHandler = new LockInfoDumpHandler() {

      public void addAllLocksTo(LockInfoByThreadID lockInfo) {
        lockManager.addAllLocksTo(lockInfo);
      }

      public ThreadIDMap getThreadIDMap() {
        return threadIDMap;
      }

    };

    final L1Info l1info = new L1Info(lockInfoDumpHandler);

    final LockID lockID = new LockID("my lock");
    final ThreadID tx1 = new ThreadID(1);
    final TimerSpec waitInvocation = new TimerSpec();
    final Object waitObject = new Object();
    final NoExceptionLinkedQueue barrier = new NoExceptionLinkedQueue();
    lockManager.lock(lockID, tx1, LockLevel.WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    Thread t = new LockWaiter(barrier, lockID, threadLockManager, waitInvocation, waitObject);
    t.start();
    barrier.take();
    ThreadUtil.reallySleep(200);

    Set s = new HashSet();
    lockManager.addAllWaitersTo(s);
    List waiters = new LinkedList(s);
    String threadDump = l1info.takeThreadDump(System.currentTimeMillis());
    assertEquals(1, waiters.size());
    WaitLockRequest waitRequest = (WaitLockRequest) waiters.get(0);
    assertEquals(lockID, waitRequest.lockID());
    assertEquals(tx1, waitRequest.threadID());
    assertEquals(waitInvocation, waitRequest.getTimerSpec());

    if (threadDump.indexOf("require JRE-1.5 or greater") < 0) {
      Assert.eval("The text \"WAITING ON LOCK: [LockID(my lock)]\" should be present in the thread dump", threadDump
          .indexOf("WAITING ON LOCK: [LockID(my lock)]") >= 0);
    }

    // The lock this waiter was in when wait was called should no longer be
    // outstanding.
    assertEquals(0, lockManager.addAllHeldLocksTo(new HashSet()).size());
  }

  public void testPauseBlocks() throws Exception {
    final LinkedQueue flowControl = new LinkedQueue();
    final LinkedQueue lockComplete = new LinkedQueue();
    final LinkedQueue unlockComplete = new LinkedQueue();
    final LockID lockID = new LockID("1");
    final ThreadID txID = new ThreadID(1);
    final int lockType = LockLevel.WRITE;

    final List lockerException = new ArrayList();
    Thread locker = new Thread("LOCKER") {
      public void run() {
        try {
          flowControl.put("locker: Calling lock");
          lockManager.lock(lockID, txID, lockType, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
          lockComplete.put("locker: lock complete.");

          // wait until I'm allowed to unlock...
          System.out.println(flowControl.take());
          lockManager.unlock(lockID, txID);
          unlockComplete.put("locker: unlock complete.");

          // wait until I'm allowed to call lock() again
          System.out.println(flowControl.take());
          rmtLockManager.lockResponder = rmtLockManager.NULL_LOCK_RESPONDER;
          lockManager.lock(lockID, txID, lockType, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
          System.out.println("locker: Done calling lock again");

        } catch (Throwable e) {
          e.printStackTrace();
          lockerException.add(e);
        }
      }
    };

    pause();
    locker.start();

    // wait until the locker has a chance to start up...
    System.out.println(flowControl.take());

    ThreadUtil.reallySleep(500);

    // make sure it hasn't returned from the lock call.
    assertTrue(lockComplete.peek() == null);

    unpause();

    // make sure the call to lock(..) completes
    System.out.println(lockComplete.take());
    System.out.println("Done testing lock(..)");

    // now pause again and allow the locker to call unlock...
    pause();
    flowControl.put("test: lock manager paused, it's ok for locker to call unlock(..)");
    ThreadUtil.reallySleep(500);
    assertTrue(unlockComplete.peek() == null);

    // now UN-pause and make sure the locker returns from unlock(..)
    unpause();

    unlockComplete.take();
    System.out.println("Done testing unlock(..)");

    // TODO: test awardLock() and the other public methods I didn't have
    // time to test...

    // assert locker thread never threw an exception
    assertTrue("Locker thread threw an exception: " + lockerException, lockerException.isEmpty());
  }

  public void testResendBasics() throws Exception {
    final List requests = new ArrayList();
    final LinkedQueue flowControl = new LinkedQueue();
    final SynchronizedBoolean respond = new SynchronizedBoolean(true);
    final List lockerException = new ArrayList();

    rmtLockManager.lockResponder = new LockResponder() {
      public void respondToLockRequest(final LockRequest request) {
        new Thread() {
          public void run() {
            requests.add(request);
            if (respond.get()) {
              lockManager.awardLock(gid, sessionManager.getSessionID(gid), request.lockID(), request.threadID(), request
                  .lockLevel());
            }
            try {
              flowControl.put("responder: respondToLockRequest complete.  Lock awarded: " + respond.get());
            } catch (InterruptedException e) {
              e.printStackTrace();
              lockerException.add(e);
            }
          }
        }.start();
      }
    };

    final ThreadID tid0 = new ThreadID(0);
    final ThreadID tid1 = new ThreadID(1);
    final LockID lid0 = new LockID("0");
    final LockID lid1 = new LockID("1");

    LockRequest lr0 = new LockRequest(lid0, tid0, LockLevel.WRITE);
    LockRequest lr1 = new LockRequest(lid1, tid1, LockLevel.WRITE);

    // request a lock that gets a response
    Thread t = new LockGetter(lid0, tid0, LockLevel.WRITE);
    t.start();
    // wait until the lock responder finishes...
    System.out.println(flowControl.take());
    assertEquals(1, requests.size());
    assertEquals(lr0, requests.get(0));

    // now request a lock that doesn't get a response.
    requests.clear();
    respond.set(false);

    t = new LockGetter(lid1, tid1, LockLevel.WRITE);
    t.start();
    System.out.println(flowControl.take());

    assertEquals(1, requests.size());
    assertEquals(lr1, requests.get(0));

    // resend outstanding lock requests and respond to them.
    requests.clear();
    respond.set(true);

    lockManager.addAllPendingLockRequestsTo(requests);

    assertEquals(1, requests.size());
    assertEquals(lr1, requests.get(0));

    // there should be no outstanding lock requests.
    // calling requestOutstanding() should cause no lock requests.

    requests.clear();
    rmtLockManager.lockResponder = rmtLockManager.LOOPBACK_LOCK_RESPONDER;

    // assert locker thread never threw an exception
    assertTrue("Locker thread threw an exception: " + lockerException, lockerException.isEmpty());
  }

  public void testAwardWhenNotPending() throws Exception {
    LockID lockID = new LockID("1");
    ThreadID txID = new ThreadID(1);
    lockManager.awardLock(gid, sessionManager.getSessionID(gid), lockID, txID, LockLevel.WRITE);
  }

  public void testBasics() throws Exception {
    final ThreadID tid0 = new ThreadID(0);
    final LockID lid0 = new LockID("0");

    final ThreadID tid1 = new ThreadID(1);

    System.out.println("Get lock0 for tx0");
    lockManager.lock(lid0, tid0, LockLevel.WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    System.out.println("Got lock0 for tx0");
    lockManager.lock(lid0, tid0, LockLevel.WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    System.out.println("Got lock0 for tx0 AGAIN so the recursion lock is correct");
    final boolean[] done = new boolean[1];
    done[0] = false;
    Thread t = new Thread() {
      public void run() {
        lockManager.lock(lid0, tid1, LockLevel.WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
        System.out.println("Got lock0 for tx1");
        done[0] = true;
      }
    };
    t.start();
    ThreadUtil.reallySleep(500);
    assertFalse(done[0]);
    lockManager.unlock(lid0, tid0);
    ThreadUtil.reallySleep(500);
    assertFalse(done[0]);
    lockManager.unlock(lid0, tid0);
    ThreadUtil.reallySleep(500);
    assertTrue(done[0]);
  }

  public void testAllLockInfoInThreadDump() throws Exception {

    final ThreadIDMap threadIDMap = ThreadIDMapUtil.getInstance();
    final ThreadLockManager threadLockManager = new ThreadLockManagerImpl(lockManager, threadIDMap);

    final LockInfoDumpHandler lockInfoDumpHandler = new LockInfoDumpHandler() {

      public void addAllLocksTo(LockInfoByThreadID lockInfo) {
        lockManager.addAllLocksTo(lockInfo);
      }

      public ThreadIDMap getThreadIDMap() {
        return threadIDMap;
      }

    };

    final L1Info l1info = new L1Info(lockInfoDumpHandler);
    final LockID lid0 = threadLockManager.lockIDFor("Locky0");
    final LockID lid1 = threadLockManager.lockIDFor("Locky1");
    final LockID lid2 = threadLockManager.lockIDFor("Locky2");
    final LockID lid3 = threadLockManager.lockIDFor("Locky3");

    final CyclicBarrier txnBarrier = new CyclicBarrier(3);

    final Latch[] done = new Latch[3];
    for (int i = 0; i < done.length; i++) {
      done[i] = new Latch();
    }

    Thread.currentThread().setName("terracotta_thread");
    threadLockManager.lock(lid0, LockLevel.WRITE, LockContextInfo.NULL_LOCK_OBJECT_TYPE,
                           LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    System.out.println("XXX TERRA Thread : Got WRITE lock0 for tx0");

    threadLockManager.lock(lid0, LockLevel.WRITE, LockContextInfo.NULL_LOCK_OBJECT_TYPE,
                           LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    System.out.println("XXX TERRA Thread : Again .. Got WRITE lock0 for tx0");

    threadLockManager.lock(lid1, LockLevel.READ, LockContextInfo.NULL_LOCK_OBJECT_TYPE,
                           LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    System.out.println("XXX TERRA Thread : Got READ lock1 for tx0");

    Thread t1 = new Thread("yahoo_thread") {
      public void run() {
        threadLockManager.lock(lid3, LockLevel.WRITE, LockContextInfo.NULL_LOCK_OBJECT_TYPE,
                               LockContextInfo.NULL_LOCK_CONTEXT_INFO);
        System.out.println("XXX YAHOO Thread : Got WRITE lock3 for tx1");

        threadLockManager.lock(lid0, LockLevel.WRITE, LockContextInfo.NULL_LOCK_OBJECT_TYPE,
                               LockContextInfo.NULL_LOCK_CONTEXT_INFO);
        System.out.println("XXX YAHOO Thread : Got WRITE lock0 for tx1");

        try {
          txnBarrier.barrier();
        } catch (Exception e) {
          throw new AssertionError(e);
        }

        /*
         * threadLockManager.unlock(lid0); System.out.println("XXX YAHOO Thread : Released WRITE lock0 for tx1");
         */

        threadLockManager.unlock(lid3);
        System.out.println("XXX YAHOO Thread : Released WRITE lock3 for tx1");

        done[1].release();
      }
    };

    Thread t2 = new Thread("google_thread") {
      public void run() {
        threadLockManager.lock(lid2, LockLevel.WRITE, LockContextInfo.NULL_LOCK_OBJECT_TYPE,
                               LockContextInfo.NULL_LOCK_CONTEXT_INFO);
        System.out.println("XXX GOOGL Thread : Got WRITE lock2 for tx2");

        try {
          txnBarrier.barrier();
        } catch (Exception e) {
          throw new AssertionError(e);
        }

        threadLockManager.lock(lid1, LockLevel.WRITE, LockContextInfo.NULL_LOCK_OBJECT_TYPE,
                               LockContextInfo.NULL_LOCK_CONTEXT_INFO);
        System.out.println("XXX GOOGL Thread : Got WRITE lock1 for tx2");
        done[2].release();
      }
    };

    t1.start();
    t2.start();

    assertFalse(done[1].attempt(500));
    assertFalse(done[2].attempt(500));

    // pauseAndStart();
    String threadDump = l1info.takeThreadDump(System.currentTimeMillis());

    if (threadDump.indexOf("require JRE-1.5 or greater") < 0) {
      Assert.eval("The text \"LOCKED : [LockID(Locky2)]\" should be present in the thread dump", threadDump
          .indexOf("LOCKED : [LockID(Locky2)]") >= 0);

      Assert.eval("The text \"LOCKED : [LockID(Locky3)]\" should be present in the thread dump", threadDump
          .indexOf("LOCKED : [LockID(Locky3)]") >= 0);

      Assert.eval("The text \"WAITING TO LOCK: [LockID(Locky0)]\" should be present in the thread dump", threadDump
          .indexOf("WAITING TO LOCK: [LockID(Locky0)]") >= 0);

      Assert.eval((threadDump.indexOf("LOCKED : [LockID(Locky1), LockID(Locky0)]") >= 0)
                  || (threadDump.indexOf("LOCKED : [LockID(Locky0), LockID(Locky1)]") >= 0));
    }

    threadLockManager.unlock(lid0);
    System.out.println("XXX TERRA Thread : Released WRITE lock0 for tx0");
    assertFalse(done[1].attempt(500));
    assertFalse(done[2].attempt(500));

    threadLockManager.unlock(lid0);
    System.out.println("XXX TERRA  Thread : Again Released WRITE lock0 for tx0");
    threadLockManager.unlock(lid1);
    System.out.println("XXX TERRA Thread : Released READ lock1 for tx0");
    assertFalse(done[1].attempt(500));
    assertFalse(done[2].attempt(500));

    txnBarrier.barrier();

    done[1].acquire();
    done[2].acquire();
  }

  public void testBasicUnlock() throws Exception {
    assertEquals(0, rmtLockManager.getLockRequestCount());
    assertEquals(0, rmtLockManager.getUnlockRequestCount());
    ThreadID tid0 = new ThreadID(0);
    LockID lid0 = new LockID("0");

    lockManager.lock(lid0, tid0, LockLevel.READ, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    assertEquals(1, rmtLockManager.getLockRequestCount());
    assertEquals(0, rmtLockManager.getUnlockRequestCount());

    lockManager.unlock(lid0, tid0);
    assertEquals(1, rmtLockManager.getLockRequestCount());
    assertEquals(1, rmtLockManager.getUnlockRequestCount());

    lockManager.lock(lid0, tid0, LockLevel.WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    assertEquals(2, rmtLockManager.getLockRequestCount());
    assertEquals(1, rmtLockManager.getUnlockRequestCount());

    lockManager.unlock(lid0, tid0);
    assertEquals(2, rmtLockManager.getLockRequestCount());
    assertEquals(2, rmtLockManager.getUnlockRequestCount());
  }

  public void testLockUpgradeMakesRemoteRequest() throws Exception {
    assertEquals(0, rmtLockManager.getLockRequestCount());
    ThreadID tid0 = new ThreadID(0);
    LockID lid0 = new LockID("0");

    lockManager.lock(lid0, tid0, LockLevel.READ, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    assertEquals(1, rmtLockManager.getLockRequestCount());

    // upgrade lock
    try {
      lockManager.lock(lid0, tid0, LockLevel.WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
      throw new AssertionError("Should have thrown a TCLockUpgradeNotSupportedError.");
    } catch (TCLockUpgradeNotSupportedError e) {
      // expected
    }
    assertEquals(1, rmtLockManager.getLockRequestCount());
  }

  public void testNestedReadLocksGrantsLocally() throws Exception {
    assertEquals(0, rmtLockManager.getLockRequestCount());
    ThreadID tid0 = new ThreadID(0);
    LockID lid0 = new LockID("0");

    lockManager.lock(lid0, tid0, LockLevel.READ, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    assertEquals(1, rmtLockManager.getLockRequestCount());

    final int count = 25;

    for (int i = 0; i < count; i++) {
      // get nested read locks
      lockManager.lock(lid0, tid0, LockLevel.READ, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
      assertEquals(1, rmtLockManager.getLockRequestCount());
    }

    for (int i = 0; i < count; i++) {
      lockManager.unlock(lid0, tid0);
      assertEquals(1, rmtLockManager.getLockRequestCount());
      assertEquals(0, rmtLockManager.getUnlockRequestCount());
    }

    lockManager.unlock(lid0, tid0);
    assertEquals(1, rmtLockManager.getLockRequestCount());
    assertEquals(1, rmtLockManager.getUnlockRequestCount());
  }

  public void testUnlockAfterDowngrade() throws Exception {
    assertEquals(0, rmtLockManager.getLockRequestCount());
    assertEquals(0, rmtLockManager.getUnlockRequestCount());
    ThreadID tid0 = new ThreadID(0);
    LockID lid0 = new LockID("0");

    lockManager.lock(lid0, tid0, LockLevel.WRITE, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    assertEquals(1, rmtLockManager.getLockRequestCount());
    assertEquals(0, rmtLockManager.getUnlockRequestCount());

    // downgrade lock
    lockManager.lock(lid0, tid0, LockLevel.READ, "", LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    assertEquals(1, rmtLockManager.getLockRequestCount());
    assertEquals(0, rmtLockManager.getUnlockRequestCount());

    lockManager.unlock(lid0, tid0);
    assertEquals(1, rmtLockManager.getLockRequestCount());
    assertEquals(0, rmtLockManager.getUnlockRequestCount());

    lockManager.unlock(lid0, tid0);
    assertEquals(1, rmtLockManager.getLockRequestCount());
    assertEquals(1, rmtLockManager.getUnlockRequestCount());
  }

  private void pause() {
    lockManager.pause(GroupID.ALL_GROUPS, 1);
  }

  private void unpause() {
    lockManager.initializeHandshake(GroupID.NULL_ID, GroupID.ALL_GROUPS, new TestClientHandshakeMessage());
    lockManager.unpause(GroupID.ALL_GROUPS, 0);
  }

  public static void main(String[] args) {
    //
  }

  public class LockWaiter extends Thread implements WaitListener {
    private final LockID                 lid;
    private final ThreadID               tid;
    private final TimerSpec              call;
    private final NoExceptionLinkedQueue preWaitSignalQueue;
    private final Object                 waitObject;
    private final List                   exceptions = new LinkedList();
    private final ThreadLockManager      threadLockManager;

    private LockWaiter(NoExceptionLinkedQueue preWaitSignalQueue, WaitLockRequest request, Object waitObject) {
      this(preWaitSignalQueue, request.lockID(), null, request.threadID(), request.getTimerSpec(), waitObject);
    }

    private LockWaiter(NoExceptionLinkedQueue preWaitSignalQueue, LockID lid, ThreadLockManager threadLockManager,
                       TimerSpec call, Object waitObject) {
      this(preWaitSignalQueue, lid, threadLockManager, null, call, waitObject);
    }

    private LockWaiter(NoExceptionLinkedQueue preWaitSignalQueue, LockID lid, ThreadID threadID, TimerSpec call,
                       Object waitObject) {
      this(preWaitSignalQueue, lid, null, threadID, call, waitObject);
    }

    private LockWaiter(NoExceptionLinkedQueue preWaitSignalQueue, LockID lid, ThreadLockManager threadLockManager,
                       ThreadID threadID, TimerSpec call, Object waitObject) {
      this.preWaitSignalQueue = preWaitSignalQueue;
      this.lid = lid;
      this.tid = threadID;
      this.threadLockManager = threadLockManager;
      this.waitObject = waitObject;
      this.call = call;
      this.setName("LockWaiter");
    }

    public void run() {
      try {
        if (threadLockManager != null) {
          threadLockManager.wait(lid, call, waitObject, this);
        } else {
          lockManager.wait(lid, tid, call, waitObject, this);
        }
      } catch (Throwable t) {
        exceptions.add(t);
      }

      ThreadUtil.reallySleep(2000);
    }

    public void handleWaitEvent() {
      preWaitSignalQueue.put(new Object());
    }

    public Collection getExceptions() {
      return this.exceptions;
    }
  }

  private class LockGetter extends Thread {
    LockID   lid;
    ThreadID tid;
    int      lockType;

    private LockGetter(LockID lid, ThreadID tid, int lockType) {
      this.lid = lid;
      this.tid = tid;
      this.lockType = lockType;
    }

    public void run() {
      lockManager.lock(lid, tid, lockType, LockContextInfo.NULL_LOCK_OBJECT_TYPE,
                       LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    }
  }
}
