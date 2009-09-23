/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.net.ClientID;
import com.tc.object.locks.ThreadID;
import com.tc.object.locks.Lock.NotifyAction;
import com.tc.objectserver.lockmanager.api.LockMBean;
import com.tc.objectserver.lockmanager.api.NotifiedWaiters;

import java.util.Collection;

public interface LockManager {
  void lock(LockID lid, ClientID cid, ThreadID tid, ServerLockLevel level);

  void tryLock(LockID lid, ClientID cid, ThreadID threadID, ServerLockLevel level, long timeout);

  void unlock(LockID lid, ClientID receiverID, ThreadID threadID);

  void queryLock(LockID lid, ClientID cid, ThreadID threadID);

  void interrupt(LockID lid, ClientID cid, ThreadID threadID);

  void recallCommit(LockID lid, ClientID cid, Collection<ClientServerExchangeLockContext> serverLockContexts);

  void notify(LockID lid, ClientID cid, ThreadID tid, NotifyAction action, NotifiedWaiters addNotifiedWaitersTo);

  void wait(LockID lid, ClientID cid, ThreadID tid, long timeout);

  void reestablishState(ClientID cid, Collection<ClientServerExchangeLockContext> serverLockContexts);

  void clearAllLocksFor(ClientID cid);

  void enableLockStatsForNodeIfNeeded(ClientID cid);

  void start();

  void stop() throws InterruptedException;
  
  public LockMBean[] getAllLocks();
}
