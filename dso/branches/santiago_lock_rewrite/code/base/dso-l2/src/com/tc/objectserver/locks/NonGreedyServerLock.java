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

  @Override
  protected void awardAllReads(LockHelper helper, ServerLockContext request) {
    List<ServerLockContext> contexts = removeAllPendingReadRequests(helper);
    contexts.add(request);

    for (ServerLockContext context : contexts) {
      awardLock(helper, context);
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

  protected ServerLockContext getNotifyHolder(ClientID cid, ThreadID tid) {
    return get(cid, tid);
  }

  @Override
  protected ServerLockContext removeUnlockHolder(ClientID cid, ThreadID tid) {
    return remove(cid, tid);
  }
}
