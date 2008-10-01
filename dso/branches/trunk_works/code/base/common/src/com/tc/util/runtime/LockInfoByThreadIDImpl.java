/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.object.lockmanager.api.ThreadID;

import java.util.HashMap;
import java.util.Map;

public class LockInfoByThreadIDImpl implements LockInfoByThreadID {

  Map heldLocks    = new HashMap();
  Map waitOnLocks  = new HashMap();
  Map pendingLocks = new HashMap();

  public LockInfoByThreadIDImpl() {
    //
  }

  public Object getHeldLocksList(ThreadID threadID) {
    return (heldLocks.get(threadID));
  }

  public Object getWaitOnLocksList(ThreadID threadID) {
    return (waitOnLocks.get(threadID));
  }

  public Object getPendingLocksList(ThreadID threadID) {
    return (pendingLocks.get(threadID));
  }

  public void addLock(String type, ThreadID threadID, Object value) {
    if (type.equals(LockInfoByThreadID.HELD_LOCK)) {
      heldLocks.put(threadID, value);
    } else if (type.equals(LockInfoByThreadID.WAIT_ON_LOCK)) {
      waitOnLocks.put(threadID, value);
    } else if (type.equals(LockInfoByThreadID.WAIT_TO_LOCK)) {
      pendingLocks.put(threadID, value);
    } else {
      throw new AssertionError("Unexpected Lock type : " + type);
    }
  }

  public Object getLock(String lockType, ThreadID threadID) {
    if (lockType.equals(LockInfoByThreadID.HELD_LOCK)) {
      return getHeldLocksList(threadID);
    } else if (lockType.equals(LockInfoByThreadID.WAIT_ON_LOCK)) {
      return getWaitOnLocksList(threadID);
    } else if (lockType.equals(LockInfoByThreadID.WAIT_TO_LOCK)) {
      return getPendingLocksList(threadID);
    } else {
      throw new AssertionError("Unexpected Lock type : " + lockType);
    }
  }
}
