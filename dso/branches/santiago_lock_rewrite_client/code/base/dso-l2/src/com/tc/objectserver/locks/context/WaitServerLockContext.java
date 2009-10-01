/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks.context;

import com.tc.objectserver.locks.LockHelper;

import java.util.TimerTask;

/**
 * Instances of this interface will store the try pending and wait contexts
 */
public interface WaitServerLockContext {
  public long getTimeout();

  public void setTimerTask(TimerTask task);

  public TimerTask getTimerTask();

  public LockHelper getLockHelper();
}
