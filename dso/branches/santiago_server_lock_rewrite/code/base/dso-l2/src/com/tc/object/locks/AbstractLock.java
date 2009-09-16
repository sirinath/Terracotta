/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.management.L2LockStatsManager;
import com.tc.net.ClientID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.locks.ServerLockContext.State;
import com.tc.object.locks.ServerLockContext.Type;
import com.tc.object.locks.context.LinkedServerLockContext;
import com.tc.object.locks.context.SingleServerLockContext;
import com.tc.object.locks.context.WaitLinkedServerLockContext;
import com.tc.object.locks.context.WaitServerLockContext;
import com.tc.object.locks.context.WaitSingleServerLockContext;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.lockmanager.api.LockMBean;
import com.tc.objectserver.lockmanager.api.NotifiedWaiters;
import com.tc.util.Assert;
import com.tc.util.SinglyLinkedList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TimerTask;

/**
 * This class extends SinglyLinkedList which stores ServerLockContext. The ServerLockContexts are placed in the order of
 * greedy holders, pending requests, try lock requests and then waiters.
 */
public abstract class AbstractLock extends SinglyLinkedList<ServerLockContext> implements Lock {
  private final LockID lockID;

  public AbstractLock(LockID lockID) {
    this.lockID = lockID;
  }

  public void lock(ClientID cid, ThreadID tid, ServerLockLevel level, LockHelper helper) {
    //
  }

  public void tryLock(ClientID cid, ThreadID tid, ServerLockLevel level, long timeout, LockHelper helper) {
    //
  }

  public void queryLock(ClientID cid, ThreadID tid, LockHelper helper) {
    //
  }

  public void interrupt(ClientID cid, ThreadID tid, LockHelper helper) {
    //
  }

  public void unlock(ClientID cid, ThreadID tid, LockHelper helper) {
    //
  }

  public void recallCommit(ClientID cid, Collection<ClientServerExchangeLockContext> serverLockContexts,
                           LockHelper helper) {
    //
  }

  public void notify(ClientID cid, ThreadID tid, NotifyAction action, NotifiedWaiters addNotifiedWaitersTo,
                     LockHelper helper) {
    //
  }

  public void wait(ClientID cid, ThreadID tid, long timeout, LockHelper helper) {
    //
  }

  public void reestablishState(ClientID cid, ClientServerExchangeLockContext serverLockContext, LockHelper lockHelper) {
    //
  }

  public void clearStateForNode(ClientID cid) {
    //
  }

  public LockMBean getMBean(DSOChannelManager channelManager) {
    // TODO
    return null;
  }

  public LockID getLockID() {
    return lockID;
  }

  public void timerTimeout(Object callbackObject) {
    // TODO
  }

  protected abstract void processPendingRequests(LockHelper helper);

  protected void awardLock(LockHelper helper, ServerLockContext request) {
    State state = null;
    switch (request.getState().getLockLevel()) {
      case READ:
        state = State.HOLDER_READ;
        break;
      case WRITE:
        state = State.HOLDER_WRITE;
        break;
      default:
    }
    awardLock(helper, request, state);
  }

  /**
   * Assumption that this context has already been removed from the list
   */
  protected void awardLock(LockHelper helper, ServerLockContext request, State state) {
    // add this request to the front of the list
    request = changeStateToHolder(request, state, helper);
    
    // TODO: This might not be first always .. add after greedy
    addFirst(request);

    // create a lock response message and add it to the sink
    LockResponseContextFactory.createLockAwardResponseContext(lockID, request.getClientID(), request.getThreadID(),
                                                              request.getState().getLockLevel());
    // record it to the stats
    L2LockStatsManager l2LockStats = helper.getLockStatsManager();
    l2LockStats.recordLockAwarded(lockID, request.getClientID(), request.getThreadID(), request.isGreedyHolder(),
                                  System.currentTimeMillis());
  }

  protected abstract void awardAllReads(LockHelper helper, ServerLockContext request);

  protected boolean canAwardRequest(ServerLockLevel requestLevel) {
    switch (requestLevel) {
      case READ:
        if (!hasHolders() || isRead()) { return true; }
        break;
      case WRITE:
        return !hasHolders();
      default:
        throw new IllegalStateException("Nil Lock level not supported for request");
    }
    return false;
  }

  protected ServerLockContext getNextRequestIfCanAward() {
    // Fetch the next pending context
    SinglyLinkedListIterator<ServerLockContext> contexts = iterator();
    while (contexts.hasNext()) {
      ServerLockContext request = contexts.next();
      switch (request.getState().getType()) {
        case PENDING:
        case TRY_PENDING:
          if (canAwardRequest(request.getState().getLockLevel())) {
            contexts.remove();
            return request;
          }
          return null;
        case WAITER:
          return null;
        default:
      }
    }
    return null;
  }

