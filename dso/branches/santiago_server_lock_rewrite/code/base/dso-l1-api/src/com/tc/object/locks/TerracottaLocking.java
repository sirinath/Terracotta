/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.object.locks.LockID;

public interface TerracottaLocking {
  public void lock(LockID lock, LockLevel level);
  public boolean tryLock(LockID lock, LockLevel level);
  public boolean tryLock(LockID lock, LockLevel level, long timeout);
  public void lockInterruptibly(LockID lock, LockLevel level);

  public void unlock(LockID lock, LockLevel level);

  public void notify(LockID lock);
  public void notifyAll(LockID lock);
  public void wait(LockID lock);
  public void wait(LockID lock, long timeout);

  public boolean isLocked(LockID lock, LockLevel level);
  public boolean isLockedByCurrentThread(LockID lock, LockLevel level);

  public int localHoldCount(LockID lock, LockLevel level);
  public int globalHoldCount(LockID lock, LockLevel level);
  public int globalPendingCount(LockID lock);
  public int globalWaitingCount(LockID lock);
  
  public LockID generateLockIdentifier(String str);
  public LockID generateLockIdentifier(Object obj);
  public LockID generateLockIdentifier(Object obj, String field);
  public LockID generateLockIdentifier(Object obj, long fieldOffset);  
}
