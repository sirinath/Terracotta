/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.async.impl.NullSink;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.management.L2LockStatsManager;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.lockmanager.api.LockContext;
import com.tc.object.lockmanager.api.TryLockContext;
import com.tc.object.lockmanager.api.WaitContext;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ServerLockLevel;
import com.tc.object.msg.ClientHandshakeMessageImpl;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionProvider;
import com.tc.object.tx.TimerSpec;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.context.LockResponseContext;
import com.tc.objectserver.lockmanager.api.NotifiedWaiters;
import com.tc.objectserver.lockmanager.api.NullChannelManager;
import com.tc.objectserver.lockmanager.impl.LockManagerImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class ClientServerLockManagerGlue implements RemoteLockManager, Runnable {

  private static final Sink       NULL_SINK = new NullSink();

  private LockManagerImpl         serverLockManager;
  protected ClientLockManagerImpl clientLockManager;

  protected TestSink              sink;
  private final ClientID          clientID  = new ClientID(1);
  protected boolean               stop      = false;
  protected Thread                eventNotifier;

  protected final SessionProvider sessionProvider;

  public ClientServerLockManagerGlue(SessionProvider sessionProvider) {
    this(sessionProvider, new TestSink(), "ClientServerLockManagerGlue");
  }

  protected ClientServerLockManagerGlue(SessionProvider sessionProvider, TestSink sink, String threadName) {
    super();
    this.sessionProvider = sessionProvider;
    this.sink = sink;
    eventNotifier = new Thread(this, threadName);
    eventNotifier.setDaemon(true);
    eventNotifier.start();
  }

  public void lock(LockID lockID, ThreadID threadID, ServerLockLevel level) {
    serverLockManager.requestLock(lockID, clientID, threadID, ServerLockLevel.toLegacyInt(level), "", sink);
  }

  public void unlock(LockID lockID, ThreadID threadID, ServerLockLevel level) {
    serverLockManager.unlock(lockID, clientID, threadID);
  }

  public void wait(LockID lockID, ThreadID threadID, long timeout) {
    if (timeout < 0) {
      serverLockManager.wait(lockID, clientID, threadID, new TimerSpec(), sink);      
    } else {
      serverLockManager.wait(lockID, clientID, threadID, new TimerSpec(timeout), sink);
    }
  }

  public void recallCommit(LockID lockID, Collection<ClientServerExchangeLockContext> contexts) {
    Collection<LockContext> serverLC = new ArrayList<LockContext>();
    Collection<WaitContext> serverWC = new ArrayList<WaitContext>();
    Collection<LockContext> serverPC = new ArrayList<LockContext>();
    Collection<TryLockContext> serverPTC = new ArrayList<TryLockContext>();

    for (ClientServerExchangeLockContext c : contexts) {
      switch (c.getState().getType()) {
        case HOLDER:
        case GREEDY_HOLDER:
          serverLC.add(c.getLockContext());
          break;
        case WAITER:
          serverWC.add(c.getWaitContext());
          break;
        case PENDING:
          serverPC.add(c.getLockContext());
          break;
        case TRY_PENDING:
          serverPTC.add(c.getTryWaitContext());
      }
    }

    serverLockManager.recallCommit(lockID, clientID, serverLC, serverWC, serverPC, serverPTC, sink);
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
          clientLockManager.award(GroupID.NULL_ID, sessionProvider.getSessionID(lrc.getNodeID()), lrc.getLockID(),
                                      lrc.getThreadID(), ServerLockLevel.fromLegacyInt(lrc.getLockLevel()));
        }
      }
      // ToDO :: implment WaitContext etc..
    }
  }

  public LockManagerImpl restartServer() {
    int policy = this.serverLockManager.getLockPolicy();
    this.serverLockManager = new LockManagerImpl(new NullChannelManager(), L2LockStatsManager.NULL_LOCK_STATS_MANAGER);
    clientLockManager.pause(GroupID.ALL_GROUPS, 1);
    ClientHandshakeMessageImpl handshakeMessage = new ClientHandshakeMessageImpl(SessionID.NULL_ID, null,
                                                                                 new TCByteBufferOutputStream(), null,
                                                                                 TCMessageType.CLIENT_HANDSHAKE_MESSAGE);
    clientLockManager.initializeHandshake(GroupID.NULL_ID, GroupID.ALL_GROUPS, handshakeMessage);

    for (Iterator i = handshakeMessage.getLockContexts().iterator(); i.hasNext();) {      
      ClientServerExchangeLockContext context = ((ClientServerExchangeLockContext) i.next());
      switch (context.getState().getType()) {
        case GREEDY_HOLDER:
        case HOLDER:
          serverLockManager.reestablishLock(context.getLockID(), context.getNodeID(), context.getThreadID(), ServerLockLevel
              .toLegacyInt(context.getState().getLockLevel()), NULL_SINK);
          break;
        case WAITER:
          TimerSpec spec = context.timeout() == -1 ? new TimerSpec() : new TimerSpec(context.timeout());
          serverLockManager.reestablishWait(context.getLockID(), context.getNodeID(), context.getThreadID(), ServerLockLevel
              .toLegacyInt(context.getState().getLockLevel()), spec, NULL_SINK);
          break;
        case PENDING:
          serverLockManager.requestLock(context.getLockID(), context.getNodeID(), context.getThreadID(), ServerLockLevel
              .toLegacyInt(context.getState().getLockLevel()), "", NULL_SINK);
          break;
        case TRY_PENDING:
          spec = context.timeout() == -1 ? new TimerSpec() : new TimerSpec(context.timeout());
          serverLockManager.tryRequestLock(context.getLockID(), context.getNodeID(), context.getThreadID(), ServerLockLevel
              .toLegacyInt(context.getState().getLockLevel()), "", spec, NULL_SINK);
          break;
      }
    }

    if (policy == LockManagerImpl.ALTRUISTIC_LOCK_POLICY) {
      this.serverLockManager.setLockPolicy(policy);
    }
    this.serverLockManager.start();
    clientLockManager.unpause(GroupID.ALL_GROUPS, 0);
    return this.serverLockManager;
  }

  public void notify(LockID lockID1, ThreadID tx2, boolean all) {
    NotifiedWaiters waiters = new NotifiedWaiters();
    serverLockManager.notify(lockID1, clientID, tx2, all, waiters);
    Set s = waiters.getNotifiedFor(clientID);
    for (Iterator i = s.iterator(); i.hasNext();) {
      LockContext lc = (LockContext) i.next();
      clientLockManager.notified(lc.getLockID(), lc.getThreadID());
    }
  }

  public void stop() {
    stop = true;
    eventNotifier.interrupt();
  }

  public void query(LockID lockID, ThreadID threadID) {
    serverLockManager.queryLock(lockID, clientID, threadID, sink);
  }

  public void interrupt(LockID lockID, ThreadID threadID) {
    serverLockManager.interrupt(lockID, clientID, threadID);
  }

  public void tryLock(LockID lockID, ThreadID threadID, ServerLockLevel level, long timeout) {
    serverLockManager.tryRequestLock(lockID, clientID, threadID, ServerLockLevel.toLegacyInt(level), "", new TimerSpec(timeout), sink);
  }
}
