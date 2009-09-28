/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks;

import com.tc.async.api.Sink;
import com.tc.management.L2LockStatsManager;
import com.tc.object.locks.LockTimer;
import com.tc.object.locks.LockTimerImpl;
import com.tc.object.locks.ServerLockContextStateMachine;

public class LockHelper {
  private final LockTimer                     lockTimer;
  private final L2LockStatsManager            lockStatsManager;
  private final Sink                          lockSink;
  private final LockStore                     lockStore;
  private final ServerLockContextStateMachine contextStateMachine;

  public LockHelper(L2LockStatsManager lockStatsManager, Sink lockSink, LockStore lockStore) {
    this.lockTimer = new LockTimerImpl();
    this.lockStatsManager = lockStatsManager;
    this.lockSink = lockSink;
    this.lockStore = lockStore;
    this.contextStateMachine = new ServerLockContextStateMachine();
  }

  public LockTimer getLockTimer() {
    return lockTimer;
  }

  public L2LockStatsManager getLockStatsManager() {
    return lockStatsManager;
  }

  public Sink getLockSink() {
    return lockSink;
  }

  public LockStore getLockStore() {
    return lockStore;
  }

  public ServerLockContextStateMachine getContextStateMachine() {
    return contextStateMachine;
  }
}
