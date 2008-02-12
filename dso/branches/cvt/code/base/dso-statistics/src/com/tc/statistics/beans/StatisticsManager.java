/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.beans;

import com.tc.statistics.CaptureSession;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.config.StatisticsConfig;
import com.tc.statistics.buffer.StatisticsBuffer;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferException;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;
import com.tc.statistics.retrieval.StatisticsRetriever;
import com.tc.util.Assert;
import com.tc.management.AbstractTerracottaMBean;

import java.util.Collection;
import java.util.Map;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

public class StatisticsManager extends AbstractTerracottaMBean implements StatisticsManagerMBean {
  private final StatisticsConfig config;
  private final StatisticsRetrievalRegistry registry;
  private final StatisticsBuffer buffer;
  private final Map retrieverMap = new ConcurrentHashMap();
  
  public StatisticsManager(final StatisticsConfig config, final StatisticsRetrievalRegistry registry, final StatisticsBuffer buffer) throws NotCompliantMBeanException {
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

  public long createCaptureSession() {
    try {
      CaptureSession session = buffer.createCaptureSession();

      retrieverMap.put(new Long(session.getId()), session.getRetriever());
      return session.getId();
    } catch (TCStatisticsBufferException e) {
      throw new RuntimeException("Unexpected error while creating a new capture session.", e);
    }
  }

  public void disableAllStatistics(final long sessionId) {
    StatisticsRetriever retriever = obtainRetriever(sessionId);
    retriever.removeAllActions();
  }

  public void enableStatistic(final long sessionId, final String name) {
    StatisticsRetriever retriever = obtainRetriever(sessionId);
    StatisticRetrievalAction action = registry.getActionInstance(name);
    if (null == action) {
      throw new RuntimeException("Couldn't find a statistic retrieval action with the name '"+name+"' to register for capture session with ID '"+sessionId+"'.");
    }
    retriever.registerAction(action);
  }

  public void startCapturing(final long sessionId) {
    try {
      buffer.startCapturing(sessionId);
    } catch (TCStatisticsBufferException e) {
      throw new RuntimeException("Error while starting the capture session with ID '"+sessionId+"'.", e);
    }
  }

  public void stopCapturing(final long sessionId) {
    try {
      buffer.stopCapturing(sessionId);
      retrieverMap.remove(new Long(sessionId));
    } catch (TCStatisticsBufferException e) {
      throw new RuntimeException("Error while stopping the capture session with ID '"+sessionId+"'.", e);
    }
  }

  public void setGlobalParam(final String key, final Object value) {
    config.setParam(key, value);
  }

  public Object getGlobalParam(final String key) {
    return config.getParam(key);
  }

  public void setSessionParam(final long sessionId, final String key, final Object value) {
    StatisticsRetriever retriever = obtainRetriever(sessionId);
    retriever.getConfig().setParam(key, value);
  }

  public Object getSessionParam(final long sessionId, final String key) {
    StatisticsRetriever retriever = obtainRetriever(sessionId);
    return retriever.getConfig().getParam(key);
  }

  StatisticsRetriever obtainRetriever(final long sessionId) {
    StatisticsRetriever retriever = (StatisticsRetriever)retrieverMap.get(new Long(sessionId));
    if (null == retriever) {
      throw new RuntimeException("The statistics retriever for the capture session with ID '"+sessionId+"' couldn't be found.");
    }
    return retriever;
  }
}