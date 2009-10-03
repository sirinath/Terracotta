/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.locks;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.logging.NullTCLogger;
import com.tc.management.L2LockStatsManager;
import com.tc.object.lockmanager.api.WaitListener;
import com.tc.object.locks.ServerLockContext.Type;
import com.tc.object.session.TestSessionManager;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.lockmanager.api.NullChannelManager;
import com.tc.objectserver.locks.LockManagerImpl;
import com.tc.objectserver.locks.factory.NonGreedyLockPolicyFactory;
import com.tc.util.Assert;

import junit.framework.TestCase;

public class ClientServerLockManagerTest extends TestCase {

  private ClientLockManagerImpl       clientLockManager;
  private LockManagerImpl             serverLockManager;
  private ClientServerLockManagerGlue glue;
  private TestSessionManager          sessionManager;
  private ManualThreadIDManager       threadManager;

  protected void setUp() throws Exception {
    super.setUp();
    sessionManager = new TestSessionManager();
    TestSink sink = new TestSink();
    glue = new ClientServerLockManagerGlue(sessionManager, sink, new NonGreedyLockPolicyFactory());
    threadManager = new ManualThreadIDManager();
    clientLockManager = new ClientLockManagerImpl(new NullTCLogger(), sessionManager, glue, threadManager, new NullClientLockManagerConfig());

    serverLockManager = new LockManagerImpl(sink, L2LockStatsManager.NULL_LOCK_STATS_MANAGER, new NullChannelManager(),
                                            new NonGreedyLockPolicyFactory());
    glue.set(clientLockManager, serverLockManager);
  }

  public void testRWServer() {
    final LockID lockID1 = new StringLockID("1");
    final LockID lockID2 = new StringLockID("2");
    final ThreadID tx1 = new ThreadID(1);
    final ThreadID tx2 = new ThreadID(2);
    final ThreadID tx3 = new ThreadID(3);

    threadManager.setThreadID(tx1);
    clientLockManager.lock(lockID1, LockLevel.READ);
    threadManager.setThreadID(tx3);
    clientLockManager.lock(lockID1, LockLevel.READ);
    threadManager.setThreadID(tx2);
    clientLockManager.lock(lockID2, LockLevel.READ);
    glue.restartServer();
    // clientLockManager.lock(lockID2, tx2, LockLevel.WRITE); // Upgrade
    // LockMBean[] lockBeans1 = serverLockManager.getAllLocks();
    // LockManagerImpl server2 = glue.restartServer();
    // LockMBean[] lockBeans2 = server2.getAllLocks();
    // if (!equals(lockBeans1, lockBeans2)) { throw new AssertionError("The locks are not the same"); }
  }

  public void testWRServer() {
    final LockID lockID1 = new StringLockID("1");
    final LockID lockID2 = new StringLockID("2");
    final ThreadID tx1 = new ThreadID(1);
    final ThreadID tx2 = new ThreadID(2);
    final ThreadID tx3 = new ThreadID(3);

    threadManager.setThreadID(tx1);
    clientLockManager.lock(lockID1, LockLevel.READ);
    threadManager.setThreadID(tx3);
    clientLockManager.lock(lockID1, LockLevel.READ);
    threadManager.setThreadID(tx2);
    clientLockManager.lock(lockID2, LockLevel.WRITE);
    clientLockManager.lock(lockID2, LockLevel.READ);

    glue.restartServer();
    // LockMBean[] lockBeans1 = serverLockManager.getAllLocks();
    // LockManagerImpl server2 = glue.restartServer();
    // LockMBean[] lockBeans2 = server2.getAllLocks();
    // if (!equals(lockBeans1, lockBeans2)) { throw new AssertionError("The locks are not the same"); }
  }

  public void testLockWaitWriteServer() throws Exception {
    final LockID lockID1 = new StringLockID("1");
    final ThreadID tx1 = new ThreadID(1);

    threadManager.setThreadID(tx1);
    clientLockManager.lock(lockID1, LockLevel.WRITE);

    final CyclicBarrier barrier = new CyclicBarrier(2);
    Thread waitCallThread = new Thread() {

      public void run() {
        try {
          threadManager.setThreadID(tx1);
          clientLockManager.wait(lockID1, new WaitListener() {
            public void handleWaitEvent() {
              try {
                barrier.barrier();
              } catch (Exception e) {
                e.printStackTrace();
                throw new AssertionError(e);
              }
            }
          });
        } catch (InterruptedException ie) {
          handleExceptionForTest(ie);
        }
      }
    };
    waitCallThread.start();
    barrier.barrier();
    glue.restartServer();
    // LockMBean[] lockBeans1 = serverLockManager.getAllLocks();
    // LockManagerImpl server2 = glue.restartServer();
    // LockMBean[] lockBeans2 = server2.getAllLocks();
    // if (!equals(lockBeans1, lockBeans2)) { throw new AssertionError("The locks are not the same"); }
  }

