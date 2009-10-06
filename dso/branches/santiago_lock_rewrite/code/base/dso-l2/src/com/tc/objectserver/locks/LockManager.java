/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks;

import com.tc.logging.DumpHandler;
import com.tc.net.ClientID;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ServerLockLevel;
import com.tc.object.locks.ThreadID;
import com.tc.objectserver.locks.ServerLock.NotifyAction;
import com.tc.text.PrettyPrintable;

import java.util.Collection;

public interface LockManager extends DumpHandler, PrettyPrintable, LockManagerMBean {
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
}
