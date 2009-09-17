/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.net.ClientID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.locks.ServerLockContext.State;
import com.tc.object.locks.ServerLockContext.Type;

import java.util.List;

import junit.framework.Assert;

public final class NonGreedyPolicyLock extends AbstractLock {
  public NonGreedyPolicyLock(LockID lockID) {
    super(lockID);
  }

  @Override
  public void tryLock(ClientID cid, ThreadID tid, ServerLockLevel level, long timeout, LockHelper helper) {
    int noOfPendingRequests = doPreLockCheckAndCalculations(cid, tid, level);
    recordLockRequestStat(cid, tid, noOfPendingRequests, helper);

    if (timeout <= 0 && !canAwardRequest(level)) {
      cannotAward(cid, tid, level, helper);
      return;
    }

    requestLock(cid, tid, level, Type.TRY_PENDING, timeout, helper);
  }

  @Override
  public void interrupt(ClientID cid, ThreadID tid, LockHelper helper) {
    // check if waiters are present
    ServerLockContext waiter = remove(cid, tid);
    if (waiter == null) {
      logger.warn("Cannot interrupt: " + cid + "," + tid + " is not waiting.");
      return;
    }
    Assert.assertTrue(waiter.getState() == State.WAITER);

    int noOfPendingRequests = getNoOfPendingRequests();
    recordLockRequestStat(cid, tid, noOfPendingRequests, helper);
    cancelTryLockOrWaitTimer(waiter, helper);
    // Add a pending request
    queue(cid, tid, waiter.getState().getLockLevel(), Type.PENDING, -1, helper);
  }

  @Override
  protected void requestLock(ClientID cid, ThreadID tid, ServerLockLevel level, Type type, long timeout,
                             LockHelper helper) {
    // Lock granting logic:
    // 0. If no one is holding this lock, go ahead and award it
    // 1. If only a read lock is held and no write locks are pending, and another read
    // (and only read) lock is requested, award it. If Write locks are pending, we dont want to
    // starve the WRITES by keeping on awarding READ Locks.
    // 2. Else the request must be queued (ie. added to pending list)

    switch (level) {
      case WRITE:
        if (!hasHolders()) {
          awardLock(helper, createPendingContext(cid, tid, level, helper));
          return;
        }
        break;
      case READ:
        if (!hasHolders() || (hasOnlyReadHolders() && !hasPendingWrites())) {
          awardLock(helper, createPendingContext(cid, tid, level, helper));
          return;
        }
        break;
      default:
        throw new IllegalArgumentException("Nil lock level is not supported");
    }

    queue(cid, tid, level, type, timeout, helper);
  }

  @Override
  protected void awardAllReads(LockHelper helper, ServerLockContext request) {
    List<ServerLockContext> contexts = getAllPendingReadRequests();
    contexts.add(request);

    for (ServerLockContext context : contexts) {
      awardLock(helper, context);
    }
  }

  @Override
  protected void processPendingRequests(LockHelper helper) {
    ServerLockContext request = getNextRequestIfCanAward();
    if (request == null) { return; }

    switch (request.getState().getLockLevel()) {
      case READ:
        awardAllReads(helper, request);
        break;
      case WRITE:
        awardLock(helper, request);
        break;
      default:
        throw new IllegalStateException("Nil lock level not supported here");
    }
  }
}
