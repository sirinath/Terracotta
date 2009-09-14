/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.object.lockmanager.api.LockFlushCallback;
import com.tc.object.lockmanager.api.ThreadID;

import java.util.Collection;

public interface RemoteLockManager {
  public void lock(LockID lock, ThreadID thread, LockLevel level);  
  public void tryLock(LockID lock, ThreadID thread, LockLevel level, long timeout);

  public void unlock(LockID lock, ThreadID thread, LockLevel level);
 
  public void unlockWithWait(LockID lock, ThreadID thread, long waitTime);
  public void interruptWait(LockID lock, ThreadID thread);
 
  public void recallCommit(LockID lock, Collection<ClientServerExchangeLockContext> lockState);

  public void flush(LockID lock);  
  public boolean isTransactionsForLockFlushed(LockID lock, LockFlushCallback callback);

  public void query(LockID lock, ThreadID thread);
}
