/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks;

import com.tc.net.ClientID;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ServerLockContext;
import com.tc.object.locks.ServerLockLevel;
import com.tc.object.locks.ThreadID;
import com.tc.object.locks.ServerLockContext.Type;

import java.util.Collection;
import java.util.List;

public final class NonGreedyServerLock extends AbstractServerLock {
  public NonGreedyServerLock(LockID lockID) {
    super(lockID);
  }

  public void tryLock(ClientID cid, ThreadID tid, ServerLockLevel level, long timeout, LockHelper helper) {
    int noOfPendingRequests = validateAndGetNumberOfPending(cid, tid, level);
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
    List<ServerLockContext> contexts = removeAllPendingReadRequests(helper);
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

  public boolean clearStateForNode(ClientID cid, LockHelper helper) {
    clearContextsForClient(cid, helper);
    processPendingRequests(helper);

    return isEmpty();
  }

  public void recallCommit(ClientID cid, Collection<ClientServerExchangeLockContext> serverLockContexts,
                           LockHelper helper) {
    // NO-OP
  }

  protected ServerLockContext getPotentialNotifyHolders(ClientID cid, ThreadID tid) {
    return get(cid, tid);
  }

  @Override
  protected ServerLockContext removeUnlockHolders(ClientID cid, ThreadID tid) {
    return remove(cid, tid);
  }
}
