/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans;

import java.util.Collection;

public interface LockStatisticsMonitorMBean {

  public Collection getTopHeld(int n);

  public Collection getTopRequested(int n);
  
  public Collection getTopWaitingLocks(int n);

  public Collection getTopContentedLocks(int n);
  
  public Collection getTopPingPongLocks(int n);
  
  public Collection getStackTraces(String lockID);
  
  public void enableClientStat(String lockID);
  
  public void disableClientStat(String lockID);
}
