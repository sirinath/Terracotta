/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks;

import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.L2LockStatsManager;
import com.tc.net.ClientID;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ServerLockLevel;
import com.tc.object.locks.ThreadID;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.lockmanager.api.DeadlockChain;
import com.tc.objectserver.lockmanager.api.DeadlockResults;
import com.tc.objectserver.lockmanager.api.TCIllegalMonitorStateException;
import com.tc.objectserver.locks.ServerLock.NotifyAction;
import com.tc.objectserver.locks.LockStore.LockIterator;
import com.tc.objectserver.locks.factory.LockFactoryImpl;
import com.tc.text.PrettyPrinter;
import com.tc.text.PrettyPrinterImpl;
import com.tc.util.Assert;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LockManagerImpl implements LockManager {
  private enum Status {
    STARTING, STARTED, STOPPING, STOPPED
  }

  private enum RequestType {
    LOCK, TRY_LOCK
  }

  private final LockStore              lockStore;
  private final DSOChannelManager      channelManager;
  private final LockHelper             lockHelper;
  private final ReentrantReadWriteLock statusLock       = new ReentrantReadWriteLock();
  private List<RequestLockContext>     lockRequestQueue = new ArrayList<RequestLockContext>();
  private Status                       status           = Status.STARTING;

  private static final TCLogger        logger           = TCLogging.getLogger(LockManagerImpl.class);

  public LockManagerImpl(Sink lockSink, L2LockStatsManager statsManager, DSOChannelManager channelManager) {
    this(lockSink, statsManager, channelManager, new LockFactoryImpl());
  }

  public LockManagerImpl(Sink lockSink, L2LockStatsManager statsManager, DSOChannelManager channelManager,
                         LockFactory factory) {
    this.lockStore = new LockStore(factory);
    this.lockHelper = new LockHelper(statsManager, lockSink, lockStore);
    this.channelManager = channelManager;
  }

  public void lock(LockID lid, ClientID cid, ThreadID tid, ServerLockLevel level) {
    if (!preChecksForLock(lid, cid, tid, level, RequestType.LOCK)) { return; }

    ServerLock lock = lockStore.checkOut(lid);
    try {
      lock.lock(cid, tid, level, lockHelper);
    } finally {
      lockStore.checkIn(lock);
    }
  }

  public void tryLock(LockID lid, ClientID cid, ThreadID tid, ServerLockLevel level, long timeout) {
    if (!checkAndQueue(lid, cid, tid, level, RequestType.TRY_LOCK, timeout)) { return; }
    ServerLock lock = lockStore.checkOut(lid);
    try {
      lock.tryLock(cid, tid, level, timeout, lockHelper);
    } finally {
      lockStore.checkIn(lock);
    }
  }

  public void unlock(LockID lid, ClientID cid, ThreadID tid) {
    if (!preCheckLogAndContinue(lid, cid, tid, "Unlock")) { return; }

    // Lock might be removed from the lock store in the call to the unlock
    ServerLock lock = lockStore.checkOut(lid);
    try {
      lock.unlock(cid, tid, lockHelper);
    } finally {
      lockStore.checkIn(lock);
    }
  }

  public void queryLock(LockID lid, ClientID cid, ThreadID tid) {
    if (!preCheckLogAndContinue(lid, cid, tid, "QueryLock")) { return; }

    ServerLock lock = lockStore.checkOut(lid);
    try {
      lock.queryLock(cid, tid, lockHelper);
    } finally {
      lockStore.checkIn(lock);
    }
  }

  public void interrupt(LockID lid, ClientID cid, ThreadID tid) {
    if (!preCheckLogAndContinue(lid, cid, tid, "Interrupt")) { return; }

    ServerLock lock = lockStore.checkOut(lid);
    try {
      lock.interrupt(cid, tid, lockHelper);
    } finally {
      lockStore.checkIn(lock);
    }
  }

  public void recallCommit(LockID lid, ClientID cid, Collection<ClientServerExchangeLockContext> serverLockContexts) {
    if (!preCheckLogAndContinue(lid, cid, ThreadID.VM_ID, "Recall Commit")) { return; }
    if (!this.channelManager.isActiveID(cid)) {
      logger.warn("Ignoring Recall Commit message from disconnected client : " + cid + " : Lock ID : " + lid);
      return;
    }

    ServerLock lock = lockStore.checkOut(lid);
    try {
      lock.recallCommit(cid, serverLockContexts, lockHelper);
    } finally {
      lockStore.checkIn(lock);
    }
  }

  public NotifiedWaiters notify(LockID lid, ClientID cid, ThreadID tid, NotifyAction action, NotifiedWaiters addNotifiedWaitersTo) {
    try {
      lockStatusRead();
      Assert.assertFalse("Notify was called before the LockManager was started.", isStarting());
      if (!isStarted()) {
        logger.warn("Notify was called after shutdown sequence commenced.");
      }
    } finally {
      unlockStatusRead();
    }

    ServerLock lock = lockStore.checkOut(lid);
    try {
      return lock.notify(cid, tid, action, addNotifiedWaitersTo, lockHelper);
    } catch (TCIllegalMonitorStateException e) {
      throw new RuntimeException(e);
    } finally {
      lockStore.checkIn(lock);
    }
  }

  public void wait(LockID lid, ClientID cid, ThreadID tid, long timeout) {
    try {
      lockStatusRead();
      Assert.assertFalse("Wait was called before the LockManager was started.", isStopped());
    } finally {
      unlockStatusRead();
    }

    ServerLock lock = lockStore.checkOut(lid);
    try {
      lock.wait(cid, tid, timeout, lockHelper);
    } catch (TCIllegalMonitorStateException e) {
      throw new RuntimeException(e);
    } finally {
      lockStore.checkIn(lock);
    }
  }

  public void reestablishState(ClientID cid, Collection<ClientServerExchangeLockContext> serverLockContexts) {
    lockStatusRead();
    try {
      Assert.assertTrue("Reestablish was called after the LockManager was started.", isStarting());
    } finally {
      unlockStatusRead();
    }

    for (ClientServerExchangeLockContext cselc : serverLockContexts) {
      LockID lid = cselc.getLockID();

      switch (cselc.getState().getType()) {
        case GREEDY_HOLDER:
        case HOLDER:
        case WAITER:
          ServerLock lock = lockStore.checkOut(lid);
          try {
            lock.reestablishState(cselc, lockHelper);
          } finally {
            lockStore.checkIn(lock);
          }
          break;
        case PENDING:
          lock(lid, (ClientID) cselc.getNodeID(), cselc.getThreadID(), cselc.getState().getLockLevel());
          break;
        case TRY_PENDING:
          tryLock(lid, (ClientID) cselc.getNodeID(), cselc.getThreadID(), cselc.getState().getLockLevel(), cselc
              .timeout());
          break;
      }
    }
  }

  public void clearAllLocksFor(ClientID cid) {
    LockIterator iter = lockStore.iterator();
    ServerLock lock = iter.getNextLock(null);
    while (lock != null) {
      if (lock.clearStateForNode(cid, lockHelper)) {
        iter.remove();
      }
      lock = iter.getNextLock(lock);
    }
    this.lockHelper.getLockStatsManager().clearAllStatsFor(cid);
  }

  public void enableLockStatsForNodeIfNeeded(ClientID cid) {
    this.lockHelper.getLockStatsManager().enableStatsForNodeIfNeeded(cid);
  }

  public LockMBean[] getAllLocks() {
    List<LockMBean> beansList = new ArrayList<LockMBean>();

    LockIterator iter = lockStore.iterator();
    ServerLock lock = iter.getNextLock(null);
    while (lock != null) {
      beansList.add(lock.getMBean(channelManager));
      lock = iter.getNextLock(lock);
    }

    return beansList.toArray(new LockMBean[beansList.size()]);
  }

  public void start() {
    try {
      lockStatusWrite();
      Assert.assertTrue(isStarting());
      setStatus(Status.STARTED);
      synchronized (this) {
        notifyAll();
      }

      // Done to make sure that all wait/try timers are started
      lockHelper.getLockTimer().start();

      // Go through the request queue and request locks
      for (Iterator i = this.lockRequestQueue.iterator(); i.hasNext();) {
        RequestLockContext ctxt = (RequestLockContext) i.next();
        switch (ctxt.getType()) {
          case LOCK:
            lock(ctxt.getLockID(), ctxt.getClientID(), ctxt.getThreadID(), ctxt.getRequestedLockLevel());
            break;
          case TRY_LOCK:
            tryLock(ctxt.getLockID(), ctxt.getClientID(), ctxt.getThreadID(), ctxt.getRequestedLockLevel(), ctxt
                .getTimeout());
        }
      }
      this.lockRequestQueue = null;
      this.lockHelper.getContextStateMachine().start();
    } finally {
      unlockStatusWrite();
    }
  }

  public void stop() throws InterruptedException {
    try {
      lockStatusWrite();
      while (isStarting()) {
        synchronized (this) {
          wait();
        }
      }
      Assert.assertTrue(isStarted());

      this.lockHelper.getContextStateMachine().stop();
      setStatus(Status.STOPPING);

      lockStore.clear();
      lockHelper.getLockTimer().shutdown();

      setStatus(Status.STOPPED);
    } finally {
      unlockStatusWrite();
    }
  }

  private void setStatus(Status status) {
    this.status = status;
  }

  private void queueRequest(LockID lid, ClientID cid, ThreadID tid, ServerLockLevel level, RequestType type,
                            long timeout) {
    RequestLockContext context = new RequestLockContext(lid, cid, tid, level, type, timeout);
    lockRequestQueue.add(context);
  }

  private boolean preChecksForLock(LockID lid, ClientID cid, ThreadID tid, ServerLockLevel level, RequestType type) {
    return checkAndQueue(lid, cid, tid, level, type, -1);
  }

  private boolean checkAndQueue(LockID lid, ClientID cid, ThreadID tid, ServerLockLevel level, RequestType type,
                                   long timeout) {
    lockStatusRead();
    try {
      if (isStarting()) {
        queueRequest(lid, cid, tid, level, type, timeout);
        return false;
      } else if (!isStarted()) { return false; }
      return this.channelManager.isActiveID(cid);
    } finally {
      unlockStatusRead();
    }
  }

  private boolean preCheckLogAndContinue(LockID lid, ClientID cid, ThreadID tid, String callType) {
    lockStatusRead();
    try {
      if (isStarting()) {
        logger.warn(callType + " message received during lock manager is starting -- ignoring the message.\n"
                    + "Message Context: [LockID=" + lid + ", NodeID=" + cid + ", ThreadID=" + tid + "]");
        return false;
      }
      return isStarted();
    } finally {
      unlockStatusRead();
    }
  }

  private boolean isStarted() {
    return status == Status.STARTED;
  }

  private boolean isStopped() {
    return status == Status.STOPPED;
  }

  private boolean isStarting() {
    return status == Status.STARTING;
  }

  private void lockStatusRead() {
    statusLock.readLock().lock();
  }

  private void unlockStatusRead() {
    statusLock.readLock().unlock();
  }

  private void lockStatusWrite() {
    statusLock.writeLock().lock();
  }

  private void unlockStatusWrite() {
    statusLock.writeLock().unlock();
  }

  private static class RequestLockContext {
    private final LockID          lockID;
    private final ClientID        nodeID;
    private final ThreadID        threadID;
    private final ServerLockLevel requestedLockLevel;
    private final RequestType     type;
    private final long            timeout;

    public RequestLockContext(LockID lockID, ClientID nodeID, ThreadID threadID, ServerLockLevel requestedLockLevel,
                              RequestType type, long timeout) {
      this.lockID = lockID;
      this.nodeID = nodeID;
      this.threadID = threadID;
      this.requestedLockLevel = requestedLockLevel;
      this.type = type;
      this.timeout = timeout;
    }

    public LockID getLockID() {
      return lockID;
    }

    public ClientID getClientID() {
      return nodeID;
    }

    public ThreadID getThreadID() {
      return threadID;
    }

    public ServerLockLevel getRequestedLockLevel() {
      return requestedLockLevel;
    }

    public RequestType getType() {
      return type;
    }

    public long getTimeout() {
      return timeout;
    }

    @Override
    public String toString() {
      return "RequestLockContext [ " + this.lockID + "," + this.nodeID + "," + this.threadID + ","
             + this.requestedLockLevel + ", " + this.type + ", " + this.timeout + " ]";
    }
  }

  public String dump() {
    StringWriter writer = new StringWriter();
    PrintWriter pw = new PrintWriter(writer);
    new PrettyPrinterImpl(pw).visit(this);
    writer.flush();
    return writer.toString();
  }

  public void dumpToLogger() {
    logger.info(dump());
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.println(getClass().getName());
    int size = 0;
    LockIterator iter = lockStore.iterator();
    ServerLock lock = iter.getNextLock(null);
    while (lock != null) {
      out.println(lock);
      size++;
      lock = iter.getNextLock(lock);
    }
    out.indent().println("locks: " + size);
    return out;
  }

  public DeadlockChain[] scanForDeadlocks() {
    final List chains = new ArrayList();
    DeadlockResults results = new DeadlockResults() {
      public void foundDeadlock(DeadlockChain chain) {
        chains.add(chain);
      }
    };

    scanForDeadlocks(results);

    return (DeadlockChain[]) chains.toArray(new DeadlockChain[chains.size()]);
  }

  public void scanForDeadlocks(DeadlockResults output) {
    throw new UnsupportedOperationException();
  }

  /**
   * To be used only in tests
   */
  public int getLockCount() {
    int size = 0;
    ServerLock oldLock = null;
    LockIterator iter = lockStore.iterator();
    ServerLock lock = iter.getNextLock(oldLock);
    while (lock != null) {
      oldLock = lock;
      size++;
      lock = iter.getNextLock(oldLock);
    }
    return size;
  }

  /**
   * To be used only in tests
   */
  public boolean hasPending(LockID lid) {
    AbstractServerLock lock = (AbstractServerLock) lockStore.checkOut(lid);
    boolean result = false;
    try {
      result = lock.hasPendingRequests();
    } finally {
      lockStore.checkIn(lock);
    }
    return result;
  }

  /**
   * To be used only in tests
   */
  public LockHelper getHelper() {
    return lockHelper;
  }
}
