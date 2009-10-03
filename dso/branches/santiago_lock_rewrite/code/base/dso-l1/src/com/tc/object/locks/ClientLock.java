/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;


import com.tc.object.lockmanager.api.WaitListener;
import com.tc.object.msg.ClientHandshakeMessage;

import java.util.Collection;

public interface ClientLock {
  public void lock(RemoteLockManager remote, ThreadID thread, LockLevel level) throws GarbageLockException;
  public boolean tryLock(RemoteLockManager remote, ThreadID thread, LockLevel level) throws GarbageLockException;
  public boolean tryLock(RemoteLockManager remote, ThreadID thread, LockLevel level, long timeout) throws InterruptedException, GarbageLockException;
  public void lockInterruptibly(RemoteLockManager remote, ThreadID thread, LockLevel level) throws InterruptedException, GarbageLockException;

  public void unlock(RemoteLockManager remote, ThreadID thread, LockLevel level);

  public boolean notify(RemoteLockManager remote, ThreadID thread);
  public boolean notifyAll(RemoteLockManager remote, ThreadID thread);
  public void wait(RemoteLockManager remote, WaitListener listener, ThreadID thread) throws InterruptedException, GarbageLockException;
  public void wait(RemoteLockManager remote, WaitListener listener, ThreadID thread, long timeout) throws InterruptedException, GarbageLockException;

  public boolean isLocked(LockLevel level);
  public boolean isLockedBy(ThreadID thread, LockLevel level);

  public int holdCount(LockLevel level);
  public int pendingCount();
  public int waitingCount();

  public void notified(ThreadID thread);
  public void recall(RemoteLockManager remote, ServerLockLevel interest, int lease);
  public void award(RemoteLockManager remote, ThreadID thread, ServerLockLevel level);
  public void refuse(ThreadID thread, ServerLockLevel level);

  public Collection<ClientServerExchangeLockContext> getStateSnapshot();
  
  public void initializeHandshake(ClientHandshakeMessage handshake);

  /**
   * ClientLock implementations must return true (and subsequently throw GarbageLockException) if
   * they consider themselves garbage.
   * @param remote remote manager to interact with
   */
  public boolean tryToMarkAsGarbage(RemoteLockManager remote);
}
