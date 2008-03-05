/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.agent.listeners;

import com.tc.objectserver.handler.BroadcastChangeHandler;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;
import com.tc.statistics.retrieval.actions.SRAL2BroadcastCount;
import com.tc.statistics.retrieval.actions.SRAL2BroadcastPerTransaction;
import com.tc.statistics.retrieval.actions.SRAL2ChangesPerBroadcast;
import com.tc.async.api.StageManager;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class L2StatisticsManagerListener extends CommonStatisticsManagerListener {

  private final BroadcastChangeHandler broadcastChangeHandler;

  public L2StatisticsManagerListener(StageManager stageManager, StatisticsRetrievalRegistry registry, BroadcastChangeHandler broadcastChangeHandler) {
    super(stageManager, registry);
    this.broadcastChangeHandler = broadcastChangeHandler;
  }

  /**
    Sub-classes overriding this method should always call super.enableStatisticCollection()
   */
  protected void enableStatisticCollection(String actionName) {
    super.enableStatisticCollection(actionName);
    if (SRAL2BroadcastCount.ACTION_NAME.equals(actionName)) {
      broadcastChangeHandler.setBroadcastCounterEnabled(true);
    } else if (SRAL2BroadcastPerTransaction.ACTION_NAME.equals(actionName)) {
      broadcastChangeHandler.setBroadcastCounterEnabled(true);
    } else if (SRAL2ChangesPerBroadcast.ACTION_NAME.equals(actionName)) {
      broadcastChangeHandler.setBroadcastCounterEnabled(true);
      broadcastChangeHandler.setChangeCounterEnabled(true);
    }
  }

  /**
    Sub-classes overriding this method should always call super.cleanUpStatisticsCollection()
   */
  protected void cleanUpStatisticsCollection() {
    super.cleanUpStatisticsCollection();
    cleanUpBroadcastStatistic();
    cleanUpChangesStatistic();
  }

  private void cleanUpBroadcastStatistic() {
    //check if broadcast counter should be enabled
    boolean broadcastCountSRAUsed = true;
    Set sessionIds = (Set)actionsUsageMap.get(SRAL2BroadcastCount.ACTION_NAME);
    if (null != sessionIds && sessionIds.size() <= 0) {
      broadcastCountSRAUsed = false;
    }
    if (!broadcastCountSRAUsed) {
      sessionIds = (Set)actionsUsageMap.get(SRAL2BroadcastPerTransaction.ACTION_NAME);
      if (sessionIds != null && sessionIds.size() <= 0) {
        broadcastCountSRAUsed = false;
      } else {
        broadcastCountSRAUsed = true;
      }
    }
    if (!broadcastCountSRAUsed) {
      sessionIds = (Set)actionsUsageMap.get(SRAL2ChangesPerBroadcast.ACTION_NAME);
      if (sessionIds != null && sessionIds.size() <= 0) {
        //broadcast counter is being used by only 3 SRA's right now
        //if no SRA out of these 3 is enabled, disable broadcasts counter
        broadcastChangeHandler.setBroadcastCounterEnabled(false);
      }
    }
  }

  private void cleanUpChangesStatistic() {
    //check if changes counter should be enabled
    Set sessionIds = (Set)actionsUsageMap.get(SRAL2ChangesPerBroadcast.ACTION_NAME);
    if (null != sessionIds && sessionIds.size() <= 0) {
      broadcastChangeHandler.setChangeCounterEnabled(false);
    }
  }

}
