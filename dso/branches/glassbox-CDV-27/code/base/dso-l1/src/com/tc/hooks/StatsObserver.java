/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.hooks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public final class StatsObserver implements StatsListener {

  private volatile StatsListener statsListener;

  public StatsObserver() {
    this.statsListener = new NullStatsListener();
  }

  public synchronized void registerListener(StatsListener listener) {
    if (statsListener instanceof StatsObserver.NullStatsListener) {
      statsListener = listener;
    } else if (statsListener instanceof StatsObserver.BroadcastStatsListener) {
      StatsObserver.BroadcastStatsListener broadcast = (StatsObserver.BroadcastStatsListener) statsListener;
      List listeners = broadcast.getListeners();
      listeners.add(listener);
      broadcast.setListeners(listeners);
    } else {
      StatsObserver.BroadcastStatsListener broadcast = new BroadcastStatsListener();
      List listeners = broadcast.getListeners();
      listeners.add(statsListener);
      listeners.add(listener);
      broadcast.setListeners(listeners);
      statsListener = broadcast;
    }
  }

  public synchronized void removeListener(StatsListener listener) throws NoSuchElementException {
    if (statsListener instanceof StatsObserver.BroadcastStatsListener) {
      StatsObserver.BroadcastStatsListener broadcast = (StatsObserver.BroadcastStatsListener) statsListener;
      List listeners = broadcast.getListeners();
      if (!listeners.remove(listener)) throw new NoSuchElementException();
      if (listeners.size() == 1) {
        statsListener = (StatsListener) listeners.iterator().next();
        return;
      }
      broadcast.setListeners(listeners);
    } else {
      if (statsListener != listener) throw new NoSuchElementException();
      statsListener = new NullStatsListener();
    }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // StatsListener implementation

  public void lockAquire(String lockID, long startTime, long endTime) {
    statsListener.lockAquire(lockID, startTime, endTime);
  }

  public void objectFault(int size) {
    statsListener.objectFault(size);
  }

  public void transactionCommit(String lockID, long startTime, long endTime) {
    statsListener.transactionCommit(lockID, startTime, endTime);
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Multi-Listener implementation

  private final class BroadcastStatsListener implements StatsListener {

    private volatile List listeners;

    private BroadcastStatsListener() {
      this.listeners = new ArrayList();
    }

    public void lockAquire(String lockID, long startTime, long endTime) {
      for (Iterator iter = listeners.iterator(); iter.hasNext();) {
        ((StatsListener) iter.next()).lockAquire(lockID, startTime, endTime);
      }
    }

    public void objectFault(int size) {
      for (Iterator iter = listeners.iterator(); iter.hasNext();) {
        ((StatsListener) iter.next()).objectFault(size);
      }
    }

    public void transactionCommit(String lockID, long startTime, long endTime) {
      for (Iterator iter = listeners.iterator(); iter.hasNext();) {
        ((StatsListener) iter.next()).transactionCommit(lockID, startTime, endTime);
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

  private final class NullStatsListener implements StatsListener {

    public void lockAquire(String lockID, long startTime, long endTime) {
      // do nothing
    }

    public void transactionCommit(String lockID, long startTime, long endTime) {
      // do nothing
    }

    public void objectFault(int size) {
      // do nothing
    }
  }
}
