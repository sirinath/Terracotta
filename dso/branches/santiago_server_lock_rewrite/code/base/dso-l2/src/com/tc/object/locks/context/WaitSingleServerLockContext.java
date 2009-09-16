/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks.context;

import com.tc.net.ClientID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.locks.LockHelper;

import java.util.TimerTask;

public class WaitSingleServerLockContext extends SingleServerLockContext implements WaitServerLockContext {
  private final TimerTask  task;
  private final long       timeout;
  private final LockHelper helper;

  public WaitSingleServerLockContext(ClientID clientID, ThreadID threadID, TimerTask task, long timeout,
                                     LockHelper lockHelper) {
    super(clientID, threadID);
    this.task = task;
    this.timeout = timeout;
    this.helper = lockHelper;
  }

  public long getTimeout() {
    return timeout;
  }

  public TimerTask getTimerTask() {
    return task;
  }

  public LockHelper getLockHelper() {
    return helper;
  }
}
