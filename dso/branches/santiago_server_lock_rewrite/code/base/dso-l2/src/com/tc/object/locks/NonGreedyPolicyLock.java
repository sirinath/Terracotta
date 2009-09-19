/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.net.ClientID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.locks.ServerLockContext.Type;
import com.tc.objectserver.lockmanager.api.TCIllegalMonitorStateException;
import com.tc.util.Assert;

import java.util.Collection;
import java.util.List;

public final class NonGreedyPolicyLock extends AbstractLock {
  public NonGreedyPolicyLock(LockID lockID) {
    super(lockID);
  }

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
    List<ServerLockContext> contexts = getAllPendingReadRequests(helper);
    contexts.add(request);

    for (ServerLockContext context : contexts) {
      awardLock(helper, context);
    }
  }

  @Override
  protected void processPendingRequests(LockHelper helper) {
    ServerLockContext request = getNextRequestIfCanAward(helper);
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

  public void wait(ClientID cid, ThreadID tid, long timeout, LockHelper helper) throws TCIllegalMonitorStateException {
    moveFromHolderToWaiter(cid, tid, timeout, helper);
    processPendingRequests(helper);
  }

  public void clearStateForNode(ClientID cid, LockHelper helper) {
    clearContextsForClient(cid, helper);

    if (checkIfLockCanBeCleared(helper)) { return; }
    processPendingRequests(helper);
  }

  public void unlock(ClientID cid, ThreadID tid, LockHelper helper) {
    // remove current hold
    ServerLockContext context = remove(cid, tid, helper);
    recordLockReleaseStat(cid, tid, helper);
    
    Assert.assertNotNull(context);
    Assert.assertTrue(context.getState().getType() == Type.HOLDER);

    if (checkIfLockCanBeCleared(helper)) { return; }
    processPendingRequests(helper);
  }

  public void recallCommit(ClientID cid, Collection<ClientServerExchangeLockContext> serverLockContexts,
                           LockHelper helper) {
    // NO-OP
  }
  
  protected ServerLockContext getPotentialNotifyHolders(ClientID cid, ThreadID tid) {
    return get(cid, tid);
  }
}
