/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.object.lockmanager.api.LockFlushCallback;
import com.tc.object.lockmanager.api.ThreadID;

import java.util.Collection;

public class RemoteLockManagerImpl implements RemoteLockManager {

  public void flush(LockID lock) {
    throw new AssertionError();
  }

  public void interruptWait(LockID lock, ThreadID thread) {
    throw new AssertionError();
  }

  public boolean isTransactionsForLockFlushed(LockID lock, LockFlushCallback callback) {
    throw new AssertionError();
  }

  public void lock(LockID lock, ThreadID thread, LockLevel level) {
    throw new AssertionError();
  }

  public void query(LockID lock, ThreadID thread) {
    throw new AssertionError();
  }

  public void tryLock(LockID lock, ThreadID thread, LockLevel level, long timeout) {
    throw new AssertionError();
  }

  public void unlock(LockID lock, ThreadID thread, LockLevel level) {
    throw new AssertionError();
  }

  public void unlockWithWait(LockID lock, ThreadID thread, long waitTime) {
    throw new AssertionError();
  }

  public void recallCommit(LockID lock, Collection<ClientServerExchangeLockContext> lockState) {
    throw new AssertionError();
  }

}
