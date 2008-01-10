/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.statistics.StatisticData;
import com.tc.statistics.retrieval.StatisticRetrievalAction;
import com.tc.statistics.retrieval.StatisticType;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.TimeStampedCounterValue;
import com.tc.util.Assert;

import java.util.Date;

public class SRAL2ToL1FaultRate implements StatisticRetrievalAction {
  private SampledCounter counter;
  
  public SRAL2ToL1FaultRate(DSOGlobalServerStats serverStats) {
    Assert.assertNotNull("serverStats", serverStats);
    counter = serverStats.getObjectFaultCounter();
  }
  
  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData retrieveStatisticData() {
    TimeStampedCounterValue value = counter.getMostRecentSample();
    return StatisticData.buildInstanceForClassAtLocalhost(getClass(), new Date(value.getTimestamp()), new Long(value.getCounterValue()));
  }
}