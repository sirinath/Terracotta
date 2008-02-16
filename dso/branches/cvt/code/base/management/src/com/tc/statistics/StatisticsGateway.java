/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics;

import javax.management.MBeanServerConnection;

public interface StatisticsGateway  {
  public void addStatisticsAgent(MBeanServerConnection mbeanServerConnection);
  public void cleanup();
}