/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.object.lockmanager.api.TimerCallback;

import java.util.Timer;
import java.util.TimerTask;

public interface LockTimer {
  /**
   * This method has been added to notify that the lock manager has started.
   */
  public void start();

  /**
   * The callbackObject is being used both in the server and the client with different types.
   */
  public TimerTask scheduleTimer(TimerCallback callback, long call, Object callbackObject);

  public void shutdown();

  public Timer getTimer();
}
