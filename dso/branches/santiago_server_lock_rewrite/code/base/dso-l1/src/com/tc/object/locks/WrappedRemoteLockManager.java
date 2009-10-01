/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.net.ClientID;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.lockmanager.api.LockRequest;
import com.tc.object.lockmanager.api.RemoteLockManager;
import com.tc.object.lockmanager.api.TryLockRequest;
import com.tc.object.lockmanager.api.WaitLockRequest;
import com.tc.object.tx.TimerSpec;

import com.tc.object.locks.ServerLockContext.State;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class WrappedRemoteLockManager implements RemoteLockManager {

  private final com.tc.object.locks.RemoteLockManager wrappedManager;
  
  public WrappedRemoteLockManager(com.tc.object.locks.RemoteLockManager wrapped) {
    this.wrappedManager = wrapped;
  }
  
  public void flush(LockID lockID) {
    wrappedManager.flush(lockID);
  }

  public void interrruptWait(LockID lockID, ThreadID threadID) {
    wrappedManager.interrupt(lockID, threadID);
  }

  public boolean isTransactionsForLockFlushed(LockID lockID, LockFlushCallback callback) {
    return wrappedManager.isTransactionsForLockFlushed(lockID, callback);
  }

  public void queryLock(LockID lockID, ThreadID threadID) {
    wrappedManager.query(lockID, threadID);
  }

  public void recallCommit(LockID lockID, Collection lockContext, Collection waitContext, Collection pendingRequests, Collection pendingTryLockRequests) {
    Collection<ClientServerExchangeLockContext> contexts = new ArrayList<ClientServerExchangeLockContext>(); 
    ClientID client;
    try {
      client = new ClientID(Long.parseLong(ManagerUtil.getClientID()));
    } catch (NumberFormatException e) {
      client = ClientID.NULL_ID;
    }
    
    for (Iterator i = lockContext.iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      State state = null;
      switch (request.lockLevel()) {
        case com.tc.object.lockmanager.api.LockLevel.GREEDY | com.tc.object.lockmanager.api.LockLevel.READ:
          state = State.GREEDY_HOLDER_READ;
          break;
        case com.tc.object.lockmanager.api.LockLevel.READ:
          state = State.HOLDER_READ;
          break;
        case com.tc.object.lockmanager.api.LockLevel.GREEDY | com.tc.object.lockmanager.api.LockLevel.WRITE:
          state = State.GREEDY_HOLDER_WRITE;
          break;
        case com.tc.object.lockmanager.api.LockLevel.WRITE:
          state = State.HOLDER_WRITE;
          break;
      }
      contexts.add(new ClientServerExchangeLockContext(request.lockID(), client, request.threadID(), state));
    }

    for (Iterator i = waitContext.iterator(); i.hasNext();) {
      WaitLockRequest request = (WaitLockRequest) i.next();
      contexts.add(new ClientServerExchangeLockContext(request.lockID(), client, request.threadID(), State.WAITER, request.getTimerSpec().getMillis()));
    }

    for (Iterator i = pendingRequests.iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();

      State state = null;
      switch (request.lockLevel()) {
        case com.tc.object.lockmanager.api.LockLevel.READ:
          state = State.PENDING_READ;
          break;
        case com.tc.object.lockmanager.api.LockLevel.WRITE:
          state = State.PENDING_WRITE;
          break;
      }
      contexts.add(new ClientServerExchangeLockContext(request.lockID(), client, request.threadID(), state));
    }

    for (Iterator i = pendingTryLockRequests.iterator(); i.hasNext();) {
      TryLockRequest request = (TryLockRequest) i.next();

      State state = null;
      switch (request.lockLevel()) {
        case com.tc.object.lockmanager.api.LockLevel.READ:
          state = State.TRY_PENDING_READ;
          break;
        case com.tc.object.lockmanager.api.LockLevel.WRITE:
          state = State.TRY_PENDING_WRITE;
          break;
      }
      contexts.add(new ClientServerExchangeLockContext(request.lockID(), client, request.threadID(), state, request.getTimerSpec().getMillis()));
    }
    
    wrappedManager.recallCommit(lockID, contexts);
  }

  public void releaseLock(LockID lockID, ThreadID threadID) {
    wrappedManager.unlock(lockID, threadID, null);
  }

  public void releaseLockWait(LockID lockID, ThreadID threadID, TimerSpec call) {
    wrappedManager.wait(lockID, threadID, call.getMillis());
  }

  public void requestLock(LockID lockID, ThreadID threadID, int lockType, String lockObjectType) {
    wrappedManager.lock(lockID, threadID, ServerLockLevel.fromLegacyInt(lockType));
  }

  public void tryRequestLock(LockID lockID, ThreadID threadID, TimerSpec timeout, int lockType, String lockObjectType) {
    wrappedManager.tryLock(lockID, threadID, ServerLockLevel.fromLegacyInt(lockType), timeout.getMillis());
  }

}
