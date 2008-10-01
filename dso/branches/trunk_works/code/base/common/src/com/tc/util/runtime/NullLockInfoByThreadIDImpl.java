/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.object.lockmanager.api.ThreadID;

public class NullLockInfoByThreadIDImpl implements LockInfoByThreadID {

  public Object getHeldLocksList(ThreadID threadID) {
    return null;
  }


  public Object getPendingLocksList(ThreadID threadID) {
    return null;
  }


  public Object getWaitOnLocksList(ThreadID threadID) {
    return null;
  }

  public void addLock(String lockType, ThreadID threadID, Object value) {
    //
  }

  public Object getLock(String lockType, ThreadID threadID) {
    return null;
  }

}
