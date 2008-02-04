/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.beans;

import com.tc.management.TerracottaMBean;

public interface StatisticsEmitterMBean extends TerracottaMBean {
  public final static Long DEFAULT_SCHEDULE_PERIOD = new Long(3000L);
}