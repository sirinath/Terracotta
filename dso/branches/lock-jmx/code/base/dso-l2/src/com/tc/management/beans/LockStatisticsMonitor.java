/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.management.AbstractTerracottaMBean;
import com.tc.management.L2LockStatsManager;
import com.tc.management.lock.stats.LockSpec;

import java.io.Serializable;
import java.util.Collection;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;

public class LockStatisticsMonitor extends AbstractTerracottaMBean implements LockStatisticsMonitorMBean, Serializable {
  private final L2LockStatsManager lockStatsManager;

  private static final String[]    ALL_EVENTS           = new String[] { TRACE_DEPTH, TRACES_ENABLED };
  private static final String      DESCRIPTION          = "Terracotta Lock Statistics Event Notification";

  private final SynchronizedLong   notificationSequence = new SynchronizedLong(0L);

  public LockStatisticsMonitor(L2LockStatsManager lockStatsManager) throws NotCompliantMBeanException {
    super(LockStatisticsMonitorMBean.class, true);
    this.lockStatsManager = lockStatsManager;
  }

  public Collection<LockSpec> getLockSpecs() {
    return this.lockStatsManager.getLockSpecs();
  }

  public void setLockStatisticsConfig(int traceDepth, int gatherInterval) {
    this.lockStatsManager.setLockStatisticsConfig(traceDepth, gatherInterval);
    sendNotification(new Notification(TRACE_DEPTH, this, notificationSequence.increment()));
  }

  public void setLockStatisticsEnabled(boolean lockStatsEnabled) {
    this.lockStatsManager.setLockStatisticsEnabled(lockStatsEnabled);
    sendNotification(new Notification(TRACES_ENABLED, this, notificationSequence.increment()));
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

  public MBeanNotificationInfo[] getNotificationInfo() {
    return new MBeanNotificationInfo[] { new MBeanNotificationInfo(ALL_EVENTS, AttributeChangeNotification.class
        .getName(), DESCRIPTION) };
  }

  public void reset() {
    // nothing to do
  }
}
