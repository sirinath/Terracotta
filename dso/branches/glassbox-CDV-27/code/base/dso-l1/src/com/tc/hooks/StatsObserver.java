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

  public void beginLockAquire(String lockID) {
    statsListener.beginLockAquire(lockID);
  }
  
  public void endLockAquire(String lockID) {
    statsListener.endLockAquire(lockID);
  }
  
  public void beginObjectFault(int size) {
    statsListener.beginObjectFault(size);
  }

  public void endObjectFault(int size) {
    statsListener.endObjectFault(size);
  }

  public void beginTransactionCommit(String lockID) {
    statsListener.beginTransactionCommit(lockID);
  }
  
  public void endTransactionCommit(String lockID) {
    statsListener.endTransactionCommit(lockID);
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Multi-Listener implementation

  private final class BroadcastStatsListener implements StatsListener {

    private volatile List listeners;

    private BroadcastStatsListener() {
      this.listeners = new ArrayList();
    }

    public void beginLockAquire(String lockID) {
      for (Iterator iter = listeners.iterator(); iter.hasNext();) {
        ((StatsListener) iter.next()).beginLockAquire(lockID);
      }
    }
    
    public void endLockAquire(String lockID) {
      for (Iterator iter = listeners.iterator(); iter.hasNext();) {
        ((StatsListener) iter.next()).endLockAquire(lockID);
      }
    }

    public void beginObjectFault(int size) {
      for (Iterator iter = listeners.iterator(); iter.hasNext();) {
        ((StatsListener) iter.next()).beginObjectFault(size);
      }
    }
    
    public void endObjectFault(int size) {
      for (Iterator iter = listeners.iterator(); iter.hasNext();) {
        ((StatsListener) iter.next()).endObjectFault(size);
      }
    }

    public void beginTransactionCommit(String lockID) {
      for (Iterator iter = listeners.iterator(); iter.hasNext();) {
        ((StatsListener) iter.next()).beginTransactionCommit(lockID);
      }
    }
    
    public void endTransactionCommit(String lockID) {
      for (Iterator iter = listeners.iterator(); iter.hasNext();) {
        ((StatsListener) iter.next()).endTransactionCommit(lockID);
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

    public void beginLockAquire(String lockID) {
      // do nothing
    }
    
    public void endLockAquire(String lockID) {
      // do nothing
    }

    public void beginTransactionCommit(String lockID) {
      // do nothing
    }
    
    public void endTransactionCommit(String lockID) {
      // do nothing
    }

    public void beginObjectFault(int size) {
      // do nothing
    }
    
    public void endObjectFault(int size) {
      // do nothing
    }
  }
}
