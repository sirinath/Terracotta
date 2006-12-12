/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.hooks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public final class StatsObserver {

  private volatile StatsListener statsListener;
  private static StatsObserver   statsObserver = new StatsObserver();

  private StatsObserver() {
    this.statsListener = new NullStatsListener();
  }

  public static synchronized void registerListener(StatsListener listener) {
    if (statsObserver.statsListener instanceof StatsObserver.NullStatsListener) {
      statsObserver.statsListener = listener;
    } else if (statsObserver.statsListener instanceof StatsObserver.BroadcastStatsListener) {
      StatsObserver.BroadcastStatsListener broadcast = (StatsObserver.BroadcastStatsListener) statsObserver.statsListener;
      List listeners = broadcast.getListeners();
      listeners.add(listener);
      broadcast.setListeners(listeners);
    } else {
      StatsObserver.BroadcastStatsListener broadcast = statsObserver.new BroadcastStatsListener();
      List listeners = broadcast.getListeners();
      listeners.add(statsObserver.statsListener);
      listeners.add(listener);
      broadcast.setListeners(listeners);
      statsObserver.statsListener = broadcast;
    }
  }

  public static synchronized void removeListener(StatsListener listener) throws NoSuchElementException {
    if (statsObserver.statsListener instanceof StatsObserver.BroadcastStatsListener) {
      StatsObserver.BroadcastStatsListener broadcast = (StatsObserver.BroadcastStatsListener) statsObserver.statsListener;
      List listeners = broadcast.getListeners();
      if (!listeners.remove(listener)) throw new NoSuchElementException();
      if (listeners.size() == 1) {
        statsObserver.statsListener = (StatsListener) listeners.iterator().next();
        return;
      }
      broadcast.setListeners(listeners);
    } else {
      if (statsObserver.statsListener != listener) throw new NoSuchElementException();
      statsObserver.statsListener = statsObserver.new NullStatsListener();
    }
  }
  
  public static long currentTime() {
    return statsObserver.statsListener.currentTime();
  }

  // -------------------------------------------------------------------------------------------------------------------
  // StatsListener implementation

  public static void lockAquire(String lockID, long startTime) {
    statsObserver.statsListener.lockAquire(lockID, startTime);
  }

  public static void objectFault() {
    statsObserver.statsListener.objectFault();
  }

  public static void transactionCommit() {
    statsObserver.statsListener.transactionCommit();
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Multi-Listener implementation

  private final class BroadcastStatsListener extends StatsListener {

    private volatile List listeners;

    private BroadcastStatsListener() {
      this.listeners = new ArrayList();
    }

    public void lockAquire(String lockID, long startTime) {
      for (Iterator iter = listeners.iterator(); iter.hasNext();) {
        ((StatsListener) iter.next()).lockAquire(lockID, startTime);
      }
    }

    public void objectFault() {
      for (Iterator iter = listeners.iterator(); iter.hasNext();) {
        ((StatsListener) iter.next()).objectFault();
      }
    }

    public void transactionCommit() {
      for (Iterator iter = listeners.iterator(); iter.hasNext();) {
        ((StatsListener) iter.next()).transactionCommit();
      }
    }

    private List getListeners() {
      return (List) ((ArrayList) listeners).clone();
    }

    private void setListeners(List listeners) {
      this.listeners = listeners;
    }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Null-Listener implementation

  private final class NullStatsListener extends StatsListener {

    public void lockAquire(String lockID, long startTime) {
      // do nothing
    }

    public void transactionCommit() {
      // do nothing
    }

    public void objectFault() {
      // do nothing
    }
    
    protected long currentTime() {
      return 0L;
    }
  }
}
