/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks;

import com.tc.exception.TCLockUpgradeNotSupportedError;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.L2LockStatsManager;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ServerLockContext;
import com.tc.object.locks.ServerLockLevel;
import com.tc.object.locks.ThreadID;
import com.tc.object.locks.ServerLockContext.State;
import com.tc.object.locks.ServerLockContext.Type;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.lockmanager.api.LockMBean;
import com.tc.objectserver.lockmanager.api.TCIllegalMonitorStateException;
import com.tc.objectserver.locks.context.LinkedServerLockContext;
import com.tc.objectserver.locks.context.SingleServerLockContext;
import com.tc.objectserver.locks.context.WaitLinkedServerLockContext;
import com.tc.objectserver.locks.context.WaitServerLockContext;
import com.tc.objectserver.locks.context.WaitSingleServerLockContext;
import com.tc.util.Assert;
import com.tc.util.SinglyLinkedList;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

/**
 * This class extends SinglyLinkedList which stores ServerLockContext. The ServerLockContexts are placed in the order of
 * greedy holders, pending requests, try lock requests and then waiters.
 */
public abstract class AbstractLock extends SinglyLinkedList<ServerLockContext> implements Lock {
  protected final LockID          lockID;
  protected static final TCLogger logger = TCLogging.getLogger(AbstractLock.class);

  public AbstractLock(LockID lockID) {
    this.lockID = lockID;
  }

  public void lock(ClientID cid, ThreadID tid, ServerLockLevel level, LockHelper helper) {
    int noOfPendingRequests = doPreLockCheckAndCalculations(cid, tid, level);
    recordLockRequestStat(cid, tid, noOfPendingRequests, helper);
    requestLock(cid, tid, level, Type.PENDING, -1, helper);
  }

  public void queryLock(ClientID cid, ThreadID tid, LockHelper helper) {
    LockResponseContext lrc = LockResponseContextFactory.createLockQueriedResponseContext(this.lockID, cid, tid, this
        .holderLevel(), this);
    helper.getLockSink().add(lrc);
  }

  public void interrupt(ClientID cid, ThreadID tid, LockHelper helper) {
    // check if waiters are present
    ServerLockContext waiter = remove(cid, tid);
    if (waiter == null) {
      logger.warn("Cannot interrupt: " + cid + "," + tid + " is not waiting.");
      return;
    }
    Assert.assertTrue(waiter.getState() == State.WAITER);
    moveWaiterToPending(waiter, helper);
  }

  public void notify(ClientID cid, ThreadID tid, NotifyAction action, NotifiedWaiters addNotifiedWaitersTo,
                     LockHelper helper) throws TCIllegalMonitorStateException {
    ServerLockContext holder = getPotentialNotifyHolders(cid, tid);
    validateNotifyState(cid, tid, holder, helper);

    List<ServerLockContext> waiters = removeWaiters(action);
    for (ServerLockContext waiter : waiters) {
      Assert.assertTrue(waiter.getState() == State.WAITER);
      switch (action) {
        case ALL:
          moveWaiterToPending(waiter, helper);
          break;
        case ONE:
          Assert.assertEquals(1, waiters.size());
          moveWaiterToPending(waiter, helper);
          break;
      }
      ClientServerExchangeLockContext cselc = new ClientServerExchangeLockContext(lockID, waiter.getClientID(), waiter
          .getThreadID(), State.WAITER);
      addNotifiedWaitersTo.addNotification(cselc);
    }
  }

  public void wait(ClientID cid, ThreadID tid, long timeout, LockHelper helper) throws TCIllegalMonitorStateException {
    moveFromHolderToWaiter(cid, tid, timeout, helper);
    processPendingRequests(helper);
  }

  public void unlock(ClientID cid, ThreadID tid, LockHelper helper) {
    // remove current hold
    ServerLockContext context = removeUnlockHolders(cid, tid);
    recordLockReleaseStat(cid, tid, helper);

    if (context == null) {
      logger.warn("An attempt was made to unlock:" + lockID + " for channelID:" + cid
                  + " This lock was not held. This could be do to that node being down so it may not be an error.");
      return;
    }
    Assert.assertTrue(context.isHolder());

    if (clearLockIfRequired(helper)) { return; }
    processPendingRequests(helper);
  }

