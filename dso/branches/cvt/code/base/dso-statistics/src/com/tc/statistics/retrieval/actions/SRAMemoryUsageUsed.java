/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.runtime.JVMMemoryManager;
import com.tc.runtime.TCRuntime;
import com.tc.statistics.StatisticData;
import com.tc.statistics.retrieval.StatisticRetrievalAction;
import com.tc.statistics.retrieval.StatisticType;

import java.util.Date;

public class SRAMemoryUsageUsed implements StatisticRetrievalAction {
  private JVMMemoryManager manager;
  
  public SRAMemoryUsageUsed() {
    manager = TCRuntime.getJVMMemoryManager();
  }
  
  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData retrieveStatisticData() {
    return StatisticData.buildInstanceForClassAtLocalhost(getClass(), new Date(), new Long(manager.getMemoryUsage().getUsedMemory()));
  }
}