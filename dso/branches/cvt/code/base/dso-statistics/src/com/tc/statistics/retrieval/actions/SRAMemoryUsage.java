/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.runtime.JVMMemoryManager;
import com.tc.runtime.MemoryUsage;
import com.tc.runtime.TCRuntime;
import com.tc.statistics.StatisticData;
import com.tc.statistics.retrieval.StatisticRetrievalAction;
import com.tc.statistics.retrieval.StatisticType;

import java.util.Date;

public class SRAMemoryUsage implements StatisticRetrievalAction {
  public final static String ELEMENT_FREE = "free";
  public final static String ELEMENT_USED = "used";
  public final static String ELEMENT_MAX = "max";

  private JVMMemoryManager manager;

  public SRAMemoryUsage() {
    manager = TCRuntime.getJVMMemoryManager();
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    Date moment = new Date();
    MemoryUsage usage = manager.getMemoryUsage();
    return new StatisticData[] {
      StatisticData.buildInstanceForClassAtLocalhost(getClass(), moment, ELEMENT_FREE, new Long(usage.getFreeMemory())),
      StatisticData.buildInstanceForClassAtLocalhost(getClass(), moment, ELEMENT_USED, new Long(usage.getUsedMemory())),
      StatisticData.buildInstanceForClassAtLocalhost(getClass(), moment, ELEMENT_MAX, new Long(usage.getMaxMemory()))
    };
  }

  public void cleanup() {
    // nothing to clean up
  }
}