  public void reestablishState(ClientServerExchangeLockContext cselc, LockHelper helper) {
    Assert.assertFalse(checkDuplicate((ClientID) cselc.getNodeID(), cselc.getThreadID()));
    switch (cselc.getState().getType()) {
      case GREEDY_HOLDER:
      case HOLDER:
        if (!canAwardRequest(cselc.getState().getLockLevel())) { throw new AssertionError(
                                                                                          "Lock could not be awarded as it is already held "
                                                                                              + cselc); }
        reestablishLock(cselc, helper);
        break;
      case WAITER:
        ServerLockContext context = createWaiterAndScheduleTask(cselc, helper);
        addWaiter(context, helper);
        break;
      default:
        throw new IllegalArgumentException("Called with wrong type = " + cselc.getState().getType());
    }
  }

  public LockMBean getMBean(DSOChannelManager channelManager) {
    // TODO
    return null;
  }

  public LockID getLockID() {
    return lockID;
  }

  public void timerTimeout(Object callbackObject) {
    ServerLockContext context = (ServerLockContext) callbackObject;
    LockHelper helper = ((WaitServerLockContext) callbackObject).getLockHelper();

    if (helper.getContextStateMachine().isStopped()) { return; }

    LockStore store = helper.getLockStore();
    store.checkOut(lockID);

    // Ignore contexts for which time out could not be canceled
    context = get(context.getClientID(), context.getThreadID());
    if (context == null
        || (context.getState().getType() != Type.TRY_PENDING && context.getState().getType() != Type.WAITER)) {
      store.checkIn(this);
      return;
    }

    // Remove from pending requests
    context = remove(context.getClientID(), context.getThreadID());

    if (context.isWaiter()) {
      waitTimeout(context, helper);
    } else {
      tryLockTimeout(context, helper);
    }

    store.checkIn(this);
  }

  // 
  private void tryLockTimeout(ServerLockContext context, LockHelper helper) {
    Assert.assertTrue(context.getState().getType() == Type.TRY_PENDING);
    cannotAward(context.getClientID(), context.getThreadID(), context.getState().getLockLevel(), helper);
  }

  private void waitTimeout(ServerLockContext context, LockHelper helper) {
    Assert.assertTrue(context.getState() == State.WAITER);
    // Add a wait Timeout message
    LockResponseContext lrc = LockResponseContextFactory.createLockWaitTimeoutResponseContext(this.lockID, context
        .getClientID(), context.getThreadID(), context.getState().getLockLevel());
    helper.getLockSink().add(lrc);
    lock(context.getClientID(), context.getThreadID(), ServerLockLevel.WRITE, helper);
  }

  protected ServerLockContext createWaiterAndScheduleTask(ClientServerExchangeLockContext cselc, LockHelper helper) {
    ServerLockContext context = createWaitOrTryPendingServerLockContext((ClientID) cselc.getNodeID(), cselc
        .getThreadID(), cselc.getState(), cselc.timeout(), helper);
    if (cselc.timeout() > 0) {
      TimerTask task = helper.getLockTimer().scheduleTimer(this, cselc.timeout(), context);
      ((WaitServerLockContext) context).setTimerTask(task);
    }
    return context;
  }

  protected boolean clearLockIfRequired(LockHelper helper) {
    if (isEmpty()) {
      LockStore store = helper.getLockStore();
      store.remove(lockID);
      return true;
    }
    return false;
  }

  protected void reestablishLock(ClientServerExchangeLockContext cselc, LockHelper helper) {
    awardLock(helper, createPendingContext((ClientID) cselc.getNodeID(), cselc.getThreadID(), cselc.getState()
        .getLockLevel(), helper), false);
  }

  protected void moveFromHolderToWaiter(ClientID cid, ThreadID tid, long timeout, LockHelper helper)
      throws TCIllegalMonitorStateException {
    ServerLockContext holder = remove(cid, tid);
    validateWaitState(cid, tid, holder, helper);

    recordLockReleaseStat(cid, tid, helper);
    ServerLockContext waiter = createWaitOrTryPendingServerLockContext(cid, tid, State.WAITER, timeout, helper);
    if (timeout > 0) {
      TimerTask task = helper.getLockTimer().scheduleTimer(this, timeout, waiter);
      ((WaitServerLockContext) waiter).setTimerTask(task);
    }
    addWaiter(waiter, helper);
  }

  protected void validateNotifyState(ClientID cid, ThreadID tid, ServerLockContext holder, LockHelper helper)
      throws TCIllegalMonitorStateException {
    validateWaitNotifyState(cid, tid, holder, helper, true);
  }