  // Contexts methods
  protected ServerLockContext changeStateToHolder(ServerLockContext request, State state, LockHelper helper) {
    Assert.assertTrue(state.getType() == Type.GREEDY_HOLDER || state.getType() == Type.HOLDER);
    switch (request.getState().getType()) {
      case WAITER:
      case TRY_PENDING:
        cancelTryLockTimer(request, helper);
        request = createSingleOrLinkedServerLockContext(request, helper);
        break;
      default:
    }
    request.setState(helper.getContextStateMachine(), state);
    return request;
  }

  private void cancelTryLockTimer(ServerLockContext request, LockHelper helper) {
    ((WaitServerLockContext) request).getTimerTask().cancel();
  }

  protected ServerLockContext changeStateToWaiterOrTryPending(ServerLockContext request, State state,
                                                              LockHelper helper, TimerTask task, long timeout) {
    Assert.assertTrue(state.getType() == Type.WAITER || state.getType() == Type.TRY_PENDING);
    ServerLockContext context = createWaitServerLockContext(request, helper, task, timeout);
    context.setState(helper.getContextStateMachine(), state);
    return context;
  }

  private ServerLockContext createSingleOrLinkedServerLockContext(ServerLockContext request, LockHelper helper) {
    ServerLockContext context = null;
    if (isEmpty()) {
      context = new SingleServerLockContext(request.getClientID(), request.getThreadID());
    } else {
      context = new LinkedServerLockContext(request.getClientID(), request.getThreadID());
    }
    return context;
  }

  protected ServerLockContext createWaitServerLockContext(ServerLockContext request, LockHelper helper, TimerTask task,
                                                          long timeout) {
    ServerLockContext context = null;
    if (isEmpty()) {
      context = new WaitSingleServerLockContext(request.getClientID(), request.getThreadID(), task, timeout, helper);
    } else {
      context = new WaitLinkedServerLockContext(request.getClientID(), request.getThreadID(), task, timeout, helper);
    }
    context.setState(helper.getContextStateMachine(), request.getState());
    return context;
  }

  // Helper methods
  protected boolean hasHolders() {
    if (!isEmpty() && getFirst().isHolder()) { return true; }
    return false;
  }

  protected boolean hasOnlyReadHolders() {
    return isRead();
  }

  protected boolean hasOnlyWriteHolders() {
    return isWrite();
  }

  protected boolean hasWaiters() {
    if (!isEmpty() && getLast().isWaiter()) { return true; }
    return false;
  }

  protected boolean hasPendingRequests() {
    if (getNextPendingContext() == null) { return false; }
    return true;
  }

  protected boolean hasPendingWriters() {
    SinglyLinkedListIterator<ServerLockContext> iterator = iterator();
    while (iterator.hasNext()) {
      ServerLockContext context = iterator.next();
      switch (context.getState().getType()) {
        case PENDING:
        case TRY_PENDING:
          if (context.getState().getLockLevel() == ServerLockLevel.WRITE) { return true; }
          break;
        case WAITER:
          return false;
        default:
      }
    }
    return false;
  }

  protected List<ServerLockContext> getAllPendingReadRequests() {
    List<ServerLockContext> requests = new ArrayList<ServerLockContext>();
    SinglyLinkedListIterator<ServerLockContext> iterator = iterator();
    while (iterator.hasNext()) {
      ServerLockContext context = iterator.next();
      switch (context.getState().getType()) {
        case PENDING:
        case TRY_PENDING:
          if (context.getState().getLockLevel() == ServerLockLevel.READ) {
            iterator.remove();
            requests.add(context);
          }
          break;
        case WAITER:
          return requests;
        default:
      }
    }
    return requests;
  }

  protected boolean hasPendingRequestsFromOtherClients(ClientID cid) {
    SinglyLinkedListIterator<ServerLockContext> iterator = iterator();
    while (iterator.hasNext()) {
      ServerLockContext context = iterator.next();
      switch (context.getState().getType()) {
        case PENDING:
        case TRY_PENDING:
          if (!context.getClientID().equals(cid)) { return true; }
          break;
        case WAITER:
          return false;
        default:
      }
    }
    return false;
  }

  protected ServerLockContext getNextPendingContext() {
    SinglyLinkedListIterator<ServerLockContext> iterator = iterator();
    while (iterator.hasNext()) {
      ServerLockContext context = iterator.next();
      switch (context.getState().getType()) {
        case PENDING:
        case TRY_PENDING:
          return context;
        case WAITER:
          return null;
        default:
      }
    }
    return null;
  }

  protected boolean hasTryLockRequests() {
    SinglyLinkedListIterator<ServerLockContext> iterator = iterator();
    while (iterator.hasNext()) {
      ServerLockContext context = iterator.next();
      switch (context.getState().getType()) {
        case TRY_PENDING:
          return true;
        case WAITER:
          return false;
        default:
      }
    }
    return false;
  }

  protected ServerLockLevel holderLevel() {
    if (!hasHolders()) { return ServerLockLevel.NONE; }
    ServerLockContext holder = getFirst();
    return holder.getState().getLockLevel();
  }

  protected boolean isRead() {
    if (holderLevel() == ServerLockLevel.READ) { return true; }
    return false;
  }

  protected boolean isWrite() {
    if (holderLevel() == ServerLockLevel.WRITE) { return true; }
    return false;
  }
}