  public void testWaitWRServer() {
    final LockID lockID1 = new StringLockID("1");
    final ThreadID tx1 = new ThreadID(1);

    threadManager.setThreadID(tx1);
    clientLockManager.lock(lockID1, LockLevel.WRITE);
    clientLockManager.lock(lockID1, LockLevel.READ);
    Thread waitCallThread = new Thread() {

      public void run() {
        try {
          threadManager.setThreadID(tx1);
          clientLockManager.wait(lockID1);
        } catch (InterruptedException ie) {
          handleExceptionForTest(ie);
        }
      }
    };
    waitCallThread.start();
    sleep(1000l);
    glue.restartServer();
    // LockMBean[] lockBeans1 = serverLockManager.getAllLocks();
    // LockManagerImpl server2 = glue.restartServer();
    // LockMBean[] lockBeans2 = server2.getAllLocks();
    // if (!equals(lockBeans1, lockBeans2)) { throw new AssertionError("The locks are not the same"); }
  }

  public void testWaitNotifyRWServer() {
    final LockID lockID1 = new StringLockID("1");
    final ThreadID tx1 = new ThreadID(1);
    final ThreadID tx2 = new ThreadID(2);

    threadManager.setThreadID(tx1);
    clientLockManager.lock(lockID1, LockLevel.WRITE);

    Thread waitCallThread = new Thread() {

      public void run() {
        try {
          threadManager.setThreadID(tx1);
          clientLockManager.wait(lockID1);
        } catch (InterruptedException ie) {
          handleExceptionForTest(ie);
        }
      }
    };
    waitCallThread.start();
    sleep(1000l);

    threadManager.setThreadID(tx2);
    clientLockManager.lock(lockID1, LockLevel.WRITE);
    /*
     * Since this call is no longer in Lock manager, forced to call the server lock manager directly
     * clientLockManager.notify(lockID1,tx2, true);
     */
    glue.notify(lockID1, tx2, true);
    clientLockManager.unlock(lockID1, LockLevel.WRITE);
    sleep(1000l);
    glue.restartServer();
    // LockMBean[] lockBeans1 = serverLockManager.getAllLocks();
    // LockManagerImpl server2 = glue.restartServer();
    // LockMBean[] lockBeans2 = server2.getAllLocks();
    // if (!equals(lockBeans1, lockBeans2)) { throw new AssertionError("The locks are not the same"); }
  }

  public void testWaitNotifyRWClientServer() {
    final LockID lockID1 = new StringLockID("1");
    final ThreadID tx1 = new ThreadID(1);
    final ThreadID tx2 = new ThreadID(2);

    threadManager.setThreadID(tx1);
    clientLockManager.lock(lockID1, LockLevel.WRITE);

    // LockMBean[] lockBeans1 = serverLockManager.getAllLocks();

    Thread waitCallThread = new Thread() {

      public void run() {
        try {
          threadManager.setThreadID(tx1);
          clientLockManager.wait(lockID1);
        } catch (InterruptedException ie) {
          handleExceptionForTest(ie);
        }
      }
    };
    waitCallThread.start();
    sleep(1000l);

    threadManager.setThreadID(tx2);
    clientLockManager.lock(lockID1, LockLevel.WRITE);
    /*
     * Since this call is no longer in Lock manager, forced to call the server lock manager directly
     * clientLockManager.notify(lockID1,tx2, true);
     */
    glue.notify(lockID1, tx2, true);
    clientLockManager.unlock(lockID1, LockLevel.WRITE);
    sleep(1000l);

    boolean found = false;
    for (ClientServerExchangeLockContext c : clientLockManager.getAllLockContexts()) {
      if (c.getState().getType() == Type.HOLDER && c.getLockID().equals(lockID1) && c.getThreadID().equals(tx1)) {
        if (c.getState().getLockLevel() == ServerLockLevel.READ) { throw new AssertionError(
                                                                                            "Should not have READ lock level."); }
        found = true;
        break;
      }
    }
    if (!found) {
      // formatter
      throw new AssertionError("Didn't find the lock I am looking for");
    }
    // LockMBean[] lockBeans2 = serverLockManager.getAllLocks();
    // if (!equals(lockBeans1, lockBeans2)) { throw new AssertionError("The locks are not the same"); }
  }