  protected void validateWaitState(ClientID cid, ThreadID tid, ServerLockContext holder, LockHelper helper)
      throws TCIllegalMonitorStateException {
    validateWaitNotifyState(cid, tid, holder, helper, false);
  }

  private void validateWaitNotifyState(ClientID cid, ThreadID tid, ServerLockContext holder, LockHelper helper,
                                       boolean isNotify) throws TCIllegalMonitorStateException {
    if (holder == null) {
      throw new TCIllegalMonitorStateException("No holder present for " + cid + "," + tid);
    } else if (holder.getState() != State.HOLDER_WRITE && holder.getState() != State.GREEDY_HOLDER_WRITE) {
      if (!isNotify) {
        add(holder, helper);
      }
      String message = "Holder not in correct state " + lockID + " " + holder;
      throw new TCIllegalMonitorStateException(message);
    }
  }

  protected void queue(ClientID cid, ThreadID tid, ServerLockLevel level, Type type, long timeout, LockHelper helper) {
    ServerLockContext context = null;
    switch (type) {
      case TRY_PENDING:
        context = createTryPendingServerLockContext(cid, tid, level, timeout, helper);
        Assert.assertTrue(timeout > 0);
        TimerTask task = helper.getLockTimer().scheduleTimer(this, timeout, context);
        ((WaitServerLockContext) context).setTimerTask(task);
        addTryPending(context, helper);
        break;
      case PENDING:
        context = createPendingContext(cid, tid, level, helper);
        addPending(context, helper);
        break;
      default:
        throw new IllegalStateException("Only pending and try pending state should be passed = " + type);
    }
  }

  protected abstract void requestLock(ClientID cid, ThreadID tid, ServerLockLevel level, Type type, long timeout,
                                      LockHelper helper);

  protected int doPreLockCheckAndCalculations(ClientID cid, ThreadID tid, ServerLockLevel reqLevel) {
    SinglyLinkedListIterator<ServerLockContext> iterator = iterator();
    int noOfPendingRequests = 0;
    while (iterator.hasNext()) {
      ServerLockContext context = iterator.next();
      switch (context.getState().getType()) {
        case GREEDY_HOLDER:
        case HOLDER:
          if (isUpgradeRequest(cid, tid, reqLevel, context)) { throw new TCLockUpgradeNotSupportedError(
                                                                                                        "Lock upgrade is not supported."
                                                                                                            + context); }
          if (isAlreadyHeldBySameContext(cid, tid, reqLevel, context)) { throw new AssertionError(
                                                                                                  "Client requesting already held lock!"
                                                                                                      + context); }
          break;
        case PENDING:
        case TRY_PENDING:
          noOfPendingRequests++;
          break;
        case WAITER:
          if (context.getClientID().equals(cid) && context.getThreadID().equals(tid)) { throw new AssertionError(
                                                                                                                 "This thread is already in wait state"); }
          break;
        default:
      }
    }
    return noOfPendingRequests;
  }

  protected void clearContextsForClient(ClientID cid, LockHelper helper) {
    SinglyLinkedListIterator<ServerLockContext> iter = iterator();

    // clear contexts and cancel timer tasks
    while (iter.hasNext()) {
      ServerLockContext context = iter.next();
      if (context.getClientID().equals(cid)) {
        iter.remove();
        switch (context.getState().getType()) {
          case WAITER:
          case TRY_PENDING:
            ((WaitServerLockContext) context).getTimerTask().cancel();
            break;
          default:
        }
      }
    }
  }

  private boolean isUpgradeRequest(ClientID cid, ThreadID tid, ServerLockLevel reqLevel, ServerLockContext holder) {
    if (reqLevel == ServerLockLevel.WRITE && this.isRead() && holder.getClientID().equals(cid)
        && holder.getThreadID().equals(tid)) { return true; }
    return false;
  }

  protected abstract ServerLockContext getPotentialNotifyHolders(ClientID cid, ThreadID tid);

  protected abstract ServerLockContext removeUnlockHolders(ClientID cid, ThreadID tid);

  private boolean isAlreadyHeldBySameContext(ClientID cid, ThreadID tid, ServerLockLevel reqLevel,
                                             ServerLockContext context) {
    if (reqLevel == context.getState().getLockLevel() && context.getClientID().equals(cid)
        && context.getThreadID().equals(tid)) { return true; }
    return false;
  }

