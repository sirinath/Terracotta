/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics;

public interface ObservableStatisticsManager {
  public void addListener(StatisticsManagerListener listener);

  public void removeListener(StatisticsManagerListener listener);
}
