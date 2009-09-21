/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import com.tc.async.api.EventContext;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.management.L2LockStatsManager;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.lockmanager.api.LockFlushCallback;
import com.tc.object.lockmanager.api.LockRequest;
import com.tc.object.lockmanager.api.RemoteLockManager;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.api.TryLockRequest;
import com.tc.object.lockmanager.api.WaitLockRequest;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.Lock;
import com.tc.object.locks.LockFactory;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockManagerImpl;
import com.tc.object.locks.LockResponseContext;
import com.tc.object.locks.NonGreedyPolicyLock;
import com.tc.object.locks.NotifiedWaiters;
import com.tc.object.locks.ServerLockLevel;
import com.tc.object.locks.ServerLockContext.State;
import com.tc.object.msg.ClientHandshakeMessageImpl;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionProvider;
import com.tc.object.tx.TimerSpec;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.lockmanager.api.NullChannelManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class ClientServerLockManagerGlue implements RemoteLockManager, Runnable {

  private LockManagerImpl         serverLockManager;
  protected ClientLockManagerImpl clientLockManager;

  protected TestSink              sink;
  private final ClientID          clientID = new ClientID(1);
  protected boolean               stop     = false;
  protected Thread                eventNotifier;

  protected final SessionProvider sessionProvider;

  protected ClientServerLockManagerGlue(SessionProvider sessionProvider, TestSink sink, String threadName) {
    super();
    this.sessionProvider = sessionProvider;
    this.sink = sink;
    eventNotifier = new Thread(this, threadName);
    eventNotifier.setDaemon(true);
    eventNotifier.start();
  }

  public void requestLock(LockID lockID, ThreadID threadID, int lockType, String lockObjectType) {
    ServerLockLevel level = ServerLockLevel.fromLegacyInt(lockType);
    serverLockManager.lock(lockID, clientID, threadID, level);
  }

  public void releaseLock(LockID lockID, ThreadID threadID) {
    serverLockManager.unlock(lockID, clientID, threadID);
  }

  public void releaseLockWait(LockID lockID, ThreadID threadID, TimerSpec call) {
    serverLockManager.wait(lockID, clientID, threadID, call.getMillis());
  }

  public void recallCommit(LockID lockID, Collection lockContext, Collection waitContext, Collection pendingRequests,
                           Collection pendingTryLockRequests) {
    Collection<ClientServerExchangeLockContext> serverContexts = new ArrayList();
    for (Iterator i = lockContext.iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      State state = null;
      switch (ServerLockLevel.fromLegacyInt(request.lockLevel())) {
        case READ:
          state = State.HOLDER_READ;
          break;
        case WRITE:
          state = State.HOLDER_WRITE;
          break;
        default:
          throw new IllegalStateException();
      }
      ClientServerExchangeLockContext ctxt = new ClientServerExchangeLockContext(request.lockID(), clientID, request
          .threadID(), state);
      serverContexts.add(ctxt);
    }

    for (Iterator i = waitContext.iterator(); i.hasNext();) {
      WaitLockRequest request = (WaitLockRequest) i.next();

      State state = null;
      switch (ServerLockLevel.fromLegacyInt(request.lockLevel())) {
        case WRITE:
          state = State.WAITER;
          break;
        default:
          throw new IllegalStateException();
      }
      ClientServerExchangeLockContext ctxt = new ClientServerExchangeLockContext(request.lockID(), clientID, request
          .threadID(), state, request.getTimerSpec().getMillis());
      serverContexts.add(ctxt);
    }

    for (Iterator i = pendingRequests.iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      State state = null;
      switch (ServerLockLevel.fromLegacyInt(request.lockLevel())) {
        case READ:
          state = State.PENDING_READ;
          break;
        case WRITE:
          state = State.PENDING_WRITE;
          break;
        default:
          throw new IllegalStateException();
      }
      ClientServerExchangeLockContext ctxt = new ClientServerExchangeLockContext(request.lockID(), clientID, request
          .threadID(), state);
      serverContexts.add(ctxt);
    }

    for (Iterator i = pendingTryLockRequests.iterator(); i.hasNext();) {
      TryLockRequest request = (TryLockRequest) i.next();
      State state = null;
      switch (ServerLockLevel.fromLegacyInt(request.lockLevel())) {
        case READ:
          state = State.TRY_PENDING_READ;
          break;
        case WRITE:
          state = State.TRY_PENDING_WRITE;
          break;
        default:
          throw new IllegalStateException();
      }
      ClientServerExchangeLockContext ctxt = new ClientServerExchangeLockContext(request.lockID(), clientID, request
          .threadID(), state, request.getTimerSpec().getMillis());
      serverContexts.add(ctxt);
    }

    serverLockManager.recallCommit(lockID, clientID, serverContexts);
  }

  public void flush(LockID lockID) {
    return;
  }

  public boolean isTransactionsForLockFlushed(LockID lockID, LockFlushCallback callback) {
    return true;
  }

  public void set(ClientLockManagerImpl clmgr, LockManagerImpl slmgr) {
    this.clientLockManager = clmgr;
    this.serverLockManager = slmgr;
    this.serverLockManager.start();
  }

  public void run() {
    while (!stop) {
      EventContext ec = null;
      try {
        ec = sink.take();
      } catch (InterruptedException e) {
        //
      }
      if (ec instanceof LockResponseContext) {
        LockResponseContext lrc = (LockResponseContext) ec;
        if (lrc.isLockAward()) {
          clientLockManager.awardLock(lrc.getNodeID(), sessionProvider.getSessionID(lrc.getNodeID()), lrc.getLockID(),
                                      lrc.getThreadID(), ServerLockLevel.toLegacyInt(lrc.getLockLevel()));
        }
      }
      // ToDO :: implment WaitContext etc..
    }
  }

  public LockManagerImpl restartServer() {
    this.serverLockManager = new LockManagerImpl(sink, L2LockStatsManager.NULL_LOCK_STATS_MANAGER,
                                                 new NullChannelManager(), new TestServerLockFactory());
    clientLockManager.pause(GroupID.ALL_GROUPS, 1);
    ClientHandshakeMessageImpl handshakeMessage = new ClientHandshakeMessageImpl(SessionID.NULL_ID, null,
                                                                                 new TCByteBufferOutputStream(), null,
                                                                                 TCMessageType.CLIENT_HANDSHAKE_MESSAGE);
    clientLockManager.initializeHandshake(this.clientID, GroupID.NULL_ID, handshakeMessage);

    serverLockManager.reestablishState(this.clientID, handshakeMessage.getLockContexts());
    this.serverLockManager.start();
    clientLockManager.unpause(GroupID.ALL_GROUPS, 0);
    return this.serverLockManager;
  }

  public void notify(LockID lockID1, ThreadID tx2, boolean all) {
    NotifiedWaiters waiters = new NotifiedWaiters();
    Lock.NotifyAction action = all ? Lock.NotifyAction.ALL : Lock.NotifyAction.ONE;
    serverLockManager.notify(lockID1, clientID, tx2, action, waiters);
    Set s = waiters.getNotifiedFor(clientID);
    for (Iterator i = s.iterator(); i.hasNext();) {
      ClientServerExchangeLockContext lc = (ClientServerExchangeLockContext) i.next();
      clientLockManager.notified(lc.getLockID(), lc.getThreadID());
    }
  }

  public void stop() {
    stop = true;
    eventNotifier.interrupt();
  }

  public void queryLock(LockID lockID, ThreadID threadID) {
    serverLockManager.queryLock(lockID, clientID, threadID);
  }

  public void tryRequestLock(LockID lockID, ThreadID threadID, int lockType, String lockObjectType) {
    ServerLockLevel level = ServerLockLevel.fromLegacyInt(lockType);
    serverLockManager.tryLock(lockID, clientID, threadID, level, -1);
  }

  public void interrruptWait(LockID lockID, ThreadID threadID) {
    serverLockManager.interrupt(lockID, clientID, threadID);

  }

  public void tryRequestLock(LockID lockID, ThreadID threadID, TimerSpec timeout, int lockType, String lockObjectType) {
    ServerLockLevel level = ServerLockLevel.fromLegacyInt(lockType);
    serverLockManager.tryLock(lockID, clientID, threadID, level, timeout.getMillis());
  }

  public static class TestServerLockFactory implements LockFactory {
    public Lock createLock(LockID lid) {
      return new NonGreedyPolicyLock(lid);
    }
  }
}
