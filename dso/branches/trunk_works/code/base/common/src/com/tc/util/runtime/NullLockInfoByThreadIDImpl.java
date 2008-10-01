/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.object.lockmanager.api.ThreadID;
import com.tc.util.State;

public class NullLockInfoByThreadIDImpl implements LockInfoByThreadID {

  public String getHeldLocks(ThreadID threadID) {
    return null;
  }

  public String getPendingLocks(ThreadID threadID) {
    return null;
  }

  public String getWaitOnLocks(ThreadID threadID) {
    return null;
  }

  public void addLock(State lockType, ThreadID threadID, String lockID) {
    //
  }
}
