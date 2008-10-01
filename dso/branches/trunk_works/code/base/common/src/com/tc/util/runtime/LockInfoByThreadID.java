/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.object.lockmanager.api.ThreadID;
import com.tc.util.State;

public interface LockInfoByThreadID {
  public static final State HELD_LOCK    = new State("HELD LOCK");
  public static final State WAIT_ON_LOCK = new State("WAITING ON LOCK");
  public static final State WAIT_TO_LOCK = new State("WAITING TO LOCK");

  public void addLock(State lockType, ThreadID threadID, String lockID);

  public String getHeldLocks(ThreadID threadID);

  public String getWaitOnLocks(ThreadID threadID);

  public String getPendingLocks(ThreadID threadID);

}
