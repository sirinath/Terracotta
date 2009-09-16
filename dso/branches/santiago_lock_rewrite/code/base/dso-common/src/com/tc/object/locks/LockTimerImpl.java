/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.lockmanager.api.TCLockTimer;
import com.tc.object.lockmanager.api.TimerCallback;
import com.tc.util.Assert;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

public class LockTimerImpl implements LockTimer {
  private static final TCLogger logger    = TCLogging.getLogger(TCLockTimer.class);

  private final Timer           timer     = new Timer("DSO Lock Object.wait() timer", true);
  private boolean               started   = false;
  private boolean               shutdown  = false;
  private LinkedList<TaskImpl>  taskQueue = new LinkedList<TaskImpl>();

  public LockTimerImpl() {
    super();
  }

  public Timer getTimer() {
    return timer;
  }

  public synchronized void start() {
    started = true;
    scheduleQueuedTasks();
    taskQueue = null;
  }

  private void scheduleQueuedTasks() {
    for (Iterator<TaskImpl> tasks = taskQueue.iterator(); tasks.hasNext();) {
      TaskImpl task = tasks.next();
      timer.schedule(task, task.getScheduleDelay());
    }
  }

  public TimerTask scheduleTimer(TimerCallback callback, long timeInMillis, Object callbackObject) {
    if (timeInMillis < 0) { throw Assert.failure("Wait time passed was negative = " + timeInMillis); }

    final TaskImpl rv = new TaskImpl(callback, timeInMillis, callbackObject);

    synchronized (this) {
      if (!started) {
        taskQueue.addLast(rv);
        return rv;
      }
    }

    timer.schedule(rv, timeInMillis);
    return rv;
  }

  public synchronized void shutdown() {
    if (shutdown) return;
    shutdown = true;
    this.timer.cancel();
  }

  private static class TaskImpl extends TimerTask {

    private final TimerCallback callback;
    private final Object        callbackObject;
    private final long          scheduleDelayInMillis;

    TaskImpl(TimerCallback callback, long timeInMillis, Object callbackObject) {
      this.callback = callback;
      this.callbackObject = callbackObject;
      this.scheduleDelayInMillis = timeInMillis;
    }

    public long getScheduleDelay() {
      return scheduleDelayInMillis;
    }

    public void run() {
      try {
        callback.timerTimeout(callbackObject);
      } catch (Exception e) {
        logger.error("Error processing wait timeout for " + callbackObject, e);
      }
    }

    public boolean cancel() {
      return super.cancel();
    }
  }
}
