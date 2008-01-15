/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.TimeStampedCounterValue;
import com.tc.util.Assert;

import java.util.Date;

public class SRAL2ToL1FaultRate implements StatisticRetrievalAction {

  public final static String ACTION_NAME = "l2 l1 fault";

  private SampledCounter counter;
  
  public SRAL2ToL1FaultRate(DSOGlobalServerStats serverStats) {
    Assert.assertNotNull("serverStats", serverStats);
    counter = serverStats.getObjectFaultCounter();
  }

  public String getName() {
    return ACTION_NAME;
  }
  
  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    TimeStampedCounterValue value = counter.getMostRecentSample();
    Date moment = new Date(); // todo: this might have to be changed into new Date(value.getTimestamp()), which is when the actual sampling occurred
    return new StatisticData[] {StatisticData.buildInstanceForLocalhost(ACTION_NAME, moment, value.getCounterValue())};
  }

  public void cleanup() {
    // nothing to clean up
  }
}