  public void testWaitNotifyWRClientServer() {
    final LockID lockID1 = new StringLockID("1");
    final ThreadID tx1 = new ThreadID(1);
    final ThreadID tx2 = new ThreadID(2);

    threadManager.setThreadID(tx1);
    clientLockManager.lock(lockID1, LockLevel.WRITE);
    clientLockManager.lock(lockID1, LockLevel.READ);

    // LockMBean[] lockBeans1 = serverLockManager.getAllLocks();

    Thread waitCallThread = new Thread() {

      public void run() {
        try {
          threadManager.setThreadID(tx1);
          clientLockManager.wait(lockID1);
        } catch (InterruptedException ie) {
          handleExceptionForTest(ie);
        }
      }
    };
    waitCallThread.start();
    sleep(1000l);

    threadManager.setThreadID(tx2);
    clientLockManager.lock(lockID1, LockLevel.WRITE);
    /*
     * Since this call is no longer in Lock manager, forced to call the server lock manager directly
     * clientLockManager.notify(lockID1,tx2, true);
     */
    glue.notify(lockID1, tx2, true);
    clientLockManager.unlock(lockID1, LockLevel.WRITE);
    sleep(1000l);

    boolean found = false;
    for (ClientServerExchangeLockContext c : clientLockManager.getAllLockContexts()) {
      if (c.getState().getType() == Type.HOLDER && c.getLockID().equals(lockID1) && c.getThreadID().equals(tx1)) {
        // if (LockLevel.isRead(request.lockLevel()) || !LockLevel.isWrite(request.lockLevel())) {
        if (c.getState().getLockLevel() == ServerLockLevel.READ) { throw new AssertionError(
                                                                                            "Server Lock Level is not WRITE only on tx2 the client side"); }
        found = true;
        break;
      }
    }
    threadManager.setThreadID(tx1);
    Assert.assertTrue(clientLockManager.isLockedByCurrentThread(lockID1, LockLevel.READ));
    Assert.assertTrue(clientLockManager.isLockedByCurrentThread(lockID1, LockLevel.WRITE));
    if (!found) {
      // formatter
      throw new AssertionError("Didn't find the lock I am looking for");
    }
    // LockMBean[] lockBeans2 = serverLockManager.getAllLocks();
    // if (!equals(lockBeans1, lockBeans2)) { throw new AssertionError("The locks are not the same"); }
  }

  public void testPendingWaitNotifiedRWClientServer() {
    final LockID lockID1 = new StringLockID("1");
    final ThreadID tx1 = new ThreadID(1);
    final ThreadID tx2 = new ThreadID(2);

    threadManager.setThreadID(tx1);
    clientLockManager.lock(lockID1, LockLevel.WRITE);

    Thread waitCallThread = new Thread() {

      public void run() {
        try {
          threadManager.setThreadID(tx1);
          clientLockManager.wait(lockID1);
        } catch (InterruptedException ie) {
          handleExceptionForTest(ie);
        }
      }
    };
    waitCallThread.start();
    sleep(1000l);

    threadManager.setThreadID(tx2);
    clientLockManager.lock(lockID1, LockLevel.WRITE);
    /*
     * Since this call is no longer in Lock manager, forced to call the server lock manager directly
     * clientLockManager.notify(lockID1,tx2, true);
     */
    glue.notify(lockID1, tx2, true);
    sleep(1000l);

    boolean found = false;
    for (ClientServerExchangeLockContext c : clientLockManager.getAllLockContexts()) {
      if (c.getState().getType() == Type.HOLDER && c.getLockID().equals(lockID1) && c.getThreadID().equals(tx2)) {
        // if (LockLevel.isRead(request.lockLevel()) || !LockLevel.isWrite(request.lockLevel())) {
        if (c.getState().getLockLevel() == ServerLockLevel.READ) { throw new AssertionError(
                                                                                            "Server Lock Level is not WRITE only on tx2 the client side"); }
        found = true;
        break;
      }
    }
    if (!found) {
      // formatter
      throw new AssertionError("Didn't find the lock I am looking for");
    }

    glue.restartServer();
    // LockMBean[] lockBeans1 = serverLockManager.getAllLocks();
    // LockManagerImpl server2 = glue.restartServer();
    // LockMBean[] lockBeans2 = server2.getAllLocks();
    // if (!equals(lockBeans1, lockBeans2)) { throw new AssertionError("The locks are not the same"); }
  }

