/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.async.api.StageManager;
import com.tc.async.api.StageQueueStats;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.stats.Stats;
import com.tc.util.Assert;

import java.util.Date;

public class SRAStageQueueDepths implements StatisticRetrievalAction {

  public static final String ACTION_NAME = "stage queue depth";

  public final StageManager stageManager;

  public SRAStageQueueDepths(final StageManager stageManager) {
    Assert.assertNotNull(stageManager);
    this.stageManager = stageManager;
  }

  public String getName() {
    return ACTION_NAME;
  }

  public StatisticType getType() {
    return StatisticType.SNAPSHOT;
  }

  public StatisticData[] retrieveStatisticData() {
    Date moment = new Date();
    Stats[] stats = stageManager.getStats();
    StatisticData[] data = new StatisticData[stats.length];
    for (int i = 0; i < stats.length; i++) {
      StageQueueStats stageStat = (StageQueueStats)stats[i];
      data[i] = new StatisticData(ACTION_NAME + " : " + stageStat.getName(), moment, new Long(stageStat.getDepth()));
    }
    return data;
  }
}
