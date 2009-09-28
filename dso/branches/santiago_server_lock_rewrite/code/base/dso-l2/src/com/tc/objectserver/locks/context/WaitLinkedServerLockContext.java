/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks.context;

import com.tc.net.ClientID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.objectserver.locks.LockHelper;

import java.util.TimerTask;

public class WaitLinkedServerLockContext extends LinkedServerLockContext implements WaitServerLockContext {
  private TimerTask        task;
  private final long       timeout;
  private final LockHelper helper;

  public WaitLinkedServerLockContext(ClientID clientID, ThreadID threadID, long timeout, LockHelper lockHelper) {
    this(clientID, threadID, timeout, null, lockHelper);
  }

  public WaitLinkedServerLockContext(ClientID clientID, ThreadID threadID, long timeout, TimerTask task,
                                     LockHelper lockHelper) {
    super(clientID, threadID);
    this.timeout = timeout;
    this.helper = lockHelper;
    this.task = task;
  }

  public long getTimeout() {
    return timeout;
  }

  public void setTimerTask(TimerTask task) {
    this.task = task;
  }

  public TimerTask getTimerTask() {
    return task;
  }

  public LockHelper getLockHelper() {
    return helper;
  }
}
