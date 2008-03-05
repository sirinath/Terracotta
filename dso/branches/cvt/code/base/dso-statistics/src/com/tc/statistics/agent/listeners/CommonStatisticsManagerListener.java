/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.agent.listeners;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;
import com.tc.statistics.retrieval.actions.SRAStageQueueDepths;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class CommonStatisticsManagerListener extends StatisticsManagerListenerAdapter {

  protected final StageManager stageManager;

  //a map containing mapping of each action instance in the registry to a list of sessionIds
  //currently enabled for the instance
  //when the size of the list is <= zero, no one is using that action
  protected final Map actionsUsageMap = new ConcurrentHashMap();
  protected final StatisticsRetrievalRegistry registry;

  public CommonStatisticsManagerListener(StageManager stageManager, StatisticsRetrievalRegistry registry) {
    this.stageManager = stageManager;
    this.registry = registry;
    Iterator actionsIterator = registry.getSupportedStatistics().iterator();
    while (actionsIterator.hasNext()) {
      actionsUsageMap.put(actionsIterator.next(), new HashSet());
    }
  }

  public void statisticEnabled(String sessionId, String statisticName) {
    StatisticRetrievalAction action = registry.getActionInstance(statisticName);
    //make sure the statistic is available with the current sub-system
    if (action == null) return;
    synchronized (actionsUsageMap) {
      Set enabledSessionIds = (Set)actionsUsageMap.get(action.getName());
      enabledSessionIds.add(sessionId);
      actionsUsageMap.put(action.getName(), enabledSessionIds);
      enableStatisticCollection(action.getName());
    }
  }

  /**
   * Sub-classes overriding this method should always call super.enableStatisticCollection()
   */
  protected void enableStatisticCollection(String statisticName) {
    StatisticRetrievalAction action = registry.getActionInstance(statisticName);
    //make sure the statistic is available with the current sub-system
    if (action == null) return;
    if (SRAStageQueueDepths.ACTION_NAME.equals(statisticName)) {
      synchronized (stageManager) {
        Collection stages = stageManager.getStages();
        for (Iterator it = stages.iterator(); it.hasNext();) {
          ((Stage)it.next()).getSink().enableStatsCollection(true);
        }
      }
    }
  }

  public void allStatisticsDisabled(String sessionId) {
    Iterator actionsIterator = actionsUsageMap.keySet().iterator();
    synchronized (actionsUsageMap) {
      while (actionsIterator.hasNext()) {
        String action = (String)actionsIterator.next();
        Set sessionIds = (Set)actionsUsageMap.get(action);
        sessionIds.remove(sessionId);
      }
      cleanUpStatisticsCollection();
    }
  }

  /**
   * Sub-classes overriding this method should always call super.cleanUpStatisticsCollection()
   */
  protected void cleanUpStatisticsCollection() {
    cleanUpStageQueueStatistic();
  }

  protected void cleanUpStageQueueStatistic() {
    //check if stage queue depths should be enabled
    Set sessionIds = (Set)actionsUsageMap.get(SRAStageQueueDepths.ACTION_NAME);
    if (null != sessionIds && sessionIds.size() <= 0) {
      synchronized (stageManager) {
        Collection stages = stageManager.getStages();
        for (Iterator it = stages.iterator(); it.hasNext();) {
          ((Stage)it.next()).getSink().enableStatsCollection(false);
        }
      }
    }
  }

  public void capturingStopped(String sessionId) {
    //statistics agents cannot distinguish between a closeSesion() and stopCapturing() in the Gatherer:
    //statisticsGatherer has a closeSession() which in turn calls stopCapturing() on the gateway/agent
    //In an agent sub-system, when a call comes for stopCapturing(), it cannot distinguish
    //whether it was due to a closeSession() or stopCapturing()
    //If it was a closeSession(), enabled statistics need to be disabled in the agent as it is no longer needed
    //while if it was stopCapturing(), agent cannot disable the statistics as the gateway
    //might issue a startCapturing() without calling enableStatistics() again.

    //todo: This impl might have to change. For now, stopCapturing() will disable
    //all the enabled statistics for the stopped session. This might have to chnge when the
    //agent can distinguish between a stopCapturing() and closeSession() in gatherer/
    //and the gateway
    //For now, assume that stopCapturing() is same as closeSession() in the agent
    allStatisticsDisabled(sessionId);
  }
}