  public void testPendingWaitNotifiedWRClientServer() {
    final LockID lockID1 = new StringLockID("1");
    final ThreadID tx1 = new ThreadID(1);
    final ThreadID tx2 = new ThreadID(2);

    threadManager.setThreadID(tx1);
    clientLockManager.lock(lockID1, LockLevel.WRITE);
    clientLockManager.lock(lockID1, LockLevel.READ);

    Thread waitCallThread = new Thread() {

      public void run() {
        try {
          threadManager.setThreadID(tx1);
          clientLockManager.wait(lockID1);
        } catch (InterruptedException ie) {
          handleExceptionForTest(ie);
        }
      }
    };
    waitCallThread.start();
    sleep(1000l);

    threadManager.setThreadID(tx2);
    clientLockManager.lock(lockID1, LockLevel.WRITE);
    /*
     * Since this call is no longer in Lock manager, forced to call the server lock manager directly
     * clientLockManager.notify(lockID1,tx2, true);
     */
    glue.notify(lockID1, tx2, true);
    sleep(1000l);

    boolean found = false;
    for (ClientServerExchangeLockContext c : clientLockManager.getAllLockContexts()) {
      if (c.getState().getType() == Type.HOLDER && c.getLockID().equals(lockID1) && c.getThreadID().equals(tx2)) {
        // if (LockLevel.isRead(request.lockLevel()) || !LockLevel.isWrite(request.lockLevel())) {
        if (c.getState().getLockLevel() == ServerLockLevel.READ) { throw new AssertionError(
                                                                                            "Server Lock Level is not WRITE only on tx2 the client side"); }
        found = true;
        break;
      }
    }
    if (!found) {
      // formatter
      throw new AssertionError("Didn't find the lock I am looking for");
    }
    glue.restartServer();
    // LockMBean[] lockBeans1 = serverLockManager.getAllLocks();
    // LockManagerImpl server2 = glue.restartServer();
    // LockMBean[] lockBeans2 = server2.getAllLocks();
    // if (!equals(lockBeans1, lockBeans2)) { throw new AssertionError("The locks are not the same"); }
  }

  public void testPendingRequestClientServer() {
    final LockID lockID1 = new StringLockID("1");
    final ThreadID tx1 = new ThreadID(1);

    threadManager.setThreadID(tx1);
    clientLockManager.lock(lockID1, LockLevel.WRITE);
    clientLockManager.lock(lockID1, LockLevel.READ);

    Thread pendingLockRequestThread = new Thread() {

      public void run() {
        threadManager.setThreadID(tx1);
        clientLockManager.lock(lockID1, LockLevel.WRITE);
      }
    };
    pendingLockRequestThread.start();
    sleep(1000l);
    glue.restartServer();
    // LockMBean[] lockBeans1 = serverLockManager.getAllLocks();
    // LockManagerImpl server2 = glue.restartServer();
    // LockMBean[] lockBeans2 = server2.getAllLocks();
    // if (!equals(lockBeans1, lockBeans2)) { throw new AssertionError("The locks are not the same"); }
  }

  public void testWRClient() {
    final LockID lockID1 = new StringLockID("1");
    final ThreadID tx1 = new ThreadID(1);

    threadManager.setThreadID(tx1);
    clientLockManager.lock(lockID1, LockLevel.WRITE);
    clientLockManager.lock(lockID1, LockLevel.READ); // Local
    // Upgrade
    clientLockManager.unlock(lockID1, LockLevel.READ); // should release READ

    boolean found = false;
    for (ClientServerExchangeLockContext c : clientLockManager.getAllLockContexts()) {
      if (c.getState().getType() == Type.HOLDER && c.getLockID().equals(lockID1) && c.getThreadID().equals(tx1)) {
        // if (LockLevel.isRead(request.lockLevel()) || !LockLevel.isWrite(request.lockLevel())) {
        if (c.getState().getLockLevel() == ServerLockLevel.READ) { throw new AssertionError(
                                                                                            "Lock Level is not WRITE only"); }
        found = true;
        break;
      }
    }
    if (!found) { throw new AssertionError("Didn't find the lock I am looking for"); }
  }

  public void testConcurrentLocksServerRestart() {
    final LockID lockID1 = new StringLockID("1");
    final ThreadID tx1 = new ThreadID(1);
    final ThreadID tx2 = new ThreadID(2);

    threadManager.setThreadID(tx1);
    clientLockManager.lock(lockID1, LockLevel.CONCURRENT);
    threadManager.setThreadID(tx2);
    clientLockManager.lock(lockID1, LockLevel.CONCURRENT);
    glue.restartServer();
    // LockMBean[] lockBeans1 = serverLockManager.getAllLocks();
    // LockManagerImpl server2 = glue.restartServer();
    // LockMBean[] lockBeans2 = server2.getAllLocks();
    // if (!equals(lockBeans1, lockBeans2)) { throw new AssertionError("The locks are not the same"); }
  }

