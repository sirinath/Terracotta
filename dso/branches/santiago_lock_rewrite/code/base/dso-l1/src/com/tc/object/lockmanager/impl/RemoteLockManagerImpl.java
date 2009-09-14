/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import com.tc.net.GroupID;
import com.tc.object.gtx.ClientGlobalTransactionManager;
import com.tc.object.lockmanager.api.LockContext;
import com.tc.object.lockmanager.api.LockFlushCallback;
import com.tc.object.lockmanager.api.LockRequest;
import com.tc.object.lockmanager.api.RemoteLockManager;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.api.TryLockContext;
import com.tc.object.lockmanager.api.TryLockRequest;
import com.tc.object.lockmanager.api.WaitContext;
import com.tc.object.lockmanager.api.WaitLockRequest;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ServerLockLevel;
import com.tc.object.msg.LockRequestMessage;
import com.tc.object.msg.LockRequestMessageFactory;
import com.tc.object.tx.TimerSpec;
import com.tc.util.Assert;

import java.util.Collection;
import java.util.Iterator;

/**
 * Responsible for communicating to server to request/release locks
 */
public class RemoteLockManagerImpl implements RemoteLockManager {

  private LockRequestMessageFactory            lockRequestMessageFactory;
  private final ClientGlobalTransactionManager gtxManager;
  private final GroupID                        groupID;

  public RemoteLockManagerImpl(GroupID groupID, LockRequestMessageFactory lrmf,
                               ClientGlobalTransactionManager gtxManager) {
    this.groupID = groupID;
    this.lockRequestMessageFactory = lrmf;
    this.gtxManager = gtxManager;
  }

  public void requestLock(LockID lockID, ThreadID threadID, int lockType, String lockObjectType) {
    Assert.assertTrue(com.tc.object.lockmanager.api.LockLevel.isDiscrete(lockType));
    LockRequestMessage req = createRequest();
    req.initializeLock(lockID, threadID, ServerLockLevel.fromLegacyInt(lockType), lockObjectType);
    send(req);
  }

  // used for tests
  protected void send(LockRequestMessage req) {
    req.send();
  }

  public void tryRequestLock(LockID lockID, ThreadID threadID, TimerSpec timeout, int lockType, String lockObjectType) {
    Assert.assertTrue(com.tc.object.lockmanager.api.LockLevel.isDiscrete(lockType));
    LockRequestMessage req = createRequest();
    req.initializeTryLock(lockID, threadID, timeout, ServerLockLevel.fromLegacyInt(lockType), lockObjectType);
    send(req);
  }

  public void releaseLock(LockID lockID, ThreadID threadID) {
    LockRequestMessage req = createRequest();
    req.initializeUnlock(lockID, threadID);
    send(req);
  }

  public void releaseLockWait(LockID lockID, ThreadID threadID, TimerSpec call) {
    LockRequestMessage req = createRequest();
    req.initializeWait(lockID, threadID, call);
    send(req);
  }

  public void queryLock(LockID lockID, ThreadID threadID) {
    LockRequestMessage req = createRequest();
    req.initializeQuery(lockID, threadID);
    send(req);
  }

  public void interrruptWait(LockID lockID, ThreadID threadID) {
    LockRequestMessage req = createRequest();
    req.initializeInterruptWait(lockID, threadID);
    send(req);
  }

  private LockRequestMessage createRequest() {
    // return (LockRequestMessage) channel.createMessage(TCMessageType.LOCK_REQUEST_MESSAGE);
    return lockRequestMessageFactory.newLockRequestMessage(groupID);
  }

  public void recallCommit(LockID lockID, Collection lockContext, Collection waitContext, Collection pendingRequests,
                           Collection pendingTryLockRequests) {
    LockRequestMessage req = createRequest();
    req.initializeRecallCommit(lockID);
    for (Iterator i = lockContext.iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      LockContext ctxt = new LockContext(request.lockID(), req.getSourceNodeID(), request.threadID(), request
          .lockLevel(), request.lockType());
      req.addLockContext(ctxt);
    }

    for (Iterator i = waitContext.iterator(); i.hasNext();) {
      WaitLockRequest request = (WaitLockRequest) i.next();
      WaitContext ctxt = new WaitContext(request.lockID(), req.getSourceNodeID(), request.threadID(), request
          .lockLevel(), request.lockType(), request.getTimerSpec());
      req.addWaitContext(ctxt);
    }

    for (Iterator i = pendingRequests.iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      LockContext ctxt = new LockContext(request.lockID(), req.getSourceNodeID(), request.threadID(), request
          .lockLevel(), request.lockType());
      req.addPendingLockContext(ctxt);
    }

    for (Iterator i = pendingTryLockRequests.iterator(); i.hasNext();) {
      TryLockRequest request = (TryLockRequest) i.next();
      LockContext ctxt = new TryLockContext(request.lockID(), req.getSourceNodeID(), request.threadID(), request
          .lockLevel(), request.lockType(), request.getTimerSpec());
      req.addPendingTryLockContext(ctxt);
    }

    send(req);
  }

  public void flush(LockID lockID) {
    gtxManager.flush(lockID);
  }

  public boolean isTransactionsForLockFlushed(LockID lockID, LockFlushCallback callback) {
    return gtxManager.isTransactionsForLockFlushed(lockID, callback);
  }
}
