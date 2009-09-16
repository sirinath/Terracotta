/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.net.ClientID;
import com.tc.object.lockmanager.api.ThreadID;

import java.util.TimerTask;

public class WaitServerLockContext extends ServerLockContext {
  private TimerTask task;
  private long      timeout;

  public WaitServerLockContext(ClientID clientID, ThreadID threadID, TimerTask task, long timeout) {
    super(clientID, threadID);
    this.task = task;
    this.timeout = timeout;
  }

  public long getTimeout() {
    return timeout;
  }

  public TimerTask getTimerTask() {
    return task;
  }
}