  // private boolean equals(LockMBean[] lockBeans1, LockMBean[] lockBeans2) {
  // if (lockBeans1.length != lockBeans2.length) { return false; }
  // for (int i = 0; i < lockBeans1.length; i++) {
  // String lockName1 = lockBeans1[i].getLockName();
  // boolean found = false;
  // for (int j = 0; j < lockBeans2.length; j++) {
  // String lockName2 = lockBeans2[j].getLockName();
  // if (lockName1.equals(lockName2)) {
  // if (!equals(lockBeans1[i], lockBeans2[j])) { return false; }
  // found = true;
  // break;
  // }
  // }
  // if (!found) { return false; }
  // }
  // return true;
  // }
  //
  // private boolean equals(LockMBean bean1, LockMBean bean2) {
  // return equals(bean1.getHolders(), bean2.getHolders())
  // && equals(bean1.getPendingRequests(), bean2.getPendingRequests())
  // && equals(bean1.getWaiters(), bean2.getWaiters());
  // }

  // private boolean equals(Waiter[] waiters1, Waiter[] waiters2) {
  // if (waiters1 == null && waiters2 == null) {
  // return true;
  // } else if (waiters1 == null || waiters2 == null || waiters1.length != waiters2.length) { return false; }
  // for (int i = 0; i < waiters1.length; i++) {
  // boolean found = false;
  // for (int j = 0; j < waiters2.length; j++) {
  // if (waiters1[i].getThreadID().equals(waiters2[j].getThreadID())) {
  // // XXX :: Should I do this -- Come back
  // /*
  // * if ( waiters1[i].getStartTime() != waiters2[j].getStartTime() ||
  // * waiters1[i].getWaitInvocation().equals(waiters2[j].getWaitInvocation())) {
  // * System.err.println("Not equal - " + waiters1[i].getStartTime() + " - " + waiters2[j].getStartTime());
  // * System.err.println("Not equal - " + waiters1[i].getWaitInvocation() + " - " +
  // * waiters2[j].getWaitInvocation()); return false; }
  // */
  // found = true;
  // break;
  // }
  // }
  // if (!found) { return false; }
  // }
  // return true;
  // }
  //
  // private boolean equals(ServerLockRequest[] pendingRequests1, ServerLockRequest[] pendingRequests2) {
  // if (pendingRequests1 == null && pendingRequests2 == null) {
  // return true;
  // } else if (pendingRequests1 == null || pendingRequests2 == null
  // || pendingRequests1.length != pendingRequests2.length) {
  // // for formatter
  // return false;
  // }
  // for (int i = 0; i < pendingRequests1.length; i++) {
  // boolean found = false;
  // for (int j = 0; j < pendingRequests2.length; j++) {
  // if (pendingRequests1[i].getThreadID().equals(pendingRequests2[j].getThreadID())) {
  // if (!pendingRequests1[i].getLockLevel().equals(pendingRequests2[j].getLockLevel())) {
  // System.err.println("Not equal - " + pendingRequests1[i].getLockLevel() + " - "
  // + pendingRequests2[j].getLockLevel());
  // return false;
  // }
  // found = true;
  // break;
  // }
  // }
  // if (!found) { return false; }
  // }
  // return true;
  // }
  //
  // private boolean equals(LockHolder[] holders1, LockHolder[] holders2) {
  // if (holders1 == null && holders2 == null) {
  // return true;
  // } else if (holders1 == null || holders2 == null || holders1.length != holders2.length) { return false; }
  // for (int i = 0; i < holders1.length; i++) {
  // boolean found = false;
  // for (int j = 0; j < holders2.length; j++) {
  // if (holders1[i].getThreadID().equals(holders2[j].getThreadID())) {
  // if (!holders1[i].getLockLevel().equals(holders2[j].getLockLevel())) {
  // System.out.println("Not equal - " + holders1[i] + " - " + holders2[j]);
  // return false;
  // }
  // found = true;
  // break;
  // }
  // }
  // if (!found) { return false; }
  // }
  // return true;
  // }

  private void sleep(long l) {
    try {
      Thread.sleep(l);
    } catch (InterruptedException e) {
      // NOP
    }
  }

  private void handleExceptionForTest(Exception e) {
    e.printStackTrace();
    throw new AssertionError(e);
  }

  protected void tearDown() throws Exception {
    glue.stop();
    super.tearDown();
  }
}
