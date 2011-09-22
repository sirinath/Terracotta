/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.object.ObjectID;
import com.tc.object.cache.CachedItemStore;
import com.tc.util.ObjectIDSet;

import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReInvalidateHandler {
  private final static long          EXPIRE_SET_TIMER_PERIOD    = 60 * 1000;
  private final static long          RE_INVALIDATE_TIMER_PERIOD = 1 * 1000;

  private final CachedItemStore      store;
  private ConcurrentObjectIDSet prev                       = null;
  private ConcurrentObjectIDSet current                    = new ConcurrentObjectIDSet();
  private final Timer                timer                      = new Timer("Re-invalidation Timer", true);

  public ReInvalidateHandler(CachedItemStore store) {
    this.store = store;
    timer.schedule(new ExpireSetTimerTask(), EXPIRE_SET_TIMER_PERIOD, EXPIRE_SET_TIMER_PERIOD);
    timer.schedule(new ReInvalidateTimerTask(), RE_INVALIDATE_TIMER_PERIOD, RE_INVALIDATE_TIMER_PERIOD);
  }

  public void add(ObjectID oid) {
    current.add(oid);
  }

  private class ExpireSetTimerTask extends TimerTask {
    @Override
    public void run() {
      synchronized (this) {
        prev = current;
        current = new ConcurrentObjectIDSet();
      }
    }
  }

  private class ReInvalidateTimerTask extends TimerTask {
    @Override
    public void run() {
      final ConcurrentObjectIDSet tempPrev;
      final ConcurrentObjectIDSet tempCurrent;

      synchronized (this) {
        tempPrev = prev;
        tempCurrent = current;
      }

      if (tempPrev != null) {
        tempPrev.processInvalidations();
      }

      if (tempCurrent != null) {
        tempCurrent.processInvalidations();
      }
    }
  }

  private class ConcurrentObjectIDSet {
    private static final int               CONCURRENCY = 4;

    private final ReentrantReadWriteLock[] locks;
    private final ObjectIDSet[]            oidSets;

    public ConcurrentObjectIDSet() {
      this(CONCURRENCY);
    }

    public ConcurrentObjectIDSet(int concurrency) {
      locks = new ReentrantReadWriteLock[concurrency];
      oidSets = new ObjectIDSet[concurrency];
      for (int i = 0; i < concurrency; i++) {
        oidSets[i] = new ObjectIDSet();
        locks[i] = new ReentrantReadWriteLock();
      }
    }

    private ReentrantReadWriteLock getLock(ObjectID oid) {
      return locks[(int) (Math.abs(oid.toLong()) % CONCURRENCY)];
    }

    private ObjectIDSet getSet(ObjectID oid) {
      return oidSets[(int) (Math.abs(oid.toLong()) % CONCURRENCY)];
    }

    public void processInvalidations() {
      for (int i = 0; i < oidSets.length; i++) {
        ReentrantReadWriteLock lock = locks[i];
        ObjectIDSet oidSet = oidSets[i];

        lock.writeLock().lock();
        try {
          if (oidSet.size() == 0) {
            continue;
          }

          Iterator<ObjectID> iterator = oidSet.iterator();
          while (iterator.hasNext()) {
            ObjectID oid = iterator.next();
            if (store.flush(oid)) {
              iterator.remove();
            }
          }
        } finally {
          lock.writeLock().unlock();
        }
      }
    }

    public void add(ObjectID oid) {
      ReentrantReadWriteLock lock = getLock(oid);
      ObjectIDSet set = getSet(oid);
      lock.writeLock().lock();

      try {
        set.add(oid);
      } finally {
        lock.writeLock().unlock();
      }
    }
  }
}
