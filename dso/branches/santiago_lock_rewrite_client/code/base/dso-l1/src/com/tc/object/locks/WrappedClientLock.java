/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.management.ClientLockStatManager;
import com.tc.net.ClientID;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.lockmanager.api.LockRequest;
import com.tc.object.lockmanager.api.TCLockTimer;
import com.tc.object.lockmanager.api.TryLockRequest;
import com.tc.object.lockmanager.api.WaitListener;
import com.tc.object.lockmanager.api.WaitLockRequest;
import com.tc.object.lockmanager.impl.TCLockTimerImpl;
import com.tc.object.locks.ServerLockContext.State;
import com.tc.object.tx.TimerSpec;

import java.util.ArrayList;
import java.util.Collection;

public class WrappedClientLock implements ClientLock {

  private static final boolean DEBUG = false;
  
  private static final String EMPTY_LOCK_TYPE = "<empty-lock-type>";
  private static final String EMPTY_LOCK_CONTEXT = "<empty-lock-context>";
  private static final TCLockTimer timer = new TCLockTimerImpl();
  
  private final LockID                                          lockId;
  private final com.tc.object.lockmanager.impl.ClientLock       wrappedLock;
  private final com.tc.object.lockmanager.api.RemoteLockManager remoteManager;
  
  public WrappedClientLock(LockID lock, RemoteLockManager remoteManager) {
    this.lockId = lock;
    this.remoteManager = new WrappedRemoteLockManager(remoteManager);
    this.wrappedLock = new com.tc.object.lockmanager.impl.ClientLock(lock, EMPTY_LOCK_TYPE, this.remoteManager, timer, ClientLockStatManager.NULL_CLIENT_LOCK_STAT_MANAGER);
  }
  
