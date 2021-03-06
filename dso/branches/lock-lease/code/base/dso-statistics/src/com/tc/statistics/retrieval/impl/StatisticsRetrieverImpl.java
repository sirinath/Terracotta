/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.impl;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.TCPropertiesImpl;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.statistics.buffer.StatisticsBuffer;
import com.tc.statistics.buffer.StatisticsBufferListener;
import com.tc.statistics.buffer.exceptions.StatisticsBufferException;
import com.tc.statistics.config.StatisticsConfig;
import com.tc.statistics.retrieval.StatisticsRetriever;
import com.tc.statistics.retrieval.actions.SRAShutdownTimestamp;
import com.tc.statistics.retrieval.actions.SRAStartupTimestamp;
import com.tc.util.Assert;
import com.tc.util.TCTimerImpl;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class StatisticsRetrieverImpl implements StatisticsRetriever, StatisticsBufferListener {
  public final static int DEFAULT_NOTIFICATION_INTERVAL = 5;

  private final static TCLogger logger = TCLogging.getLogger(StatisticsRetrieverImpl.class);

  private final Timer timer = new TCTimerImpl("Statistics Retriever Timer", true);

  private final StatisticsConfig config;
  private final StatisticsBuffer buffer;
  private final String sessionId;

  // the structure of this map is created in a synchronized fashion in the createEmptyActionsMap
  // method, outside of that method the map is only read and the values (which are lists) are being
  // modified (not replaced)
  private volatile Map actionsMap;

  private LogRetrievalInProcessTask infoTask = null;
  private RetrieveStatsTask statsTask = null;

  public StatisticsRetrieverImpl(final StatisticsConfig config, final StatisticsBuffer buffer, final String sessionId) {
    Assert.assertNotNull("config", config);
    Assert.assertNotNull("buffer", buffer);
    Assert.assertNotNull("sessionId", sessionId);
    this.config = config;
    this.buffer = buffer;
    this.sessionId = sessionId;

    // keep at the end of the constructor to make sure that all the initialization
    // is done before registering this instance as a listener
    this.buffer.addListener(this);

    createEmptyActionsMap();
  }

  private synchronized void createEmptyActionsMap() {
    // initialize the map of actions that are organized according
    // to their type
    Map actions_map_construction = new HashMap();
    for (Iterator types_it = StatisticType.getAllTypes().iterator(); types_it.hasNext();) {
      StatisticType type = (StatisticType)types_it.next();
      actions_map_construction.put(type, new CopyOnWriteArrayList());
    }
    actionsMap = Collections.unmodifiableMap(actions_map_construction);
  }

  public String getSessionId() {
    return sessionId;
  }

  public StatisticsConfig getConfig() {
    return config;
  }

  public void removeAllActions() {
    createEmptyActionsMap();
  }

  public void registerAction(final StatisticRetrievalAction action) {
    if (null == action) return;
    if (null == action.getType()) Assert.fail("Can't register an action with a null type.");

    List action_list = (List)actionsMap.get(action.getType());
    if (null == action_list) {
      Assert.fail("the actionsMap doesn't contain an entry for the statistic type '" + action.getType() + "'");
      return;
    }
    action_list.add(action);
  }

  public void startup() {
    retrieveStartupMarker();
    retrieveStartupStatistics();
    enableTimerTasks();
  }

  public void shutdown() {
    buffer.removeListener(this);

    disableTimerTasks();
  }

  public boolean containsAction(StatisticRetrievalAction action) {
    if (null == action) return false;
    if (null == action.getType()) return false;

    List action_list = (List)actionsMap.get(action.getType());
    if (null == action_list) {
      return false;
    }
    return action_list.contains(action);
  }

  private void retrieveStartupMarker() {
    retrieveAction(new Date(), new SRAStartupTimestamp());
  }

  private void retrieveShutdownMarker(final Date moment) {
    retrieveAction(moment, new SRAShutdownTimestamp());
  }

  private void retrieveStartupStatistics() {
    List action_list = (List)actionsMap.get(StatisticType.STARTUP);
    Assert.assertNotNull("list of startup actions", action_list);
    final Date moment = new Date();
    for (Iterator actions_it = action_list.iterator(); actions_it.hasNext();) {
      retrieveAction(moment, (StatisticRetrievalAction)actions_it.next());
    }
  }

  private void retrieveAction(final Date moment, final StatisticRetrievalAction action) {
    StatisticData[] data = action.retrieveStatisticData();
    if (data != null) {
      for (int i = 0; i < data.length; i++) {
        data[i].setSessionId(sessionId);
        data[i].setMoment(moment);
        bufferData(data[i]);
      }
    }
  }

  private void bufferData(final StatisticData data) {
    try {
      buffer.storeStatistic(data);
    } catch (StatisticsBufferException e) {
      throw new TCRuntimeException(e);
    }
  }

  private synchronized void enableTimerTasks() {
    if (statsTask != null ||
        infoTask != null) {
      disableTimerTasks();
    }

    infoTask = new LogRetrievalInProcessTask();
    timer.scheduleAtFixedRate(infoTask, 0, TCPropertiesImpl.getProperties().getInt("cvt.retriever.notification.interval", DEFAULT_NOTIFICATION_INTERVAL) * 1000);

    statsTask = new RetrieveStatsTask();
    timer.scheduleAtFixedRate(statsTask, 0, config.getParamLong(StatisticsConfig.KEY_RETRIEVER_SCHEDULE_INTERVAL));
  }

  private synchronized void disableTimerTasks() {
    if (statsTask != null) {
      statsTask.shutdown();
      statsTask = null;
    }

    if (infoTask != null) {
      infoTask.shutdown();
      infoTask = null;
    }
  }

  public void capturingStarted(final String sessionId) {
    if (sessionId.equals(this.sessionId)) {
      startup();
    }
  }

  public void capturingStopped(final String sessionId) {
    if (sessionId.equals(this.sessionId)) {
      shutdown();
    }
  }

  public void opened() {
  }

  public void closed() {
    shutdown();
  }

  private class RetrieveStatsTask extends TimerTask {
    private boolean performTaskShutdown = false;

    public void shutdown() {
      performTaskShutdown = true;
    }

    public void run() {
      List action_list = (List)actionsMap.get(StatisticType.SNAPSHOT);
      Assert.assertNotNull("list of snapshot actions", action_list);
      final Date moment = new Date();
      for (Iterator actions_it = action_list.iterator(); actions_it.hasNext();) {
        retrieveAction(moment, (StatisticRetrievalAction)actions_it.next());
      }

      if (performTaskShutdown) {
        cancel();
        retrieveShutdownMarker(new Date(moment.getTime()+1));
      }
    }
  }

  private class LogRetrievalInProcessTask extends TimerTask {
    private final long start;

    private volatile boolean shutdown = false;

    private LogRetrievalInProcessTask() {
      start = System.currentTimeMillis();
      logger.info("Statistics retrieval is STARTING for session ID '" + sessionId + "' on node '" + buffer.getDefaultNodeName() + "'.");
    }

    public void shutdown() {
      shutdown = true;
      logger.info("Statistics retrieval has STOPPED for session ID '" + sessionId + "' on node '" + buffer.getDefaultNodeName() + "' after running for " + ((System.currentTimeMillis() - start) / 1000) + " seconds.");
      this.cancel();
    }

    public void run() {
      if (!shutdown) {
        logger.info("Statistics retrieval in PROCESS for session ID '" + sessionId + "' on node '" + buffer.getDefaultNodeName() + "' for " + ((System.currentTimeMillis() - start) / 1000) + " seconds.");
      }
    }
  }
}
