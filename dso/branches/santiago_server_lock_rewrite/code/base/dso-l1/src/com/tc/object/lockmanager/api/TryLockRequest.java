/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.lockmanager.api;

import com.tc.object.locks.LockID;
import com.tc.object.tx.TimerSpec;
import com.tc.object.locks.ThreadID;

public class TryLockRequest extends WaitLockRequest {
  public TryLockRequest(LockID lockID, ThreadID threadID, int lockLevel, String lockType, TimerSpec call ) {
    super(lockID, threadID, lockLevel, lockType, call);
    call.mark();
  }
}
