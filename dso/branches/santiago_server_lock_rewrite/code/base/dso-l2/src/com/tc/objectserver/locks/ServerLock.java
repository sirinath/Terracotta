/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks;

import com.tc.net.ClientID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ServerLockContext;
import com.tc.object.locks.ServerLockLevel;
import com.tc.object.locks.ServerLockContext.State;
import com.tc.object.locks.ServerLockContext.Type;
import com.tc.objectserver.lockmanager.api.TCIllegalMonitorStateException;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ServerLock extends AbstractLock {
  private boolean isRecalled = false;

  public ServerLock(LockID lockID) {
    super(lockID);
  }

  public void tryLock(ClientID cid, ThreadID tid, ServerLockLevel level, long timeout, LockHelper helper) {
    int noOfPendingRequests = doPreLockCheckAndCalculations(cid, tid, level);
    recordLockRequestStat(cid, tid, noOfPendingRequests, helper);

    if (timeout <= 0 && !canAwardRequest(level)) {
      ServerLockContext holder = getGreedyHolder(cid);
      if (hasGreedyHolders() && holder == null) {
        recall(level, helper);
      }
      if (!canAwardGreedilyOnTheClient(level, holder)) {
        cannotAward(cid, tid, level, helper);
      }
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

  protected void changeWaiterToPending(ClientID cid, ThreadID tid, LockHelper helper, ServerLockContext waiter) {
    super.moveWaiterToPending(waiter, helper);

    // recall greedy holders if present
    if (hasGreedyHolders()) {
      recall(waiter.getState().getLockLevel(), helper);
    }
  }

  public boolean clearStateForNode(ClientID cid, LockHelper helper) {
    clearContextsForClient(cid, helper);

    if (!hasGreedyHolders()) {
      isRecalled = false;
    }
    processPendingRequests(helper);
    return isEmpty();
  }

  public void unlock(ClientID cid, ThreadID tid, LockHelper helper) {
    // remove current hold
    ServerLockContext context = remove(cid, tid);
    recordLockReleaseStat(cid, tid, helper);

    if (context == null) {
      logger.warn("An attempt was made to unlock:" + lockID + " for channelID:" + cid
                  + " This lock was not held. This could be do to that node being down so it may not be an error.");
      return;
    }
    Assert
        .assertTrue(context.getState().getType() == Type.HOLDER || context.getState().getType() == Type.GREEDY_HOLDER);

    if (clearLockIfRequired(helper)) { return; }
    if (!hasGreedyHolders()) {
      isRecalled = false;
    }
    processPendingRequests(helper);
  }

  @Override
  protected void reestablishLock(ClientServerExchangeLockContext cselc, LockHelper helper) {
    // if greedy request then award greedily and don't respond
    if (cselc.getThreadID().equals(ThreadID.VM_ID)) {
      awardLockGreedily(helper, createPendingContext((ClientID) cselc.getNodeID(), cselc.getThreadID(), cselc
          .getState().getLockLevel(), helper), false);
    } else {
      super.reestablishLock(cselc, helper);
    }
  }

  @Override
  protected void processPendingRequests(LockHelper helper) {
    if (isRecalled) { return; }

    ServerLockContext request = getNextRequestIfCanAward(helper);
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

  @Override
  protected void addHolder(ServerLockContext request, LockHelper helper) {
    preStepsForAdd(helper);
    Assert.assertFalse(checkDuplicate(request));

    switch (request.getState().getType()) {
      case GREEDY_HOLDER:
        this.addFirst(request);
        return;
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

    ArrayList<ClientID> listOfClients = new ArrayList<ClientID>();
    for (ServerLockContext context : contexts) {
      if (!listOfClients.contains(context.getClientID())) {
        awardLockGreedily(helper, context);
        listOfClients.add(context.getClientID());
      }
    }

    if (hasPendingWrite) {
      recall(ServerLockLevel.WRITE, helper);
    }
  }

  public void wait(ClientID cid, ThreadID tid, long timeout, LockHelper helper) throws TCIllegalMonitorStateException {
    moveFromHolderToWaiter(cid, tid, timeout, helper);

    if (!hasGreedyHolders()) {
      isRecalled = false;
    }
    processPendingRequests(helper);
  }

  private boolean canAwardGreedilyOnTheClient(ServerLockLevel level, ServerLockContext holder) {
    return holder != null
           && (holder.getState().getLockLevel() == ServerLockLevel.WRITE || level == ServerLockLevel.READ);
  }

  public void recallCommit(ClientID cid, Collection<ClientServerExchangeLockContext> serverLockContexts,
                           LockHelper helper) {
    ServerLockContext greedyHolder = remove(cid, ThreadID.VM_ID);
    Assert.assertNotNull(greedyHolder);

    recordLockReleaseStat(cid, ThreadID.VM_ID, helper);

    for (ClientServerExchangeLockContext cselc : serverLockContexts) {
      switch (cselc.getState().getType()) {
        case GREEDY_HOLDER:
          throw new IllegalArgumentException("Greedy type not allowed here");
        case HOLDER:
          awardLock(helper, createPendingContext(cid, cselc.getThreadID(), cselc.getState().getLockLevel(), helper), false);
          break;
        case PENDING:
          queue(cid, cselc.getThreadID(), cselc.getState().getLockLevel(), Type.PENDING, -1, helper);
          break;
        case TRY_PENDING:
          if (cselc.timeout() <= 0) {
            cannotAward(cid, cselc.getThreadID(), cselc.getState().getLockLevel(), helper);
          } else {
            queue(cid, cselc.getThreadID(), cselc.getState().getLockLevel(), Type.TRY_PENDING, cselc.timeout(), helper);
          }
          break;
        case WAITER:
          ServerLockContext waiter = createWaiterAndScheduleTask(cselc, helper);
          addWaiter(waiter, helper);
          break;
      }
    }

    if (!hasGreedyHolders()) {
      isRecalled = false;
    } else if (!isRecalled && hasPendingRequests()) {
      recall(ServerLockLevel.WRITE, helper);
    }

    // Also check if the lock can be removed
    if (clearLockIfRequired(helper)) { return; }
    processPendingRequests(helper);
  }

  private void recall(ServerLockLevel level, LockHelper helper) {
    if (isRecalled) { return; }

    List<ServerLockContext> greedyHolders = getGreedyHolders();
    for (ServerLockContext greedyHolder : greedyHolders) {
      LockResponseContext lrc = LockResponseContextFactory.createLockRecallResponseContext(lockID, greedyHolder
          .getClientID(), greedyHolder.getThreadID(), level);
      helper.getLockSink().add(lrc);
      isRecalled = true;
    }

    recordLockHop(helper);
  }

  private void awardLockGreedily(LockHelper helper, ServerLockContext request) {
    awardLockGreedily(helper, request, true);
  }

  private void awardLockGreedily(LockHelper helper, ServerLockContext request, boolean toRespond) {
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
    removeNonGreedyHoldersAndPendingOfSameClient(request, helper);

    // greedy requests should have their thread ids as vm id
    request.setThreadID(ThreadID.VM_ID);

    awardLock(helper, request, state, toRespond);
  }

  private void removeNonGreedyHoldersAndPendingOfSameClient(ServerLockContext context, LockHelper helper) {
    ClientID cid = context.getClientID();
    SinglyLinkedListIterator<ServerLockContext> iterator = iterator();
    while (iterator.hasNext()) {
      ServerLockContext next = iterator.next();
      switch (next.getState().getType()) {
        case GREEDY_HOLDER:
          break;
        case TRY_PENDING:
          if (cid.equals(next.getClientID())) {
            cancelTryLockOrWaitTimer(next, helper);
            iterator.remove();
          }
          break;
        case PENDING:
        case HOLDER:
          if (cid.equals(next.getClientID())) {
            iterator.remove();
          }
          break;
        case WAITER:
          return;
      }
    }
  }

  protected ServerLockContext getPotentialNotifyHolders(ClientID cid, ThreadID tid) {
    ServerLockContext context = get(cid, tid);
    if (context == null) {
      context = get(cid, ThreadID.VM_ID);
    }
    return context;
  }

  private boolean hasGreedyHolders() {
    if (!isEmpty() && getFirst().isGreedyHolder()) return true;
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
