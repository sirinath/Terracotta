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

/**
 * This statistics gives the number of locks awarded globally
 * <p/>
 * The number of locks that is happening is sampled every second presently
 */
public class SRAL2GlobalLockCount implements StatisticRetrievalAction {

  public static final String ACTION_NAME = "l2 lock count";

  private final SampledCounter lockCounter;

  public SRAL2GlobalLockCount(final DSOGlobalServerStats serverStats) {
    Assert.assertNotNull("serverStats", serverStats);
    lockCounter = serverStats.getGlobalLockCounter();
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    TimeStampedCounterValue value = lockCounter.getMostRecentSample();
    return new StatisticData[] { new StatisticData(ACTION_NAME, value.getCounterValue()) };
  }
}
