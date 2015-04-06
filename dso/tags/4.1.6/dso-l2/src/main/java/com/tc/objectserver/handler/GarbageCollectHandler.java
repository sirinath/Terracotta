/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.api.GarbageCollectionManager;
import com.tc.objectserver.context.GarbageCollectContext;
import com.tc.objectserver.context.InlineGCContext;
import com.tc.objectserver.context.PeriodicGarbageCollectContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;
import com.tc.objectserver.impl.ObjectManagerConfig;
import com.tc.util.concurrent.LifeCycleState;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class GarbageCollectHandler extends AbstractEventHandler {
  private static final TCLogger                logger         = TCLogging.getLogger(GarbageCollectHandler.class);

  private final Timer                          timer          = new Timer("GarbageCollectHandler Timer");
  private final boolean                        fullGCEnabled;
  private final long                           fullGCInterval;
  private final LifeCycleState                 gcState        = new GCState();
  private volatile boolean                     gcRunning      = false;
  private GarbageCollector                     collector;
  private GarbageCollectionManager             garbageCollectionManager;
  private Sink                                 gcSink;

  public GarbageCollectHandler(final ObjectManagerConfig objectManagerConfig) {
    this.fullGCEnabled = objectManagerConfig.doGC();
    this.fullGCInterval = objectManagerConfig.gcThreadSleepTime();
  }

  @Override
  public void handleEvent(EventContext context) {
    timer.purge(); // Get rid of finished tasks
    if (context instanceof GarbageCollectContext) {
      GarbageCollectContext gcc = (GarbageCollectContext) context;
      if (gcc.getDelay() > 0) {
        final long delay = gcc.getDelay();
        gcc.setDelay(0);
        scheduleDGC(gcc, delay);
      } else {
        gcRunning = true;
        collector.doGC(gcc.getType());
        gcRunning = false;
        // We want to let inline gc clean stuff up (quickly) before another young/full gc happens
        garbageCollectionManager.scheduleInlineGarbageCollectionIfNecessary();
        if (gcc instanceof PeriodicGarbageCollectContext) {
          // Rearm and requeue if it's a periodic gc.
          PeriodicGarbageCollectContext pgcc = (PeriodicGarbageCollectContext) gcc;
          pgcc.reset();
          gcSink.add(pgcc);
        }
      }
    } else if (context instanceof InlineGCContext) {
      garbageCollectionManager.inlineCleanup();
    } else {
      throw new AssertionError("Unknown context type: " + context.getClass().getName());
    }
  }

  public void scheduleDGC(final GarbageCollectContext gcc, final long delay) {
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        gcSink.add(gcc);
      }
    }, delay);
  }

  @Override
  protected void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    collector = scc.getObjectManager().getGarbageCollector();
    collector.setState(gcState);
    garbageCollectionManager = scc.getGarbageCollectionManager();
    gcSink = scc.getStage(ServerConfigurationContext.GARBAGE_COLLECT_STAGE).getSink();
  }

  private class GCState implements LifeCycleState {
    private volatile boolean stopRequested = false;

    @Override
    public void start() {
      if (fullGCEnabled) {
        gcSink.add(new PeriodicGarbageCollectContext(GCType.FULL_GC, fullGCInterval));
        collector.setPeriodicEnabled(true);
      }
    }

    @Override
    public boolean isStopRequested() {
      return stopRequested;
    }

    @Override
    public boolean stopAndWait(long waitTime) {
      logger.info("Garbage collection is stopping, clearing out remaining contexts.");
      stopRequested = true;
      // Purge the sink of any scheduled gc's, this needs to be equivalent to stopping the garbage collector thread.
      gcSink.clear();
      long start = System.nanoTime();
      while (gcRunning) {
        ThreadUtil.reallySleep(1000);
        if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) > waitTime) { return false; }
      }
      return true;
    }
  }
}
