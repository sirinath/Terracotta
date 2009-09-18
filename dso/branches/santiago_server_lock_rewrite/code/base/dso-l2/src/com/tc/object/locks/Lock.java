/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.net.ClientID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.api.TimerCallback;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.lockmanager.api.LockMBean;
import com.tc.objectserver.lockmanager.api.TCIllegalMonitorStateException;

import java.util.Collection;

public interface Lock extends TimerCallback {
  enum NotifyAction {
    ONE, ALL
  }

  void lock(ClientID cid, ThreadID tid, ServerLockLevel level, LockHelper helper);

  void tryLock(ClientID cid, ThreadID tid, ServerLockLevel level, long timeout, LockHelper helper);

  void queryLock(ClientID cid, ThreadID tid, LockHelper helper);

  void interrupt(ClientID cid, ThreadID tid, LockHelper helper);

  void unlock(ClientID cid, ThreadID tid, LockHelper helper);

  void recallCommit(ClientID cid, Collection<ClientServerExchangeLockContext> serverLockContexts, LockHelper helper);

  void notify(ClientID cid, ThreadID tid, NotifyAction action, NotifiedWaiters addNotifiedWaitersTo, LockHelper helper)
      throws TCIllegalMonitorStateException;

  void wait(ClientID cid, ThreadID tid, long timeout, LockHelper helper) throws TCIllegalMonitorStateException;

  void reestablishState(ClientServerExchangeLockContext serverLockContext, LockHelper lockHelper);

  void clearStateForNode(ClientID cid, LockHelper helper);

  LockMBean getMBean(DSOChannelManager channelManager);

  LockID getLockID();
}