  protected void moveWaiterToPending(ServerLockContext waiter, LockHelper helper) {
    int noOfPendingRequests = getNoOfPendingRequests();
    recordLockRequestStat(waiter.getClientID(), waiter.getThreadID(), noOfPendingRequests, helper);
    cancelTryLockOrWaitTimer(waiter, helper);
    // Add a pending request
    queue(waiter.getClientID(), waiter.getThreadID(), waiter.getState().getLockLevel(), Type.PENDING, -1, helper);
  }

  protected abstract void processPendingRequests(LockHelper helper);

  protected void awardLock(LockHelper helper, ServerLockContext request) {
    awardLock(helper, request, true);
  }

  protected void awardLock(LockHelper helper, ServerLockContext request, boolean toRespond) {
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
    awardLock(helper, request, state, toRespond);
  }

  /**
   * Assumption that this context has already been removed from the list
   */
  protected void awardLock(LockHelper helper, ServerLockContext request, State state, boolean toRespond) {
    ThreadID tid = request.getThreadID();

    // add this request to the front of the list
    cancelTryLockOrWaitTimer(request, helper);
    request = changeStateToHolder(request, state, helper);
    addHolder(request, helper);

    // record award to the stats
    recordLockAward(helper, request, tid);

    if (toRespond) {
      // create a lock response context and add it to the sink
      LockResponseContext lrc = LockResponseContextFactory.createLockAwardResponseContext(lockID,
                                                                                          request.getClientID(),
                                                                                          request.getThreadID(),
                                                                                          request.getState()
                                                                                              .getLockLevel());
      helper.getLockSink().add(lrc);
    }
  }

  protected void cannotAward(ClientID cid, ThreadID tid, ServerLockLevel requestedLockLevel, LockHelper helper) {
    LockResponseContext lrc = LockResponseContextFactory.createLockRejectedResponseContext(this.lockID, cid, tid,
                                                                                           requestedLockLevel);
    helper.getLockSink().add(lrc);
    recordLockRejectStat(cid, tid, helper);
  }

  protected void add(ServerLockContext request, LockHelper helper) {
    switch (request.getState().getType()) {
      case HOLDER:
      case GREEDY_HOLDER:
        addHolder(request, helper);
        break;
      case WAITER:
        addWaiter(request, helper);
        break;
      case PENDING:
        addPending(request, helper);
        break;
      case TRY_PENDING:
        addTryPending(request, helper);
        break;
    }
  }

  protected void addHolder(ServerLockContext request, LockHelper helper) {
    preStepsForAdd(helper);
    Assert.assertFalse(checkDuplicate(request));
    this.addFirst(request);
  }

  protected void addTryPending(ServerLockContext request, LockHelper helper) {
    preStepsForAdd(helper);
    Assert.assertFalse(checkDuplicate(request));

    SinglyLinkedListIterator<ServerLockContext> iter = iterator();
    while (iter.hasNext()) {
      switch (iter.next().getState().getType()) {
        case GREEDY_HOLDER:
        case HOLDER:
        case PENDING:
          break;
        case TRY_PENDING:
        case WAITER:
          iter.addPrevious(request);
          return;
      }
    }

    this.addLast(request);
  }

  protected void addPending(ServerLockContext request, LockHelper helper) {
    preStepsForAdd(helper);
    if (checkDuplicate(request)) {
      logger.debug("Ignoring existing Request " + request + " in Lock " + lockID);
      return;
    }

    SinglyLinkedListIterator<ServerLockContext> iter = iterator();
    while (iter.hasNext()) {
      switch (iter.next().getState().getType()) {
        case GREEDY_HOLDER:
        case HOLDER:
        case PENDING:
          break;
        case TRY_PENDING:
        case WAITER:
          iter.addPrevious(request);
          return;
      }
    }

    this.addLast(request);
  }

  protected void addWaiter(ServerLockContext request, LockHelper helper) {
    preStepsForAdd(helper);
    Assert.assertFalse(checkDuplicate(request));

    this.addLast(request);
  }

