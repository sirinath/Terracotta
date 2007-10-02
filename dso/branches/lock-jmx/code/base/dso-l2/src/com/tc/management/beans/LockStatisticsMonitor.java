/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.beans;

import com.tc.management.L2LockStatsManager;
import com.tc.object.lockmanager.api.LockID;

import java.io.Serializable;
import java.util.Collection;

public class LockStatisticsMonitor implements LockStatisticsMonitorMBean, Serializable {
  private final L2LockStatsManager lockStatsManager;
  
  public LockStatisticsMonitor(L2LockStatsManager lockStatsManager) {
    this.lockStatsManager = lockStatsManager;
  }

  public Collection getTopHeld(int n) {
    return this.lockStatsManager.getTopLockHoldersStats(n);
  }

  public Collection getTopRequested(int n) {
    return this.lockStatsManager.getTopLockStats(n);
  }
  
  public Collection getTopWaitingLocks(int n) {
    return this.lockStatsManager.getTopWaitingLocks(n);
  }
  
  public Collection getTopContentedLocks(int n) {
    return this.lockStatsManager.getTopContentedLocks(n);
  }
  
  public Collection getTopPingPongLocks(int n) {
    return this.lockStatsManager.getTopPingPongLocks(n);
  }
  
  public Collection getStackTraces(String lockID) {
    return this.lockStatsManager.getStackTraces(new LockID(lockID));
  }
  
  public void enableClientStat(String lockID) {
    this.lockStatsManager.enableClientStat(new LockID(lockID));
  }
  
  public void disableClientStat(String lockID) {
    this.lockStatsManager.disableClientStat(new LockID(lockID));
  }

}
