/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.agent.listeners;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

import com.tc.objectserver.handler.BroadcastChangeHandler;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;
import com.tc.statistics.retrieval.actions.SRAL2BroadcastCount;
import com.tc.statistics.retrieval.actions.SRAL2BroadcastPerTransaction;
import com.tc.statistics.retrieval.actions.SRAL2ChangesPerBroadcast;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class L2StatisticsManagerListener extends StatisticsManagerListenerAdapter {

  private final BroadcastChangeHandler broadcastChangeHandler;
  private final StatisticsRetrievalRegistry registry;

  //a map containing mapping of each action instance in the registry to a list of sessionIds
  //currently enabled for the instance
  //when the size of the list is <= zero, no one is using that action
  private final Map actionsUsageMap = new ConcurrentHashMap();

  public L2StatisticsManagerListener(StatisticsRetrievalRegistry registry, BroadcastChangeHandler broadcastChangeHandler) {
    this.broadcastChangeHandler = broadcastChangeHandler;
    this.registry = registry;
    Iterator actionsIterator = registry.getSupportedStatistics().iterator();
    while (actionsIterator.hasNext()) {
      actionsUsageMap.put(actionsIterator.next(), new HashSet());
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
      cleanUpStatistics();
    }
  }

  private void enableStatistic(String actionName) {
    if (SRAL2BroadcastCount.ACTION_NAME.equals(actionName)) {
      broadcastChangeHandler.setBroadcastCounterEnabled(true);
    } else if (SRAL2BroadcastPerTransaction.ACTION_NAME.equals(actionName)) {
      broadcastChangeHandler.setBroadcastCounterEnabled(true);
    } else if (SRAL2ChangesPerBroadcast.ACTION_NAME.equals(actionName)) {
      broadcastChangeHandler.setBroadcastCounterEnabled(true);
      broadcastChangeHandler.setChangeCounterEnabled(true);
    }
  }

  private void cleanUpStatistics() {
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

    //check if changes counter should be enabled
    sessionIds = (Set)actionsUsageMap.get(SRAL2ChangesPerBroadcast.ACTION_NAME);
    if (null != sessionIds && sessionIds.size() <= 0) {
      broadcastChangeHandler.setChangeCounterEnabled(false);
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
      enableStatistic(action.getName());
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
