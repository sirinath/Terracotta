/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.impl;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;

import com.tc.exception.TCRuntimeException;
import com.tc.statistics.StatisticData;
import com.tc.statistics.buffer.StatisticsBuffer;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferException;
import com.tc.statistics.retrieval.StatisticRetrievalAction;
import com.tc.statistics.retrieval.StatisticType;
import com.tc.statistics.retrieval.StatisticsRetriever;
import com.tc.util.Assert;
import com.tc.util.TCTimerImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class StatisticsRetrieverImpl implements StatisticsRetriever {
  private final StatisticsBuffer buffer;
  private final Map actionsMap;
  
  private long schedulePeriod = 1000; // HACK: make configurable
  private Timer timer = null;
  private TimerTask task = null;
  
  private long sessionId;
  
  public StatisticsRetrieverImpl(StatisticsBuffer buffer, long sessionId) {
    Assert.assertNotNull("buffer", buffer);
    this.buffer = buffer;
    this.sessionId = sessionId;
    
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

  public void registerAction(StatisticRetrievalAction action) {
    if (null == action) return;
    
    List action_list = (List)actionsMap.get(action.getType());
    if (null == action_list) Assert.fail("the actionsMap doesn't contain an entry for the statistic type '"+action.getType()+"'");
    action_list.add(action);
  }

  public void startup() {
    retrieveStartupStatistics();
    enableTimer();
  }

  public void shutdown() {
    disableTimer();
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
    StatisticData data = action.retrieveStatisticData();
    bufferData(data);
  }
  
  private void bufferData(StatisticData data) {
    try {
      buffer.storeStatistic(sessionId, data);
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
