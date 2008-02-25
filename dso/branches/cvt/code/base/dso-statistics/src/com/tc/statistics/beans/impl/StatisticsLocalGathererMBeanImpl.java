/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.beans.impl;

import com.tc.config.schema.NewCommonL2Config;
import com.tc.management.AbstractTerracottaMBean;
import com.tc.net.TCSocketAddress;
import com.tc.statistics.StatisticsGathererSubSystem;
import com.tc.statistics.beans.StatisticsLocalGathererMBean;
import com.tc.statistics.gatherer.exceptions.TCStatisticsGathererException;
import com.tc.statistics.store.StatisticsStore;
import com.tc.statistics.store.exceptions.TCStatisticsStoreException;
import com.tc.util.Assert;

import javax.management.NotCompliantMBeanException;

public class StatisticsLocalGathererMBeanImpl extends AbstractTerracottaMBean implements StatisticsLocalGathererMBean {
  private final StatisticsGathererSubSystem subsystem;
  private final NewCommonL2Config config;

  public StatisticsLocalGathererMBeanImpl(final StatisticsGathererSubSystem subsystem, final NewCommonL2Config config) throws NotCompliantMBeanException {
    super(StatisticsLocalGathererMBean.class, false, true);
    Assert.assertNotNull("subsystem", subsystem);
    Assert.assertNotNull("config", config);
    this.subsystem = subsystem;
    this.config = config;
  }

  public void reset() {
  }

  public void connect() {
    try {
      subsystem.getStatisticsGatherer().connect(TCSocketAddress.LOOPBACK_IP, config.jmxPort().getInt());
    } catch (TCStatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public void disconnect() {
    try {
      subsystem.getStatisticsGatherer().disconnect();
    } catch (TCStatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public void createSession(String sessionId) {
    try {
      subsystem.getStatisticsGatherer().createSession(sessionId);
    } catch (TCStatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public String getActiveSessionId() {
    return subsystem.getStatisticsGatherer().getActiveSessionId();
  }

  public void closeSession() {
    try {
      subsystem.getStatisticsGatherer().closeSession();
    } catch (TCStatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public String[] getSupportedStatistics() {
    try {
      return subsystem.getStatisticsGatherer().getSupportedStatistics();
    } catch (TCStatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public void enableStatistics(String[] names) {
    try {
      subsystem.getStatisticsGatherer().enableStatistics(names);
    } catch (TCStatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public void startCapturing() {
    try {
      subsystem.getStatisticsGatherer().startCapturing();
    } catch (TCStatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public void stopCapturing() {
    try {
      subsystem.getStatisticsGatherer().stopCapturing();
    } catch (TCStatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public void setGlobalParam(String key, Object value) {
    try {
      subsystem.getStatisticsGatherer().setGlobalParam(key, value);
    } catch (TCStatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public Object getGlobalParam(String key) {
    try {
      return subsystem.getStatisticsGatherer().getGlobalParam(key);
    } catch (TCStatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public void setSessionParam(String key, Object value) {
    try {
      subsystem.getStatisticsGatherer().setSessionParam(key, value);
    } catch (TCStatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public Object getSessionParam(String key) {
    try {
      return subsystem.getStatisticsGatherer().getSessionParam(key);
    } catch (TCStatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public void clearStatistics(String sessionId) {
    try {
      subsystem.getStatisticsStore().clearStatistics(sessionId);
    } catch (TCStatisticsStoreException e) {
      throw new RuntimeException(e);
    }
  }

  public void clearAllStatistics() {
    try {
      subsystem.getStatisticsStore().clearAllStatistics();
    } catch (TCStatisticsStoreException e) {
      throw new RuntimeException(e);
    }
  }
}