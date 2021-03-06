/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.impl.ObjectManagerConfig;
import com.tc.util.concurrent.StoppableThread;

public class GarbageCollectorThread extends StoppableThread {
  private static final TCLogger  logger   = TCLogging.getLogger(GarbageCollectorThread.class);

  private final GarbageCollector collector;
  private final Object           stopLock = new Object();
  private final boolean          doFullGC;
  private final long             fullGCSleepTime;
  private final long             youngGCSleepTime;

  public GarbageCollectorThread(ThreadGroup group, String name, GarbageCollector newCollector,
                                ObjectManagerConfig config) {
    super(group, name);
    this.collector = newCollector;
    doFullGC = config.doGC();
    fullGCSleepTime = config.gcThreadSleepTime();
    long youngGCTime = -1;
    if (config.isYoungGenDGCEnabled()) {
      youngGCTime = config.getYoungGenDGCFrequencyInMillis();
      if (doFullGC && youngGCTime >= fullGCSleepTime) {
        logger.warn("Disabling YoungGen Garbage Collector since the time interval for YoungGen GC ( " + youngGCTime
                    + " ms) is greater than or equal to  the time interval for Full GC ( " + fullGCSleepTime + " ms)");
        youngGCTime = -1;
      } else if (youngGCTime <= 0) {
        logger.warn("Disabling YoungGen GC since time interval is specificed as " + youngGCTime + " ms");
        youngGCTime = -1;
      }
    }
    if ((youngGCSleepTime = youngGCTime) == -1 && !doFullGC) {
      logger.warn("Stopping Garbage Collector thread as both Full and YoungGen collectors are disabled.");
      requestStop();
    } else {
      logger.info("Young Gen Time = " + youngGCTime + " Full Gen Time = " + fullGCSleepTime);
    }
  }

  public void requestStop() {
    super.requestStop();

    synchronized (stopLock) {
      stopLock.notifyAll();
    }
  }

  public void run() {
    long lastFullGC = System.currentTimeMillis();
    while (true) {
      if (isStopRequested()) { return; }
      if (doFullGC) {
        if (youngGCSleepTime <= 0) {
          // young generation GC is disabled
          doFullGC(fullGCSleepTime);
          lastFullGC = System.currentTimeMillis();
        } else {
          // run young or full GC
          long current = System.currentTimeMillis();
          if (lastFullGC + fullGCSleepTime > current + youngGCSleepTime) {
            // Run young GC next
            doYoungGC(youngGCSleepTime);
          } else {
            // Run full GC next, sleeping at least 1 second
            doFullGC(Math.max(lastFullGC + fullGCSleepTime - current, 1000));
            lastFullGC = System.currentTimeMillis();
          }
        }
      } else {
        doYoungGC(youngGCSleepTime);
      }
    }
  }

  private void doYoungGC(long sleepTime) {
    try {
      synchronized (stopLock) {
        stopLock.wait(sleepTime);
      }
      if (isStopRequested()) { return; }
      collector.gcYoung();
    } catch (InterruptedException ie) {
      throw new TCRuntimeException(ie);
    }
  }

  private void doFullGC(long sleepTime) {
    try {
      synchronized (stopLock) {
        stopLock.wait(sleepTime);
      }
      if (isStopRequested()) { return; }
      collector.gc();
    } catch (InterruptedException ie) {
      throw new TCRuntimeException(ie);
    }
  }
}