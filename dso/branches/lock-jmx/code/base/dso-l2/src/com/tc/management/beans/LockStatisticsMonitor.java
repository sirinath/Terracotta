/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.beans;

import com.tc.management.L2LockStatsManager;
import com.tc.management.lock.stats.LockSpec;

import java.io.Serializable;
import java.util.Collection;

public class LockStatisticsMonitor implements LockStatisticsMonitorMBean, Serializable {
  private final L2LockStatsManager lockStatsManager;
  
  public LockStatisticsMonitor(L2LockStatsManager lockStatsManager) {
    this.lockStatsManager = lockStatsManager;
  }

  public Collection<LockSpec> getLockSpecs() {
    return this.lockStatsManager.getLockSpecs();
  }
  
  public void setLockStatisticsConfig(int traceDepth, int gatherInterval) {
    this.lockStatsManager.setLockStatisticsConfig(traceDepth, gatherInterval);
  }
  
  public void setLockStatisticsEnabled(boolean lockStatsEnabled) {
    this.lockStatsManager.setLockStatisticsEnabled(lockStatsEnabled);
  }

  public boolean isLockStatisticsEnabled() {
    return this.lockStatsManager.isLockStatisticsEnabled();
  }
  
  public int getTraceDepth() {
    return this.lockStatsManager.getTraceDepth();
  }
  
  public int getGatherInterval() {
    return this.lockStatsManager.getGatherInterval();
  }
}
