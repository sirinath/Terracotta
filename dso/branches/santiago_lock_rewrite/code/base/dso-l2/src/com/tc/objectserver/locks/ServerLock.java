/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks;

import com.tc.net.ClientID;
import com.tc.object.lockmanager.api.TimerCallback;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ServerLockLevel;
import com.tc.object.locks.ThreadID;
import com.tc.object.net.DSOChannelManager;

import java.util.Collection;

public interface ServerLock extends TimerCallback {
  enum NotifyAction {
    ONE, ALL
  }

  void lock(ClientID cid, ThreadID tid, ServerLockLevel level, LockHelper helper);

  void tryLock(ClientID cid, ThreadID tid, ServerLockLevel level, long timeout, LockHelper helper);

  void queryLock(ClientID cid, ThreadID tid, LockHelper helper);

  void interrupt(ClientID cid, ThreadID tid, LockHelper helper);

  void unlock(ClientID cid, ThreadID tid, LockHelper helper);

  void recallCommit(ClientID cid, Collection<ClientServerExchangeLockContext> serverLockContexts, LockHelper helper);

  NotifiedWaiters notify(ClientID cid, ThreadID tid, NotifyAction action, NotifiedWaiters addNotifiedWaitersTo,
                         LockHelper helper);

  void wait(ClientID cid, ThreadID tid, long timeout, LockHelper helper);

  void reestablishState(ClientServerExchangeLockContext serverLockContext, LockHelper lockHelper);

  boolean clearStateForNode(ClientID cid, LockHelper helper);

  LockMBean getMBean(DSOChannelManager channelManager);

  LockID getLockID();
}
