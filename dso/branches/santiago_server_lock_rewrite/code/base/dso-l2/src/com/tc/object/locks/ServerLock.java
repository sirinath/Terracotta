/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.object.locks.ServerLockContext.State;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ServerLock extends AbstractLock {
  private boolean isRecalled = false;

  public ServerLock(LockID lockID) {
    super(lockID);
  }

  protected boolean hasGreedyHolders() {
    ServerLockContext context = getFirst();
    if (context != null && context.isGreedyHolder()) { return true; }
    return false;
  }

  @Override
  protected void processPendingRequests(LockHelper helper) {
    if (isRecalled) { return; }

    ServerLockContext request = getNextRequestIfCanAward();
    if (request == null) { return; }

    switch (request.getState().getLockLevel()) {
      case READ:
        awardAllReads(helper, request);
        break;
      case WRITE:
        if (hasWaiters()) {
          awardLock(helper, request);
        } else {
          awardLockGreedily(helper, request);
          // recall if it has pending requests from other clients
          if (hasPendingRequestsFromOtherClients(request.getClientID())) {
            recall(ServerLockLevel.WRITE);
          }
        }
        break;
      default:
    }
  }

  private void recall(ServerLockLevel write) {
    // TODO
  }

  protected void awardLockGreedily(LockHelper helper, ServerLockContext request) {
    State state = null;
    switch (request.getState().getLockLevel()) {
      case READ:
        state = State.GREEDY_HOLDER_READ;
        break;
      case WRITE:
        state = State.GREEDY_HOLDER_WRITE;
        break;
      default:
    }
    awardLock(helper, request, state);
  }

  protected void awardAllReads(LockHelper helper, ServerLockContext request) {
    // fetch all the read requests
    List<ServerLockContext> contexts = new ArrayList<ServerLockContext>();
    SinglyLinkedListIterator<ServerLockContext> iterator = iterator();
    boolean hasPendingWrite = false;
    while (iterator.hasNext()) {
      ServerLockContext context = iterator.next();
      if (context.isPending()) {
        switch (context.getState().getLockLevel()) {
          case READ:
            iterator.remove();
            contexts.add(context);
            break;
          case WRITE:
            hasPendingWrite = true;
            break;
          default:
        }
      }
    }

    // Since this request was already removed from the list
    contexts.add(request);

    for (Iterator<ServerLockContext> iter = contexts.iterator(); iter.hasNext();) {
      ServerLockContext context = iter.next();
      awardLockGreedily(helper, context);
    }

    if (hasPendingWrite) {
      recall(ServerLockLevel.WRITE);
    }
  }
}