  /**
   * This list that is being maintained for contexts can have SingleServerLockContext, LinkedServerLockContext,
   * WaitSingleServerLockContext and WaitLinkedServerLockContext. In case the size of the queue is 1, a
   * SingleServerLockContext or WaitSingleServerLockContext is present in the list. This has been done to save space. In
   * order to continue adding more elements to the list, a change to LinkedServerLockContext and
   * WaitLinkedServerLockContext is required.
   */
  protected void preStepsForAdd(LockHelper helper) {
    if (isEmpty() || (!isEmpty() && (getFirst().getNext() != null || getFirst() instanceof LinkedServerLockContext))) { return; }

    // Since there is only 1 element in the list, a change is required.
    SingleServerLockContext context = (SingleServerLockContext) removeFirst();
    LinkedServerLockContext newContext = null;
    switch (context.getState().getType()) {
      case GREEDY_HOLDER:
      case HOLDER:
      case PENDING:
        newContext = new LinkedServerLockContext(context.getClientID(), context.getThreadID());
        newContext.setState(helper.getContextStateMachine(), context.getState());
        break;
      case TRY_PENDING:
      case WAITER:
        newContext = new WaitLinkedServerLockContext(context.getClientID(), context.getThreadID(),
                                                     ((WaitServerLockContext) context).getTimeout(),
                                                     ((WaitServerLockContext) context).getTimerTask(),
                                                     ((WaitServerLockContext) context).getLockHelper());
        newContext.setState(helper.getContextStateMachine(), context.getState());
        break;
    }
    this.addFirst(newContext);
  }

  protected boolean checkDuplicate(ClientID cid, ThreadID tid) {
    return checkDuplicate(new SingleServerLockContext(cid, tid));
  }

