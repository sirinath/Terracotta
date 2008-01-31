/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.gatherer.impl;

import com.tc.management.JMXConnectorProxy;
import com.tc.statistics.beans.StatisticsEmitterMBean;
import com.tc.statistics.beans.StatisticsMBeansNames;
import com.tc.statistics.beans.StatisticsManagerMBean;
import com.tc.statistics.gatherer.StatisticsGatherer;
import com.tc.statistics.gatherer.exceptions.TCStatisticsGathererException;
import com.tc.statistics.store.StatisticsStore;
import com.tc.statistics.store.exceptions.TCStatisticsStoreException;
import com.tc.statistics.StatisticData;
import com.tc.statistics.retrieval.actions.SRAShutdownTimestamp;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.Collection;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.NotificationListener;
import javax.management.Notification;

public class StatisticsGathererImpl implements StatisticsGatherer {
  private final StatisticsStore store;

  private JMXConnectorProxy proxy = null;
  private StatisticsManagerMBean statManager = null;
  private StatisticsEmitterMBean statEmitter = null;

  public StatisticsGathererImpl(StatisticsStore store) {
    Assert.assertNotNull("store can't be null", store);
    this.store = store;
  }

  public void initialize() throws TCStatisticsGathererException {
    try {
      store.open();
    } catch (TCStatisticsStoreException e) {
      throw new RuntimeException(e);
    }
  }

  public void connectManager(String managerHostName, int managerPort) throws TCStatisticsGathererException {
    proxy = new JMXConnectorProxy(managerHostName, managerPort);
    MBeanServerConnection mbeanServerConnection = null;
    try {
      mbeanServerConnection = proxy.getMBeanServerConnection();
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }

    statManager = (StatisticsManagerMBean)MBeanServerInvocationHandler
        .newProxyInstance(mbeanServerConnection, StatisticsMBeansNames.STATISTICS_MANAGER, StatisticsManagerMBean.class, false);
    statEmitter = (StatisticsEmitterMBean)MBeanServerInvocationHandler
        .newProxyInstance(mbeanServerConnection, StatisticsMBeansNames.STATISTICS_EMITTER, StatisticsEmitterMBean.class, false);

  }

  public void disconnectManager() throws TCStatisticsGathererException {
    try {
      statManager = null;
      statEmitter = null;
      proxy.close();
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  public String[] getSupportedStatistics() throws TCStatisticsGathererException {
    return new String[0];
  }

  public void enableStatistics(String[] names) throws TCStatisticsGathererException {
  }

  public void startCapturing() throws TCStatisticsGathererException {
  }

  public void stopCapturing() throws TCStatisticsGathererException {
  }

  private class StoreDataListener implements NotificationListener {
    public void handleNotification(Notification notification, Object o) {

      StatisticData data = (StatisticData)notification.getUserData();
      ((Collection)o).add(data);
    }
  }
}