  public void award(ThreadID thread, ServerLockLevel level) {
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " Awarding " + lockId + " to " + thread + " [" + System.identityHashCode(this) + "]");
    if (ThreadID.VM_ID.equals(thread)) {
      wrappedLock.awardLock(thread, com.tc.object.lockmanager.api.LockLevel.makeGreedy(ServerLockLevel.toLegacyInt(level)));
    } else {
      wrappedLock.awardLock(thread, ServerLockLevel.toLegacyInt(level));
    }
    if (DEBUG) System.err.println(ManagerUtil.getClientID() + " Awarded " + lockId + "[" + System.identityHashCode(this) + "]");
  }

  public Collection<ClientServerExchangeLockContext> getStateSnapshot() {
    ClientID client;
    try {
      client = new ClientID(Long.parseLong(ManagerUtil.getClientID()));
    } catch (NumberFormatException e) {
      client = ClientID.NULL_ID;
    }    

    final Collection<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>();
    
    for (LockRequest lh : wrappedLock.addHoldersToAsLockRequests(new ArrayList<LockRequest>())) {
      switch (LockLevel.fromLegacyInt(lh.lockLevel())) {
        case READ:
          contexts.add(new ClientServerExchangeLockContext(lockId, client, lh.threadID(), State.HOLDER_READ));
          break;
        case WRITE:
        case SYNCHRONOUS_WRITE:
          contexts.add(new ClientServerExchangeLockContext(lockId, client, lh.threadID(), State.HOLDER_WRITE));
          break;
        case GREEDY_READ:
          contexts.add(new ClientServerExchangeLockContext(lockId, client, lh.threadID(), State.GREEDY_HOLDER_READ));
          break;
        case GREEDY_WRITE:
          contexts.add(new ClientServerExchangeLockContext(lockId, client, lh.threadID(), State.GREEDY_HOLDER_WRITE));
          break;
        default:
          break;
      }
    }

    for (WaitLockRequest wlr : wrappedLock.addAllWaitersTo(new ArrayList<WaitLockRequest>())) {
      contexts.add(new ClientServerExchangeLockContext(lockId, client, wlr.threadID(), State.WAITER, wlr.getTimerSpec().getMillis()));
    }

    for (LockRequest lr : wrappedLock.addAllPendingLockRequestsTo(new ArrayList<LockRequest>())) {
      switch (LockLevel.fromLegacyInt(lr.lockLevel())) {
        case READ:
          contexts.add(new ClientServerExchangeLockContext(lockId, client, lr.threadID(), State.PENDING_READ));
          break;
        case WRITE:
        case SYNCHRONOUS_WRITE:
          contexts.add(new ClientServerExchangeLockContext(lockId, client, lr.threadID(), State.PENDING_WRITE));
          break;
        default:
          break;
      }
    }
    
    for (TryLockRequest tlr : wrappedLock.addAllPendingTryLockRequestsTo(new ArrayList<TryLockRequest>())) {
      switch (LockLevel.fromLegacyInt(tlr.lockLevel())) {
        case READ:
          contexts.add(new ClientServerExchangeLockContext(lockId, client, tlr.threadID(), State.TRY_PENDING_READ));
          break;
        case WRITE:
        case SYNCHRONOUS_WRITE:
          contexts.add(new ClientServerExchangeLockContext(lockId, client, tlr.threadID(), State.TRY_PENDING_WRITE));
          break;
        default:
          break;
      }
    }
    
    return contexts;
  }

  public int pendingCount() {
    return wrappedLock.queueLength();
  }

  public int waitingCount() {
    return wrappedLock.waitLength();
  }

  public boolean isLocked(LockLevel level) {
    return wrappedLock.isHeld();
  }

  public boolean isLockedBy(ThreadID thread, LockLevel level) {
    return wrappedLock.isHeldBy(thread, LockLevel.toLegacyInt(level));
  }

  public int holdCount(LockLevel level) {
    return wrappedLock.localHeldCount(LockLevel.toLegacyInt(level));
  }

  public void lock(RemoteLockManager remote, ThreadID thread, LockLevel level) {
    wrappedLock.lock(thread, LockLevel.toLegacyInt(level), EMPTY_LOCK_CONTEXT);
  }

  public void lockInterruptibly(RemoteLockManager remote, ThreadID thread, LockLevel level) throws InterruptedException {
    wrappedLock.lockInterruptibly(thread, LockLevel.toLegacyInt(level), EMPTY_LOCK_CONTEXT);  }

  public void notified(ThreadID thread) {
    wrappedLock.notified(thread);
  }

  public boolean notify(RemoteLockManager remote, ThreadID thread) {
    return !wrappedLock.notify(thread, false).isNull();
  }

  public boolean notifyAll(RemoteLockManager remote, ThreadID thread) {
    return !wrappedLock.notify(thread, true).isNull();
  }

  public void recall(ServerLockLevel interest, int lease) {
    if (lease < 0) {
      wrappedLock.recall(ServerLockLevel.toLegacyInt(interest), wrappedLock);
    } else {
      wrappedLock.recall(ServerLockLevel.toLegacyInt(interest), wrappedLock, lease);
    }
  }

  public void refuse(ThreadID thread, ServerLockLevel level) {
    if (ThreadID.VM_ID.equals(thread)) {
      wrappedLock.cannotAwardLock(thread, com.tc.object.lockmanager.api.LockLevel.makeGreedy(ServerLockLevel.toLegacyInt(level)));
    } else {
      wrappedLock.cannotAwardLock(thread, ServerLockLevel.toLegacyInt(level));
    }
  }

  public boolean tryLock(RemoteLockManager remote, ThreadID thread, LockLevel level) {
    return wrappedLock.tryLock(thread, new TimerSpec(), LockLevel.toLegacyInt(level));
  }

  public boolean tryLock(RemoteLockManager remote, ThreadID thread, LockLevel level, long timeout) {
    return wrappedLock.tryLock(thread, new TimerSpec(timeout), LockLevel.toLegacyInt(level));
  }

  public void unlock(RemoteLockManager remote, ThreadID thread, LockLevel level) {
    wrappedLock.unlock(thread);
  }

  public void wait(RemoteLockManager remote, WaitListener listener, ThreadID thread) throws InterruptedException {
    wrappedLock.wait(thread, new TimerSpec(), lockId.javaObject(), listener);  }

  public void wait(RemoteLockManager remote, WaitListener listener, ThreadID thread, long timeout) throws InterruptedException {
    wrappedLock.wait(thread, new TimerSpec(timeout), lockId.javaObject(), listener);
  }

  public boolean garbageCollect() {
    return false;
  }  
}
