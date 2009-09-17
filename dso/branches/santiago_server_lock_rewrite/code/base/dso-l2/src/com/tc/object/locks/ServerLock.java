/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.net.ClientID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.locks.ServerLockContext.State;
import com.tc.object.locks.ServerLockContext.Type;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

public final class ServerLock extends AbstractLock {
  private boolean isRecalled = false;

  public ServerLock(LockID lockID) {
    super(lockID);
  }

  @Override
  public void tryLock(ClientID cid, ThreadID tid, ServerLockLevel level, long timeout, LockHelper helper) {
    int noOfPendingRequests = doPreLockCheckAndCalculations(cid, tid, level);
    recordLockRequestStat(cid, tid, noOfPendingRequests, helper);

    if (timeout <= 0 && !canAwardRequest(level)) {
      ServerLockContext holder = getGreedyHolder(cid);
      if (hasGreedyHolders() && holder == null) {
        recall(level, helper);
      }
      cannotAward(cid, tid, level, helper);
      return;
    }

    requestLock(cid, tid, level, Type.TRY_PENDING, timeout, helper);
  }

  @Override
  protected void requestLock(ClientID cid, ThreadID tid, ServerLockLevel level, Type type, long timeout,
                             LockHelper helper) {
    // Ignore if a client can fulfill request
    // Or if Recalled and the client doesn't already hold the greedy lock => q request
    ServerLockContext holder = getGreedyHolder(cid);
    if (canAwardGreedilyOnTheClient(level, holder)) {
      return;
    } else if (isRecalled) {
      // add to pending until recall process is complete, those who hold the lock greedily will send the
      // pending state during recall commit.
      if (holder == null) {
        queue(cid, tid, level, type, timeout, helper);
      }
      return;
    }

    // Lock granting logic:
    // 0. If no one is holding this lock, go ahead and award it
    // 1. If only a read lock is held and no write locks are pending, and another read
    // (and only read) lock is requested, award it. If Write locks are pending, we dont want to
    // starve the WRITES by keeping on awarding READ Locks.
    // 2. Else the request must be queued (i.e. added to pending list)

    switch (level) {
      case WRITE:
        if (!hasHolders()) {
          if (hasWaiters()) {
            awardLock(helper, createPendingContext(cid, tid, level, helper));
            return;
          } else {
            awardLockGreedily(helper, createPendingContext(cid, tid, level, helper));
            return;
          }
        }
        break;
      case READ:
        if (!hasHolders() || (hasOnlyReadHolders() && !hasPendingWrites())) {
          awardLockGreedily(helper, createPendingContext(cid, tid, level, helper));
          return;
        }
        break;
      default:
        throw new IllegalArgumentException("Nil lock level is not supported");
    }

    // Queue part
    if (hasGreedyHolders()) {
      recall(level, helper);
    }

    queue(cid, tid, level, type, timeout, helper);
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

    // recall greedy holders if present
    if (hasGreedyHolders()) {
      recall(waiter.getState().getLockLevel(), helper);
    }
  }

  @Override
  public void clearStateForNode(ClientID cid) {
    super.clearStateForNode(cid);
    if (!hasGreedyHolders()) {
      isRecalled = false;
    }
  }

  private boolean canAwardGreedilyOnTheClient(ServerLockLevel level, ServerLockContext holder) {
    return holder != null
           && (holder.getState().getLockLevel() == ServerLockLevel.WRITE || level == ServerLockLevel.READ);
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
            recall(ServerLockLevel.WRITE, helper);
          }
        }
        break;
      default:
    }
  }

  private void recall(ServerLockLevel level, LockHelper helper) {
    if (isRecalled) { return; }

    List<ServerLockContext> greedyHolders = getGreedyHolders();
    for (ServerLockContext greedyHolder : greedyHolders) {
      LockResponseContext lrc = LockResponseContextFactory.createLockRecallResponseContext(lockID, greedyHolder
          .getClientID(), greedyHolder.getThreadID(), level);
      helper.getLockSink().add(lrc);
    }

    recordLockHop(helper);
  }

  private void awardLockGreedily(LockHelper helper, ServerLockContext request) {
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
    // remove holders (from the same client) who have given the lock non greedily till now
    removeNonGreedyHoldersOfSameClient(request);

    // greedy requests should have their thread ids as vm id
    request.setThreadID(ThreadID.VM_ID);

    awardLock(helper, request, state);
  }

  @Override
  protected void addHolder(ServerLockContext request, LockHelper helper) {
    preStepsForAdd(helper);
    switch (request.getState().getType()) {
      case GREEDY_HOLDER:
        this.addFirst(request);
        break;
      case HOLDER:
        SinglyLinkedListIterator<ServerLockContext> iter = iterator();
        while (iter.hasNext()) {
          switch (iter.next().getState().getType()) {
            case GREEDY_HOLDER:
              break;
            default:
              iter.addPrevious(request);
              return;
          }
        }

        this.addLast(request);
        break;
      default:
        throw new IllegalStateException("Only holders context should be passed " + request.getState());
    }
  }

  @Override
  protected void awardAllReads(LockHelper helper, ServerLockContext request) {
    // fetch all the read requests and check if has write pending requests as well
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

    // Since this request was already removed from the list, so this is added to the
    // list as well
    contexts.add(request);

    for (ServerLockContext context : contexts) {
      awardLockGreedily(helper, context);
    }

    if (hasPendingWrite) {
      recall(ServerLockLevel.WRITE, helper);
    }
  }

  private void removeNonGreedyHoldersOfSameClient(ServerLockContext context) {
    ClientID cid = context.getClientID();
    SinglyLinkedListIterator<ServerLockContext> iterator = iterator();
    while (iterator.hasNext()) {
      ServerLockContext next = iterator.next();
      switch (next.getState().getType()) {
        case GREEDY_HOLDER:
          break;
        case HOLDER:
          if (cid.equals(next.getClientID())) {
            iterator.remove();
          }
          break;
        default:
          return;
      }
    }
  }

  private boolean hasGreedyHolders() {
    ServerLockContext context = getFirst();
    if (context != null && context.isGreedyHolder()) { return true; }
    return false;
  }

  private List<ServerLockContext> getGreedyHolders() {
    List<ServerLockContext> contexts = new ArrayList<ServerLockContext>();
    SinglyLinkedListIterator<ServerLockContext> iterator = iterator();
    while (iterator.hasNext()) {
      ServerLockContext context = iterator.next();
      switch (context.getState().getType()) {
        case GREEDY_HOLDER:
          contexts.add(context);
          break;
        default:
          return contexts;
      }
    }
    return contexts;
  }

  private ServerLockContext getGreedyHolder(ClientID cid) {
    SinglyLinkedListIterator<ServerLockContext> iterator = iterator();
    while (iterator.hasNext()) {
      ServerLockContext context = iterator.next();
      switch (context.getState().getType()) {
        case GREEDY_HOLDER:
          // can award greedily
          if (context.getClientID().equals(cid)) { return context; }
          break;
        default:
          return null;
      }
    }
    return null;
  }
}
