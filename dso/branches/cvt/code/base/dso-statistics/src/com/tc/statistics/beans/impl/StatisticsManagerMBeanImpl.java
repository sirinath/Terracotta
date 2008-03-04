/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.beans.impl;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArraySet;

import com.tc.management.AbstractTerracottaMBean;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticsManagerListener;
import com.tc.statistics.beans.StatisticsManagerMBean;
import com.tc.statistics.beans.exceptions.UnknownStatisticsSessionIdException;
import com.tc.statistics.buffer.StatisticsBuffer;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferStartCapturingSessionNotFoundException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferStopCapturingSessionNotFoundException;
import com.tc.statistics.config.StatisticsConfig;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;
import com.tc.statistics.retrieval.StatisticsRetriever;
import com.tc.util.Assert;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.management.NotCompliantMBeanException;

public class StatisticsManagerMBeanImpl extends AbstractTerracottaMBean implements StatisticsManagerMBean {
  private final StatisticsConfig config;
  private final StatisticsRetrievalRegistry registry;
  private final StatisticsBuffer buffer;
  private final Map retrieverMap = new ConcurrentHashMap();
  private final Set managerListeners = new CopyOnWriteArraySet();

  public StatisticsManagerMBeanImpl(final StatisticsConfig config, final StatisticsRetrievalRegistry registry, final StatisticsBuffer buffer) throws NotCompliantMBeanException {
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

  public synchronized void reinitialize() {
    boolean was_enabled = isEnabled();

    disable();

    Set session_ids = retrieverMap.keySet();
    for (Iterator it = session_ids.iterator(); it.hasNext(); ) {
      stopCapturing((String)it.next());
    }

    try {
      buffer.reinitialize();
    } catch (TCStatisticsBufferException e) {
      throw new RuntimeException("Unexpected error while reinitializing the buffer.");
    }

    if (was_enabled) {
      enable();
    }
  }

  public String[] getSupportedStatistics() {
    Collection stats = registry.getSupportedStatistics();
    return (String[])stats.toArray(new String[stats.size()]);
  }

  public synchronized void createSession(final String sessionId) {
    try {
      StatisticsRetriever retriever = buffer.createCaptureSession(sessionId);
      retrieverMap.put(sessionId, retriever);
    } catch (TCStatisticsBufferException e) {
      throw new RuntimeException("Unexpected error while creating a new capture session.", e);
    }
  }

  public synchronized void disableAllStatistics(final String sessionId) {
    StatisticsRetriever retriever = obtainRetriever(sessionId);
    retriever.removeAllActions();
    fireAllStatisticsDisabled(sessionId);
  }

  private void fireAllStatisticsDisabled(final String sessionId) {
    //notify listeners about change in subscription of statistics
    if (null != managerListeners && managerListeners.size() > 0) {
      Iterator iterator = managerListeners.iterator();
      while (iterator.hasNext()) {
        ((StatisticsManagerListener)iterator.next()).allStatisticsDisabled(sessionId);
      }
    }
  }

  public synchronized boolean enableStatistic(final String sessionId, final String name) {
    StatisticsRetriever retriever = obtainRetriever(sessionId);
    StatisticRetrievalAction action = registry.getActionInstance(name);
    if (null == action) {
      return false;
    }
    retriever.registerAction(action);
    fireStatisticEnabled(sessionId, name);
    return true;
  }

  private void fireStatisticEnabled(final String sessionId, final String name) {
    //notify listeners about change in subscription of statistics
    if (null != managerListeners && managerListeners.size() > 0) {
      Iterator iterator = managerListeners.iterator();
      while (iterator.hasNext()) {
        ((StatisticsManagerListener)iterator.next()).statisticEnabled(sessionId, name);
      }
    }
  }

  public StatisticData[] captureStatistic(final String sessionId, final String name) {
    // obtain the retriever to make sure that the provided session ID is active
    StatisticsRetriever retriever = obtainRetriever(sessionId);

    StatisticRetrievalAction action = registry.getActionInstance(name);
    if (null == action) {
      return null;
    }

    StatisticData[] data = action.retrieveStatisticData();
    if (data != null) {
      for (int i = 0; i < data.length; i++) {
        data[i].setSessionId(sessionId);
        try {
          buffer.storeStatistic(data[i]);
        } catch (TCStatisticsBufferException e) {
          throw new RuntimeException("Error while storing the statistic data '" + name + "' for cluster-wide ID '" + sessionId + "'.", e);
        }
      }
    } else {
      data = StatisticData.EMPTY_ARRAY;
    }
    return data;
  }

  private String getNodeName() {
    return buffer.getDefaultAgentIp() + " (" + buffer.getDefaultAgentDifferentiator() + ")";
  }

  public synchronized void startCapturing(final String sessionId) {
    try {
      buffer.startCapturing(sessionId);
    } catch (TCStatisticsBufferStartCapturingSessionNotFoundException e) {
      throw new UnknownStatisticsSessionIdException(getNodeName(), e.getSessionId(), e);
    } catch (TCStatisticsBufferException e) {
      throw new RuntimeException("Error while starting the capture session with cluster-wide ID '" + sessionId + "'.", e);
    }
  }

  public synchronized void stopCapturing(final String sessionId) {
    try {
      buffer.stopCapturing(sessionId);
      retrieverMap.remove(sessionId);
      fireCapturingStopped(sessionId);
    } catch (TCStatisticsBufferStopCapturingSessionNotFoundException e) {
      throw new UnknownStatisticsSessionIdException(getNodeName(), e.getSessionId(), e);
    } catch (TCStatisticsBufferException e) {
      throw new RuntimeException("Error while stopping the capture session with cluster-wide ID '" + sessionId + "'.", e);
    }
  }

  private void fireCapturingStopped(final String sessionId) {
    //notify listeners about capturing stopped
    if (null != managerListeners && managerListeners.size() > 0) {
      Iterator iterator = managerListeners.iterator();
      while (iterator.hasNext()) {
        ((StatisticsManagerListener)iterator.next()).capturingStopped(sessionId);
      }
    }
  }

  public void setGlobalParam(final String key, final Object value) {
    config.setParam(key, value);
  }

  public Object getGlobalParam(final String key) {
    return config.getParam(key);
  }

  public synchronized void setSessionParam(final String sessionId, final String key, final Object value) {
    StatisticsRetriever retriever = obtainRetriever(sessionId);
    retriever.getConfig().setParam(key, value);
  }

  public synchronized Object getSessionParam(final String sessionId, final String key) {
    StatisticsRetriever retriever = obtainRetriever(sessionId);
    return retriever.getConfig().getParam(key);
  }

  public void addListener(StatisticsManagerListener listener) {
    if (null == listener) {
      return;
    }
    managerListeners.add(listener);
  }

  public void removeListener(StatisticsManagerListener listener) {
    if (null == listener) {
      return;
    }
    managerListeners.remove(listener);
  }

  StatisticsRetriever obtainRetriever(final String sessionId) {
    StatisticsRetriever retriever = (StatisticsRetriever)retrieverMap.get(sessionId);
    if (null == retriever) {
      throw new UnknownStatisticsSessionIdException(getNodeName(), sessionId, null);
    }
    return retriever;
  }
}