/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.beans;

import com.tc.statistics.buffer.StatisticsBuffer;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferException;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;
import com.tc.statistics.retrieval.StatisticsRetriever;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.CaptureSession;

import java.util.Collection;
import java.util.Map;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

public class StatisticsManager extends StandardMBean implements StatisticsManagerMBean {
  private StatisticsRetrievalRegistry registry;
  private StatisticsBuffer buffer;
  private Map retrieverMap = new ConcurrentHashMap();
  
  public StatisticsManager(StatisticsRetrievalRegistry registry, StatisticsBuffer buffer) throws NotCompliantMBeanException {
    super(StatisticsManagerMBean.class);
    this.registry = registry;
    this.buffer = buffer;
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

  public void registerAction(long sessionId, String actionName) {
    StatisticsRetriever retriever = (StatisticsRetriever)retrieverMap.get(new Long(sessionId));
    if (null == retriever) {
      throw new RuntimeException("The capture session with ID '"+sessionId+"' couldn't be found.");
    }
    StatisticRetrievalAction action = registry.getActionInstance(actionName);
    if (null == action) {
      throw new RuntimeException("Couldn't find an action with the name '"+actionName+"' to register for capture session with ID '"+sessionId+"'.");
    }
    retriever.registerAction(action);
  }

  public void startCapturing(long sessionId) {
    try {
      buffer.startCapturing(sessionId);
    } catch (TCStatisticsBufferException e) {
      throw new RuntimeException("Error while starting the capture session with ID '"+sessionId+"'.", e);
    }
  }

  public void stopCapturing(long sessionId) {
    try {
      buffer.stopCapturing(sessionId);
      retrieverMap.remove(new Long(sessionId));
    } catch (TCStatisticsBufferException e) {
      throw new RuntimeException("Error while stopping the capture session with ID '"+sessionId+"'.", e);
    }
  }
}