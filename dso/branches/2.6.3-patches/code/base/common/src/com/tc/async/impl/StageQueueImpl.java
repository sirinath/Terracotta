/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.async.impl;

import com.tc.async.api.AddPredicate;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.async.api.Source;
import com.tc.async.api.StageQueueStats;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLoggerProvider;
import com.tc.stats.Stats;
import com.tc.util.Assert;
import com.tc.util.State;
import com.tc.util.concurrent.TCQueue;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * The beginnings of an implementation of our SEDA like framework. This Is part of an impl for the queue
 * 
 * @author steve
 */
public class StageQueueImpl implements Sink, Source {

  private static final State                RUNNING   = new State("RUNNING");
  private static final State                PAUSED    = new State("PAUSED");

  private final TCQueue                     queue;
  private final String                      stage;
  private final TCLogger                    logger;
  private AddPredicate                      predicate = DefaultAddPredicate.getInstance();
  private volatile State                    state     = RUNNING;
  private volatile StageQueueStatsCollector statsCollector;

  public StageQueueImpl(TCLoggerProvider loggerProvider, String stage, TCQueue queue) {
    this.queue = queue;
    this.logger = loggerProvider.getLogger(Sink.class.getName() + ": " + stage);
    this.stage = stage;
    this.statsCollector = new NullStageQueueStatsCollector(stage);
  }

  /**
   * The context will be added if the sink was found to be empty(at somepoint during the call). If the queue was not
   * empty (at somepoint during the call) the context might not be added. This method should only be used where the
   * stage threads are to be signaled on data availablity and the threads take care of getting data from elsewhere
   */
  public boolean addLossy(EventContext context) {
    if (isEmpty()) {
      add(context);
      return true;
    } else {
      return false;
    }
  }

  // XXX::Ugly hack since this method doesnt exist on the Channel interface
  private boolean isEmpty() {
    return queue.isEmpty();
  }

  public void addMany(Collection contexts) {
    if (logger.isDebugEnabled()) logger.debug("Added many:" + contexts + " to:" + stage);
    for (Iterator i = contexts.iterator(); i.hasNext();) {
      add((EventContext) i.next());
    }
  }

  public void add(EventContext context) {
    Assert.assertNotNull(context);
    if (state == PAUSED) {
      logger.info("Ignoring event while PAUSED: " + context);
      return;
    }

    if (logger.isDebugEnabled()) logger.debug("Added:" + context + " to:" + stage);
    if (!predicate.accept(context)) {
      if (logger.isDebugEnabled()) logger.debug("Predicate caused skip add for:" + context + " to:" + stage);
      return;
    }

    statsCollector.contextAdded();
    try {
      queue.put(context);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  public EventContext get() throws InterruptedException {
    return poll(Long.MAX_VALUE);
  }

  public EventContext poll(long period) throws InterruptedException {
    EventContext rv = (EventContext) queue.poll(period);
    if (rv != null) statsCollector.contextRemoved();
    return rv;
  }

  // Used for testing
  public int size() {
    return queue.size();
  }

  public Collection getAll() throws InterruptedException {
    List l = new LinkedList();
    l.add(queue.take());
    while (true) {
      Object o = queue.poll(0);
      if (o == null) {
        // could be a little off
        statsCollector.reset();
        break;
      } else {
        l.add(o);
      }
    }
    return l;
  }

  public void setAddPredicate(AddPredicate predicate) {
    Assert.eval(predicate != null);
    this.predicate = predicate;
  }

  public AddPredicate getPredicate() {
    return predicate;
  }

  public String toString() {
    return "StageQueue(" + stage + ")";
  }

  public void clear() {
    try {
      // XXX: poor man's clear.
      int clearCount = 0;
      while (poll(0) != null) { // calling this.poll() to get counter updated
        /* supress no-body warning */
        clearCount++;
      }
      statsCollector.reset();
      logger.info("Cleared " + clearCount);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  public void pause(List pauseEvents) {
    if (state != RUNNING) throw new AssertionError("Attempt to pause while not running: " + state);
    state = PAUSED;
    clear();
    for (Iterator i = pauseEvents.iterator(); i.hasNext();) {
      try {
        queue.put(i.next());
        statsCollector.contextAdded();
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
    }
  }

  public void unpause() {
    if (state != PAUSED) throw new AssertionError("Attempt to unpause while not paused: " + state);
    state = RUNNING;
  }

  /*********************************************************************************************************************
   * Monitorable Interface
   */

  public void enableStatsCollection(boolean enable) {
    if (enable) {
      statsCollector = new StageQueueStatsCollectorImpl(stage);
    } else {
      statsCollector = new NullStageQueueStatsCollector(stage);
    }
  }

  public Stats getStats(long frequency) {
    return statsCollector;
  }

  public Stats getStatsAndReset(long frequency) {
    return getStats(frequency);
  }

  public boolean isStatsCollectionEnabled() {
    return statsCollector instanceof StageQueueStatsCollectorImpl;
  }

  public void resetStats() {
    // NOP
  }

  public static abstract class StageQueueStatsCollector implements StageQueueStats {

    public void logDetails(TCLogger statsLogger) {
      statsLogger.info(getDetails());
    }

    public abstract void contextAdded();

    public abstract void reset();

    public abstract void contextRemoved();

    protected String makeWidth(String name, int width) {
      final int len = name.length();
      if (len == width) { return name; }
      if (len > width) { return name.substring(0, width); }

      StringBuffer buf = new StringBuffer(name);
      for (int i = len; i < width; i++) {
        buf.append(' ');
      }
      return buf.toString();
    }
  }

  public static class NullStageQueueStatsCollector extends StageQueueStatsCollector {

    private final String name;
    private final String trimmedName;

    public NullStageQueueStatsCollector(String stage) {
      this.trimmedName = stage.trim();
      this.name = makeWidth(stage, 40);
    }

    public String getDetails() {
      return name + " : Not Monitored";
    }

    public void contextAdded() {
      // NOOP
    }

    public void contextRemoved() {
      // NOOP
    }

    public void reset() {
      // NOOP
    }

    public String getName() {
      return trimmedName;
    }

    public int getDepth() {
      return -1;
    }
  }

  public static class StageQueueStatsCollectorImpl extends StageQueueStatsCollector {

    private int    count = 0;
    private final String name;
    private final String trimmedName;

    public StageQueueStatsCollectorImpl(String stage) {
      this.trimmedName = stage.trim();
      this.name = makeWidth(stage, 40);
    }

    public synchronized String getDetails() {
      return name + " : " + count;
    }

    public synchronized void contextAdded() {
      count++;
    }

    public synchronized void contextRemoved() {
      count--;
    }

    public synchronized void reset() {
      count = 0;
    }

    public String getName() {
      return trimmedName;
    }

    public int getDepth() {
      return count;
    }
  }

}
