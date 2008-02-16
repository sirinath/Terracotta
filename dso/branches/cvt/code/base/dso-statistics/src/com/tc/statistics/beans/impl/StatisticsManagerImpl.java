/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.beans.impl;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

import com.tc.management.AbstractTerracottaMBean;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.beans.StatisticsManagerMBean;
import com.tc.statistics.buffer.StatisticsBuffer;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferException;
import com.tc.statistics.config.StatisticsConfig;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;
import com.tc.statistics.retrieval.StatisticsRetriever;
import com.tc.util.Assert;

import java.util.Collection;
import java.util.Map;

import javax.management.NotCompliantMBeanException;

public class StatisticsManagerImpl extends AbstractTerracottaMBean implements StatisticsManagerMBean {
  private final StatisticsConfig config;
  private final StatisticsRetrievalRegistry registry;
  private final StatisticsBuffer buffer;
  private final Map retrieverMap = new ConcurrentHashMap();
  
  public StatisticsManagerImpl(final StatisticsConfig config, final StatisticsRetrievalRegistry registry, final StatisticsBuffer buffer) throws NotCompliantMBeanException {
    super(StatisticsManagerMBean.class, false, true);

    Assert.assertNotNull("config", config);
    Assert.assertNotNull("registry", registry);
    Assert.assertNotNull("buffer", buffer);

    this.config = config;
    this.registry = registry;
    this.buffer = buffer;
  }

  public void reset() {
  }

  public String[] getSupportedStatistics() {
    Collection stats = registry.getSupportedStatistics();
    String[] statistics = new String[stats.size()];
    stats.toArray(statistics);
    return statistics;
  }

  public void createSession(final String sessionId) {
    try {
      StatisticsRetriever retriever = buffer.createCaptureSession(sessionId);
      retrieverMap.put(sessionId, retriever);
    } catch (TCStatisticsBufferException e) {
      throw new RuntimeException("Unexpected error while creating a new capture session.", e);
    }
  }

  public void disableAllStatistics(final String sessionId) {
    StatisticsRetriever retriever = obtainRetriever(sessionId);
    retriever.removeAllActions();
  }

  public boolean enableStatistic(final String sessionId, final String name) {
    StatisticsRetriever retriever = obtainRetriever(sessionId);
    StatisticRetrievalAction action = registry.getActionInstance(name);
    if (null == action) {
      return false;
    }
    retriever.registerAction(action);
    return true;
  }

  public void startCapturing(final String sessionId) {
    try {
      buffer.startCapturing(sessionId);
    } catch (TCStatisticsBufferException e) {
      throw new RuntimeException("Error while starting the capture session with cluster-wide ID '"+sessionId+"'.", e);
    }
  }

  public void stopCapturing(final String sessionId) {
    try {
      buffer.stopCapturing(sessionId);
      retrieverMap.remove(sessionId);
    } catch (TCStatisticsBufferException e) {
      throw new RuntimeException("Error while stopping the capture session with cluster-wide ID '"+sessionId+"'.", e);
    }
  }

  public void setGlobalParam(final String key, final Object value) {
    config.setParam(key, value);
  }

  public Object getGlobalParam(final String key) {
    return config.getParam(key);
  }

  public void setSessionParam(final String sessionId, final String key, final Object value) {
    StatisticsRetriever retriever = obtainRetriever(sessionId);
    retriever.getConfig().setParam(key, value);
  }

  public Object getSessionParam(final String sessionId, final String key) {
    StatisticsRetriever retriever = obtainRetriever(sessionId);
    return retriever.getConfig().getParam(key);
  }

  StatisticsRetriever obtainRetriever(final String sessionId) {
    StatisticsRetriever retriever = (StatisticsRetriever)retrieverMap.get(sessionId);
    if (null == retriever) {
      throw new RuntimeException("The statistics retriever for the capture session with cluster-wide ID '"+sessionId+"' couldn't be found.");
    }
    return retriever;
  }
}