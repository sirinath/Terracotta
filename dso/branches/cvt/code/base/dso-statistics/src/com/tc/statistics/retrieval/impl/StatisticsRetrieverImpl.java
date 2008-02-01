/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.impl;

import com.tc.exception.TCRuntimeException;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.statistics.buffer.StatisticsBuffer;
import com.tc.statistics.buffer.StatisticsBufferListener;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferException;
import com.tc.statistics.retrieval.StatisticsRetriever;
import com.tc.statistics.retrieval.actions.SRAShutdownTimestamp;
import com.tc.statistics.retrieval.actions.SRAStartupTimestamp;
import com.tc.util.Assert;
import com.tc.util.TCTimerImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;

public class StatisticsRetrieverImpl implements StatisticsRetriever, StatisticsBufferListener {
  private final StatisticsBuffer buffer;

  private Map actionsMap;

  private long schedulePeriod = 1000; // HACK: make configurable
  private Timer timer = null;
  private TimerTask task = null;

  private long sessionId;

  public StatisticsRetrieverImpl(StatisticsBuffer buffer, long sessionId) {
    Assert.assertNotNull("buffer", buffer);
    this.buffer = buffer;
    this.sessionId = sessionId;

    this.buffer.addListener(this);

    createEmptyActionsMap();
  }

  private void createEmptyActionsMap() {
    // initialize the map of actions that are organized according
    // to their type
    Map actions_map_construction = new HashMap();
    Iterator types_it = StatisticType.getAllTypes().iterator();
    while (types_it.hasNext()) {
      StatisticType type = (StatisticType)types_it.next();
      actions_map_construction.put(type, new CopyOnWriteArrayList());
    }
    actionsMap = Collections.unmodifiableMap(actions_map_construction);
  }

  public long getSessionId() {
    return sessionId;
  }

  public void removeAllActions() {
    Map old_actions_map = actionsMap;

    createEmptyActionsMap();    

    Iterator values_it = old_actions_map.values().iterator();
    while (values_it.hasNext()) {
      List previous_actions = (List)values_it.next();
      Iterator action_it = previous_actions.iterator();
      while (action_it.hasNext()) {
        StatisticRetrievalAction action = (StatisticRetrievalAction)action_it.next();
        action.cleanup();
      }
    }
  }

  public void registerAction(StatisticRetrievalAction action) {
    if (null == action) return;
    if (null == action.getType()) Assert.fail("Can't register an action with a null type.");

    List action_list = (List)actionsMap.get(action.getType());
    if (null == action_list) {
      Assert.fail("the actionsMap doesn't contain an entry for the statistic type '" + action.getType() + "'");
    }
    action_list.add(action);
  }

  public void startup() {
    retrieveStartupMarker();
    retrieveStartupStatistics();
    enableTimer();
  }

  public void shutdown() {
    disableTimer();
    retrieveShutdownMarker();
    cleanupActions();
  }

  private void retrieveStartupMarker() {
    retrieveAction(new SRAStartupTimestamp());
  }

  private void retrieveShutdownMarker() {
    retrieveAction(new SRAShutdownTimestamp());
  }

  private void cleanupActions() {
    Iterator values_it = actionsMap.values().iterator();
    while (values_it.hasNext()) {
      List actions = (List)values_it.next();
      Iterator action_it = actions.iterator();
      while (action_it.hasNext()) {
        StatisticRetrievalAction action = (StatisticRetrievalAction)action_it.next();
        action.cleanup();
      }
    }
  }

  private void retrieveStartupStatistics() {
    List action_list = (List)actionsMap.get(StatisticType.STARTUP);
    Assert.assertNotNull("list of startup actions", action_list);
    Iterator actions_it = action_list.iterator();
    while (actions_it.hasNext()) {
      retrieveAction((StatisticRetrievalAction)actions_it.next());
    }
  }

  private void retrieveAction(StatisticRetrievalAction action) {
    StatisticData[] data = action.retrieveStatisticData();
    if (data != null) {
      for (int i = 0; i < data.length; i++) {
        data[i].setSessionId(new Long(sessionId));
        bufferData(data[i]);
      }
    }
  }

  private void bufferData(StatisticData data) {
    try {
      buffer.storeStatistic(data);
    } catch (TCStatisticsBufferException e) {
      throw new TCRuntimeException(e);
    }
  }

  private synchronized void enableTimer() {
    if (timer != null &&
        task != null) {
      disableTimer();
    }

    timer = new TCTimerImpl("Statistics Retriever Timer", true);
    task = new RetrieveStatsTask();
    timer.scheduleAtFixedRate(task, 0, schedulePeriod);
  }

  private synchronized void disableTimer() {
    if (timer != null &&
        task != null) {
      task.cancel();
      timer.cancel();
      task = null;
      timer = null;
    }
  }

  public void capturingStarted(long sessionId) {
    startup();
  }

  public void capturingStopped(long sessionId) {
    shutdown();
  }

  private class RetrieveStatsTask extends TimerTask {
    public void run() {
      List action_list = (List)actionsMap.get(StatisticType.SNAPSHOT);
      Assert.assertNotNull("list of snapshot actions", action_list);
      Iterator actions_it = action_list.iterator();
      while (actions_it.hasNext()) {
        retrieveAction((StatisticRetrievalAction)actions_it.next());
      }
    }
  }
}
