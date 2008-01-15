/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics;

import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticType;

public interface StatisticRetrievalAction {
  public StatisticData[] retrieveStatisticData();

  public String getName();

  public StatisticType getType();

  public void cleanup();
}