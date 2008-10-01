/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.object.lockmanager.api.ThreadID;
import com.tc.util.State;

import java.util.HashMap;
import java.util.Map;

public class LockInfoByThreadIDImpl implements LockInfoByThreadID {

  Map heldLocks    = new HashMap();
  Map waitOnLocks  = new HashMap();
  Map pendingLocks = new HashMap();

  public String getHeldLocks(ThreadID threadID) {
    return ((String) heldLocks.get(threadID));
  }

  public String getWaitOnLocks(ThreadID threadID) {
    return ((String) waitOnLocks.get(threadID));
  }

  public String getPendingLocks(ThreadID threadID) {
    return ((String) pendingLocks.get(threadID));
  }

  public void addLock(State lockType, ThreadID threadID, String value) {
    if (lockType == HELD_LOCK) {
      addLockTo(heldLocks, threadID, value);
    } else if (lockType == WAIT_ON_LOCK) {
      addLockTo(waitOnLocks, threadID, value);
    } else if (lockType == WAIT_TO_LOCK) {
      addLockTo(pendingLocks, threadID, value);
    } else {
      throw new AssertionError("Unexpected Lock type : " + lockType);
    }
  }

  private void addLockTo(Map lockMap, ThreadID threadID, String value) {
    Object oldValue = lockMap.get(threadID);
    if (oldValue == null) {
      lockMap.put(threadID, value);
    } else {
      lockMap.put(threadID, oldValue + "; " + value);
    }
  }
}
