/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.management.lock.stats.LockSpec;

import java.util.Collection;

public interface LockStatisticsMonitorMBean {

  public Collection<LockSpec> getLockSpecs();
  
  public void setLockStatisticsConfig(int traceDepth, int gatherInterval);
  
  public void setLockStatisticsEnabled(boolean lockStatsEnabled);

  public boolean isLockStatisticsEnabled();
  
  public int getTraceDepth();
  
  public int getGatherInterval();
}
