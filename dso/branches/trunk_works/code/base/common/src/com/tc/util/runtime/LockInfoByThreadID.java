/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.object.lockmanager.api.ThreadID;

public interface LockInfoByThreadID {
  public static final String HELD_LOCK    = "HELD LOCK";
  public static final String WAIT_ON_LOCK = "WAITING ON LOCK";
  public static final String WAIT_TO_LOCK = "WAITING TO LOCK";

  public void addLock(String lockType, ThreadID threadID, Object value);

  public Object getLock(String lockType, ThreadID threadID);

  public Object getHeldLocksList(ThreadID threadID);

  public Object getWaitOnLocksList(ThreadID threadID);

  public Object getPendingLocksList(ThreadID threadID);

}