  protected boolean checkDuplicate(ServerLockContext context) {
    SinglyLinkedListIterator<ServerLockContext> iter = iterator();
    while (iter.hasNext()) {
      ServerLockContext temp = iter.next();
      if (context.equals(temp)) { return true; }
    }
    return false;
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

  protected ServerLockContext getNextRequestIfCanAward(LockHelper helper) {
    // Fetch the next pending context
    SinglyLinkedListIterator<ServerLockContext> iter = iterator();
    while (iter.hasNext()) {
      ServerLockContext request = iter.next();
      switch (request.getState().getType()) {
        case PENDING:
        case TRY_PENDING:
          if (canAwardRequest(request.getState().getLockLevel())) {
            iter.remove();
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
    return changeStateToHolderOrPending(request, state, helper);
  }

  private ServerLockContext changeStateToHolderOrPending(ServerLockContext request, State state, LockHelper helper) {
    switch (request.getState().getType()) {
      case WAITER:
      case TRY_PENDING:
        request = createSingleOrLinkedServerLockContext(request, helper);
        break;
      default:
    }
    request.setState(helper.getContextStateMachine(), state);
    return request;
  }

  protected void cancelTryLockOrWaitTimer(ServerLockContext request, LockHelper helper) {
    if (request.getState().getType() == Type.TRY_PENDING || request.getState() == State.WAITER) {
      WaitServerLockContext waitRequest = (WaitServerLockContext) request;
      if (waitRequest.getTimeout() > 0) {
        waitRequest.getTimerTask().cancel();
      }
    }
  }

  private ServerLockContext createSingleOrLinkedServerLockContext(ServerLockContext request, LockHelper helper) {
    ServerLockContext context = null;
    if (isEmpty()) {
      context = new SingleServerLockContext(request.getClientID(), request.getThreadID());
    } else {
      context = new LinkedServerLockContext(request.getClientID(), request.getThreadID());
    }
    context.setState(helper.getContextStateMachine(), request.getState());
    return context;
  }

  protected ServerLockContext createTryPendingServerLockContext(ClientID cid, ThreadID tid, ServerLockLevel level,
                                                                long timeout, LockHelper helper) {
    State state = null;
    switch (level) {
      case READ:
        state = State.TRY_PENDING_READ;
        break;
      case WRITE:
        state = State.TRY_PENDING_WRITE;
        break;
      default:
        throw new IllegalArgumentException("Nil lock level is not allowed ");
    }

    return createWaitOrTryPendingServerLockContext(cid, tid, state, timeout, helper);
  }

  protected ServerLockContext createWaitOrTryPendingServerLockContext(ClientID cid, ThreadID tid, State state,
                                                                      long timeout, LockHelper helper) {
    ServerLockContext context = null;
    if (isEmpty()) {
      context = new WaitSingleServerLockContext(cid, tid, timeout, helper);
    } else {
      context = new WaitLinkedServerLockContext(cid, tid, timeout, helper);
    }
    context.setState(helper.getContextStateMachine(), state);

    return context;
  }

  protected ServerLockContext createPendingContext(ClientID cid, ThreadID tid, ServerLockLevel level, LockHelper helper) {
    ServerLockContext context = null;
    if (isEmpty()) {
      context = new SingleServerLockContext(cid, tid);
    } else {
      context = new LinkedServerLockContext(cid, tid);
    }
    switch (level) {
      case READ:
        context.setState(helper.getContextStateMachine(), State.PENDING_READ);
        break;
      case WRITE:
        context.setState(helper.getContextStateMachine(), State.PENDING_WRITE);
        break;
      default:
        throw new IllegalArgumentException("Nil Lock Level is not allowed here");
    }
    return context;
  }

  // record stats
  protected void recordLockAward(LockHelper helper, ServerLockContext request, ThreadID tid) {
    L2LockStatsManager l2LockStats = helper.getLockStatsManager();
    l2LockStats.recordLockAwarded(lockID, request.getClientID(), tid, request.isGreedyHolder(), System
        .currentTimeMillis());
  }

  protected void recordLockHop(LockHelper helper) {
    L2LockStatsManager l2LockStats = helper.getLockStatsManager();
    l2LockStats.recordLockHopRequested(lockID);
  }

  protected void recordLockRequestStat(final ClientID cid, final ThreadID threadID, int noOfPendingRequests,
                                       LockHelper helper) {
    L2LockStatsManager l2LockStats = helper.getLockStatsManager();
    l2LockStats.recordLockRequested(lockID, cid, threadID, "", noOfPendingRequests);
  }

  protected void recordLockRejectStat(final ClientID cid, final ThreadID threadID, LockHelper helper) {
    L2LockStatsManager l2LockStats = helper.getLockStatsManager();
    l2LockStats.recordLockRejected(lockID, cid, threadID);
  }

  protected void recordLockReleaseStat(final NodeID nodeID, final ThreadID threadID, LockHelper helper) {
    L2LockStatsManager l2LockStats = helper.getLockStatsManager();
    l2LockStats.recordLockReleased(lockID, nodeID, threadID);
  }

  // Helper methods
  protected boolean hasHolders() {
    if (!isEmpty() && getFirst().isHolder()) { return true; }
    return false;
  }

  protected boolean hasOnlyReadHolders() {
    return isRead();
  }

  protected boolean hasWaiters() {
    if (!isEmpty() && getLast().isWaiter()) { return true; }
    return false;
  }

  protected boolean hasPendingRequests() {
    if (getNextPendingContext() == null) { return false; }
    return true;
  }

  protected boolean hasPendingWrites() {
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

  protected List<ServerLockContext> removeAllPendingReadRequests(LockHelper helper) {
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

  protected int getNoOfPendingRequests() {
    int count = 0;
    SinglyLinkedListIterator<ServerLockContext> iterator = iterator();
    while (iterator.hasNext()) {
      ServerLockContext context = iterator.next();
      switch (context.getState().getType()) {
        case PENDING:
        case TRY_PENDING:
          count++;
          break;
        case WAITER:
          return count;
        default:
      }
    }
    return count;
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

  protected ServerLockContext remove(ClientID cid, ThreadID tid) {
    ServerLockContext temp = null;
    SinglyLinkedListIterator<ServerLockContext> iter = iterator();
    while (iter.hasNext()) {
      temp = iter.next();
      if (temp.getClientID().equals(cid) && temp.getThreadID().equals(tid)) {
        iter.remove();
        return temp;
      }
    }
    return null;
  }

  protected ServerLockContext get(ClientID cid, ThreadID tid) {
    ServerLockContext temp = null;
    SinglyLinkedListIterator<ServerLockContext> iter = iterator();
    while (iter.hasNext()) {
      temp = iter.next();
      if (temp.getClientID().equals(cid) && temp.getThreadID().equals(tid)) { return temp; }
    }
    return null;
  }

  private List<ServerLockContext> removeWaiters(NotifyAction action) {
    List<ServerLockContext> contexts = new ArrayList<ServerLockContext>();
    SinglyLinkedListIterator<ServerLockContext> iterator = iterator();
    while (iterator.hasNext()) {
      ServerLockContext context = iterator.next();
      switch (context.getState().getType()) {
        case WAITER:
          iterator.remove();
          contexts.add(context);
          if (action == NotifyAction.ONE) { return contexts; }
          break;
        default:
      }
    }
    return contexts;
  }

  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("Lock Info");
    buffer.append("\n");
    buffer.append(lockID);
    buffer.append("\n");
    buffer.append("Contexts [ ");
    SinglyLinkedListIterator<ServerLockContext> iter = iterator();
    while (iter.hasNext()) {
      buffer.append(iter.next().toString());
      if (iter.hasNext()) {
        buffer.append(" , ");
      }
    }
    buffer.append(" ]");
    return buffer.toString();
  }
}