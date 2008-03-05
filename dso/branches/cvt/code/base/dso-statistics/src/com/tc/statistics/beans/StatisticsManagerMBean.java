/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.beans;

import com.tc.management.TerracottaMBean;
import com.tc.statistics.StatisticsManager;
import com.tc.statistics.ObservableStatisticsManager;

public interface StatisticsManagerMBean extends TerracottaMBean, StatisticsManager, ObservableStatisticsManager {
}