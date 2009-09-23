/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.L2LockStatsManager;
import com.tc.net.ClientID;
import com.tc.object.locks.ThreadID;
import com.tc.object.locks.Lock.NotifyAction;
import com.tc.object.locks.LockStore.LockIterator;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.lockmanager.api.LockMBean;
import com.tc.objectserver.lockmanager.api.NotifiedWaiters;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class LockManagerImpl implements LockManager {
  private enum Status {
    STARTING, STARTED, STOPPING, STOPPED
  }

  private enum RequestType {
    LOCK, TRY_LOCK
  }

  private final LockStore          lockStore;
  private final DSOChannelManager  channelManager;
  private final LockHelper         lockHelper;
  private List<RequestLockContext> lockRequestQueue;
  private volatile Status          status = Status.STARTING;

  private static final TCLogger    logger = TCLogging.getLogger(LockManagerImpl.class);

  public LockManagerImpl(Sink lockSink, LockFactory lockFactory, L2LockStatsManager statsManager,
                         DSOChannelManager channelManager) {
    this.lockStore = new LockStore(lockFactory);
    this.lockHelper = new LockHelper(statsManager, lockSink, lockStore);
    this.channelManager = channelManager;
  }

  public void lock(LockID lid, ClientID cid, ThreadID tid, ServerLockLevel level) {
    if (!preCheckStateAndContinue(lid, cid, tid, level, RequestType.LOCK)) { return; }

    Lock lock = lockStore.checkOut(lid);
    lock.lock(cid, tid, level, lockHelper);
    lockStore.checkIn(lock);
  }

  public void tryLock(LockID lid, ClientID cid, ThreadID tid, ServerLockLevel level, long timeout) {
    if (!preCheckStateAndContinue(lid, cid, tid, level, RequestType.TRY_LOCK, timeout)) { return; }

    Lock lock = lockStore.checkOut(lid);
    lock.tryLock(cid, tid, level, timeout, lockHelper);
    lockStore.checkIn(lock);
  }

  public void unlock(LockID lid, ClientID cid, ThreadID tid) {
    if (!preCheckStateLogAndContinue(lid, cid, tid, "Unlock")) { return; }

    // Lock might be removed from the lock store in the call to the unlock
    // TODO remember to see if the lock can be removed from lock store
    Lock lock = lockStore.checkOut(lid);
    lock.unlock(cid, tid, lockHelper);
    lockStore.checkIn(lock);
  }

  public void queryLock(LockID lid, ClientID cid, ThreadID tid) {
    if (!preCheckStateLogAndContinue(lid, cid, tid, "QueryLock")) { return; }

    Lock lock = lockStore.checkOut(lid);
    lock.queryLock(cid, tid, lockHelper);
    lockStore.checkIn(lock);
  }

  public void interrupt(LockID lid, ClientID cid, ThreadID tid) {
    if (!preCheckStateLogAndContinue(lid, cid, tid, "Interrupt")) { return; }

    Lock lock = lockStore.checkOut(lid);
    lock.interrupt(cid, tid, lockHelper);
    lockStore.checkIn(lock);
  }

  public void recallCommit(LockID lid, ClientID cid, Collection<ClientServerExchangeLockContext> serverLockContexts) {
    if (!preCheckStateLogAndContinue(lid, cid, ThreadID.VM_ID, "Recall Commit")) { return; }
    if (!this.channelManager.isActiveID(cid)) {
      logger.warn("Ignoring Recall Commit message from disconnected client : " + cid + " : Lock ID : " + lid);
      return;
    }

    // TODO remember to see if the lock can be removed from lock store
    Lock lock = lockStore.checkOut(lid);
    lock.recallCommit(cid, serverLockContexts, lockHelper);
    lockStore.checkIn(lock);
  }

  public void notify(LockID lid, ClientID cid, ThreadID tid, NotifyAction action, NotifiedWaiters addNotifiedWaitersTo) {
    Assert.assertFalse("Notify was called before the LockManager was started.", isStarting());
    if (!isStarted()) {
      logger.warn("Notify was called after shutdown sequence commenced.");
    }

    // TODO in lock remember to check that if the lock state is not empty
    Lock lock = lockStore.checkOut(lid);
    lock.notify(cid, tid, action, addNotifiedWaitersTo, lockHelper);
    lockStore.checkIn(lock);
  }

  public void wait(LockID lid, ClientID cid, ThreadID tid, long timeout) {
    Assert.assertFalse("Wait was called before the LockManager was started.", isStoppped());

    // TODO in lock remember to check that if the lock state is not empty
    Lock lock = lockStore.checkOut(lid);
    lock.wait(cid, tid, timeout, lockHelper);
    lockStore.checkIn(lock);
  }

  public void reestablishState(ClientID cid, Collection<ClientServerExchangeLockContext> serverLockContexts) {
    Assert.assertFalse("Wait was called before the LockManager was started.", isStarting());

    for (Iterator<ClientServerExchangeLockContext> iter = serverLockContexts.iterator(); iter.hasNext();) {
      ClientServerExchangeLockContext context = iter.next();
      LockID lid = context.getLockID();

      Lock lock = lockStore.checkOut(lid);
      lock.reestablishState(cid, context, lockHelper);
      lockStore.checkIn(lock);
    }
  }

  public void clearAllLocksFor(ClientID cid) {
    // TODO check to see if the lock can be removed
    Lock oldLock = null;
    LockIterator iterator = lockStore.iterator();
    Lock lock = iterator.getNextLock(oldLock);
    do {
      lock.clearStateForNode(cid);
      oldLock = lock;
      lock = iterator.getNextLock(oldLock);
    } while (lock != null);
  }

  public void enableLockStatsForNodeIfNeeded(ClientID cid) {
    this.lockHelper.getLockStatsManager().enableStatsForNodeIfNeeded(cid);
  }

  public LockMBean[] getAllLocks() {
    // TODO
    return null;
  }

  public void start() {
    Assert.assertEquals(Status.STARTING, status);
    synchronized (this) {
      status = Status.STARTED;
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
    this.lockRequestQueue.clear();
  }

  public void stop() throws InterruptedException {
    while (status == Status.STARTING) {
      synchronized (this) {
        wait();
      }
    }
    Assert.assertEquals(Status.STARTED, status);

    synchronized (this) {
      this.status = Status.STOPPING;
    }

    lockStore.clear();
    lockHelper.getLockTimer().shutdown();

    synchronized (this) {
      this.status = Status.STOPPED;
    }
  }

  private void queueRequest(LockID lid, ClientID cid, ThreadID tid, ServerLockLevel level, RequestType type,
                            long timeout) {
    synchronized (this) {
      if (lockRequestQueue == null) {
        lockRequestQueue = new ArrayList<RequestLockContext>();
      }
      RequestLockContext context = new RequestLockContext(lid, cid, tid, level, type, timeout);
      lockRequestQueue.add(context);
    }
  }

  private boolean preCheckStateAndContinue(LockID lid, ClientID cid, ThreadID tid, ServerLockLevel level,
                                           RequestType type) {
    return preCheckStateAndContinue(lid, cid, tid, level, type, -1);
  }

  private boolean preCheckStateAndContinue(LockID lid, ClientID cid, ThreadID tid, ServerLockLevel level,
                                           RequestType type, long timeout) {
    if (status != Status.STARTING) {
      queueRequest(lid, cid, tid, level, type, timeout);
      return false;
    } else if (status != Status.STARTED) { return false; }
    if (!this.channelManager.isActiveID(cid)) { return false; }
    return true;
  }

  private boolean preCheckStateLogAndContinue(LockID lid, ClientID cid, ThreadID tid, String callType) {
    if (isStarting()) {
      logger.warn(callType + " message received during lock manager is starting -- ignoring the message.\n"
                  + "Message Context: [LockID=" + lid + ", NodeID=" + cid + ", ThreadID=" + tid + "]");
      return false;
    } else if (!isStarted()) { return false; }
    return true;
  }

  private boolean isStarted() {
    return status == Status.STARTED;
  }

  private boolean isStoppped() {
    return status == Status.STOPPED;
  }

  private boolean isStarting() {
    return status == Status.STARTING;
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
}
