/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.beans.impl;

import com.tc.config.schema.NewCommonL2Config;
import com.tc.management.AbstractTerracottaMBean;
import com.tc.net.TCSocketAddress;
import com.tc.statistics.beans.StatisticsLocalGathererMBean;
import com.tc.statistics.gatherer.StatisticsGatherer;
import com.tc.statistics.gatherer.exceptions.TCStatisticsGathererException;
import com.tc.util.Assert;

import javax.management.NotCompliantMBeanException;

public class StatisticsLocalGathererMBeanImpl extends AbstractTerracottaMBean implements StatisticsLocalGathererMBean {
  private final StatisticsGatherer gatherer;
  private final NewCommonL2Config config;

  public StatisticsLocalGathererMBeanImpl(final StatisticsGatherer gatherer, final NewCommonL2Config config) throws NotCompliantMBeanException {
    super(StatisticsLocalGathererMBean.class, false, true);
    Assert.assertNotNull("gatherer", gatherer);
    Assert.assertNotNull("config", config);
    this.gatherer = gatherer;
    this.config = config;
  }

  public void reset() {
  }

  public void connect() {
    try {
      gatherer.connect(TCSocketAddress.LOOPBACK_IP, config.jmxPort().getInt());
    } catch (TCStatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public void disconnect() {
    try {
      gatherer.disconnect();
    } catch (TCStatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public void createSession(String sessionId) {
    try {
      gatherer.createSession(sessionId);
    } catch (TCStatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public void closeSession() {
    try {
      gatherer.closeSession();
    } catch (TCStatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public String[] getSupportedStatistics() {
    try {
      return gatherer.getSupportedStatistics();
    } catch (TCStatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public void enableStatistics(String[] names) {
    try {
      gatherer.enableStatistics(names);
    } catch (TCStatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public void startCapturing() {
    try {
      gatherer.startCapturing();
    } catch (TCStatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public void stopCapturing() {
    try {
      gatherer.stopCapturing();
    } catch (TCStatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public void setGlobalParam(String key, Object value) {
    try {
      gatherer.setGlobalParam(key, value);
    } catch (TCStatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public Object getGlobalParam(String key) {
    try {
      return gatherer.getGlobalParam(key);
    } catch (TCStatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public void setSessionParam(String key, Object value) {
    try {
      gatherer.setSessionParam(key, value);
    } catch (TCStatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }

  public Object getSessionParam(String key) {
    try {
      return gatherer.getSessionParam(key);
    } catch (TCStatisticsGathererException e) {
      throw new RuntimeException(e);
    }
  }
}