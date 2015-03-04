/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.object.locks.ThreadID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LockInfoByThreadIDImpl implements LockInfoByThreadID {

  Map heldLocks    = new HashMap();
  Map waitOnLocks  = new HashMap();
  Map pendingLocks = new HashMap();

  public ArrayList getHeldLocks(ThreadID threadID) {
    return lockList((ArrayList) heldLocks.get(threadID));
  }

  public ArrayList getWaitOnLocks(ThreadID threadID) {
    return lockList((ArrayList) waitOnLocks.get(threadID));
  }

  public ArrayList getPendingLocks(ThreadID threadID) {
    return lockList((ArrayList) pendingLocks.get(threadID));
  }

  private ArrayList lockList(ArrayList lockList) {
    if (lockList == null) {
      return new ArrayList();
    } else {
      return lockList;
    }
  }

  public void addLock(LockState lockState, ThreadID threadID, String value) {
    if (lockState == LockState.HOLDING) {
      addLockTo(heldLocks, threadID, value);
    } else if (lockState == LockState.WAITING_ON) {
      addLockTo(waitOnLocks, threadID, value);
    } else if (lockState == LockState.WAITING_TO) {
      addLockTo(pendingLocks, threadID, value);
    } else {
      throw new AssertionError("Unexpected Lock type : " + lockState);
    }
  }

  private void addLockTo(Map lockMap, ThreadID threadID, String value) {
    ArrayList lockArray = (ArrayList) lockMap.get(threadID);
    if (lockArray == null) {
      ArrayList al = new ArrayList();
      al.add(value);
      lockMap.put(threadID, al);
    } else {
      lockArray.add(value);
      lockMap.put(threadID, lockArray);
    }
  }
}
