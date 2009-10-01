/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;


import com.tc.object.lockmanager.api.WaitListener;

import java.util.Collection;

public interface ClientLock {
  public void lock(RemoteLockManager remote, ThreadID thread, LockLevel level);
  public boolean tryLock(RemoteLockManager remote, ThreadID thread, LockLevel level);
  public boolean tryLock(RemoteLockManager remote, ThreadID thread, LockLevel level, long timeout) throws InterruptedException;
  public void lockInterruptibly(RemoteLockManager remote, ThreadID thread, LockLevel level) throws InterruptedException;

  public void unlock(RemoteLockManager remote, ThreadID thread, LockLevel level);

  public boolean notify(RemoteLockManager remote, ThreadID thread);
  public boolean notifyAll(RemoteLockManager remote, ThreadID thread);
  public void wait(RemoteLockManager remote, WaitListener listener, ThreadID thread) throws InterruptedException;
  public void wait(RemoteLockManager remote, WaitListener listener, ThreadID thread, long timeout) throws InterruptedException;

  public boolean isLocked(LockLevel level);
  public boolean isLockedBy(ThreadID thread, LockLevel level);

  public int holdCount(LockLevel level);
  public int pendingCount();
  public int waitingCount();

  public void notified(ThreadID thread);
  public void recall(RemoteLockManager remote, ServerLockLevel interest, int lease);
  public void award(ThreadID thread, ServerLockLevel level);
  public void refuse(ThreadID thread, ServerLockLevel level);

  /*
   * This method supports both the client handshake - (allows you
   * to dump the entire state into the handshake message) - and
   * also the sampled profiling (can take a snapshot whenever to
   * record the current lock state).
   */
  public Collection<ClientServerExchangeLockContext> getStateSnapshot();
  
  @Deprecated
  public Collection<ClientServerExchangeLockContext> getLegacyStateSnapshot();

  /**
   * ClientLock implementations must return true (and subsequently throw GarbageLockException) if
   * they consider themselves garbage.
   */
  public boolean garbageCollect();
}
