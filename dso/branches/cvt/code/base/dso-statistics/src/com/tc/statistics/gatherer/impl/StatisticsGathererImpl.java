/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.gatherer.impl;

import com.tc.exception.TCRuntimeException;
import com.tc.management.JMXConnectorProxy;
import com.tc.statistics.StatisticData;
import com.tc.statistics.beans.StatisticsEmitterMBean;
import com.tc.statistics.beans.StatisticsMBeansNames;
import com.tc.statistics.beans.StatisticsManagerMBean;
import com.tc.statistics.gatherer.StatisticsGatherer;
import com.tc.statistics.gatherer.exceptions.TCStatisticsGathererAlreadyConnectedException;
import com.tc.statistics.gatherer.exceptions.TCStatisticsGathererCloseSessionErrorException;
import com.tc.statistics.gatherer.exceptions.TCStatisticsGathererConnectionRequiredException;
import com.tc.statistics.gatherer.exceptions.TCStatisticsGathererException;
import com.tc.statistics.gatherer.exceptions.TCStatisticsGathererGlobalConfigGetErrorException;
import com.tc.statistics.gatherer.exceptions.TCStatisticsGathererGlobalConfigSetErrorException;
import com.tc.statistics.gatherer.exceptions.TCStatisticsGathererSessionConfigGetErrorException;
import com.tc.statistics.gatherer.exceptions.TCStatisticsGathererSessionConfigSetErrorException;
import com.tc.statistics.gatherer.exceptions.TCStatisticsGathererSessionCreationErrorException;
import com.tc.statistics.gatherer.exceptions.TCStatisticsGathererSessionRequiredException;
import com.tc.statistics.store.StatisticsStore;
import com.tc.statistics.store.exceptions.TCStatisticsStoreException;
import com.tc.util.Assert;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.Notification;
import javax.management.NotificationListener;

public class StatisticsGathererImpl implements StatisticsGatherer {
  private final StatisticsStore store;

  private JMXConnectorProxy proxy = null;
  private MBeanServerConnection mbeanServerConnection = null;
  private StatisticsManagerMBean statManager = null;
  private StatisticsEmitterMBean statEmitter = null;
  private Long sessionId = null;
  private StoreDataListener listener = null;

  public StatisticsGathererImpl(final StatisticsStore store) {
    Assert.assertNotNull("store can't be null", store);
    this.store = store;
  }

  public void connect(final String managerHostName, final int managerPort) throws TCStatisticsGathererException {
    if (statManager != null) throw new TCStatisticsGathererAlreadyConnectedException();
    
    try {
      store.open();
    } catch (TCStatisticsStoreException e) {
      throw new TCStatisticsGathererSessionCreationErrorException("Unexpected error while opening statistics store.", e);
    }

    proxy = new JMXConnectorProxy(managerHostName, managerPort);
    try {
      // create the server connection
      mbeanServerConnection = proxy.getMBeanServerConnection();
    } catch (Exception e) {
      throw new TCStatisticsGathererSessionCreationErrorException("Unexpected error while connecting to mbean server.", e);
    }

    // setup the mbeans
    statManager = (StatisticsManagerMBean)MBeanServerInvocationHandler
        .newProxyInstance(mbeanServerConnection, StatisticsMBeansNames.STATISTICS_MANAGER, StatisticsManagerMBean.class, false);

    statEmitter = (StatisticsEmitterMBean)MBeanServerInvocationHandler
        .newProxyInstance(mbeanServerConnection, StatisticsMBeansNames.STATISTICS_EMITTER, StatisticsEmitterMBean.class, false);

    // register the statistics data listener
    try {
      listener = new StoreDataListener();
      mbeanServerConnection.addNotificationListener(StatisticsMBeansNames.STATISTICS_EMITTER, listener, null, null);
    } catch (Exception e) {
      throw new TCStatisticsGathererSessionCreationErrorException("Unexpected error while registering the notification listener for statistics emitting.", e);
    }

    // enable the statistics envoy
    statEmitter.enable();
  }

  public void disconnect() throws TCStatisticsGathererException {
    if (null == statManager) throw new TCStatisticsGathererConnectionRequiredException();

    TCStatisticsGathererException exception = null;

    // make sure the session is closed
    try {
      closeSession();
    } catch (Exception e) {
      exception = new TCStatisticsGathererCloseSessionErrorException("Unexpected error while closing the capturing session '"+sessionId+"'.", e);
    }

    // disable the notification and detach the listener
    try {
      statEmitter.disable();
    } catch (Exception e) {
      TCStatisticsGathererException ex = new TCStatisticsGathererCloseSessionErrorException("Unexpected error while disabling the statistics emitter.", e);
      if (exception != null) {
        exception.setNextException(ex);
      } else {
        exception = ex;
      }
    }

    try {
      mbeanServerConnection.removeNotificationListener(StatisticsMBeansNames.STATISTICS_EMITTER, listener);
    } catch (Exception e) {
      TCStatisticsGathererException ex = new TCStatisticsGathererCloseSessionErrorException("Unexpected error while closing the JMX server connection.", e);
      if (exception != null) {
        exception.setNextException(ex);
      } else {
        exception = ex;
      }
    }

    try {
      proxy.close();
    } catch (Exception e) {
      TCStatisticsGathererException ex = new TCStatisticsGathererCloseSessionErrorException("Unexpected error while closing the JMX proxy.", e);
      if (exception != null) {
        exception.setNextException(ex);
      } else {
        exception = ex;
      }
    }

    try {
      store.close();
    } catch (TCStatisticsStoreException e) {
      TCStatisticsGathererException ex = new TCStatisticsGathererCloseSessionErrorException("Unexpected error while closing the statistics store.", e);
      if (exception != null) {
        exception.setNextException(ex);
      } else {
        exception = ex;
      }
    }

    proxy = null;
    listener = null;
    statEmitter = null;
    statManager = null;

    if (exception != null) {
      throw exception;
    }
  }

  public void createSession() throws TCStatisticsGathererException {
    if (null == statManager) throw new TCStatisticsGathererConnectionRequiredException();

    closeSession();

    // create a new capturing session
    sessionId = new Long(statManager.createCaptureSession());
  }

  public void closeSession() throws TCStatisticsGathererException {
    if (sessionId != null) {
      stopCapturing();
      sessionId = null;
    }
  }

  public String[] getSupportedStatistics() throws TCStatisticsGathererException {
    if (null == statManager) throw new TCStatisticsGathererConnectionRequiredException();
    return statManager.getSupportedStatistics();
  }

  public void enableStatistics(final String[] names) throws TCStatisticsGathererException {
    Assert.assertNotNull("names", names);

    if (null == sessionId) throw new TCStatisticsGathererSessionRequiredException();

    statManager.disableAllStatistics(sessionId.longValue());
    for (int i = 0; i < names.length; i++) {
      statManager.enableStatistic(sessionId.longValue(), names[i]);
    }
  }

  public void startCapturing() throws TCStatisticsGathererException {
    if (null == sessionId) throw new TCStatisticsGathererSessionRequiredException();
    statManager.startCapturing(sessionId.longValue());
  }

  public void stopCapturing() throws TCStatisticsGathererException {
    if (null == sessionId) throw new TCStatisticsGathererSessionRequiredException();
    statManager.stopCapturing(sessionId.longValue());
  }

  public void setGlobalParam(final String key, final Object value) throws TCStatisticsGathererException {
    if (null == statManager) throw new TCStatisticsGathererConnectionRequiredException();
    try {
      statManager.setGlobalParam(key, value);
    } catch (Exception e) {
      throw new TCStatisticsGathererGlobalConfigSetErrorException(key, value, e);
    }
  }

  public Object getGlobalParam(final String key) throws TCStatisticsGathererException {
    if (null == statManager) throw new TCStatisticsGathererConnectionRequiredException();
    try {
      return statManager.getGlobalParam(key);
    } catch (Exception e) {
      throw new TCStatisticsGathererGlobalConfigGetErrorException(key, e);
    }
  }

  public void setSessionParam(final String key, final Object value) throws TCStatisticsGathererException {
    if (null == sessionId) throw new TCStatisticsGathererSessionRequiredException();
    try {
      statManager.setSessionParam(sessionId.longValue(), key, value);
    } catch (Exception e) {
      throw new TCStatisticsGathererSessionConfigSetErrorException(sessionId.longValue(), key, value, e);
    }
  }

  public Object getSessionParam(final String key) throws TCStatisticsGathererException {
    if (null == sessionId) throw new TCStatisticsGathererSessionRequiredException();
    try {
      return statManager.getSessionParam(sessionId.longValue(), key);
    } catch (Exception e) {
      throw new TCStatisticsGathererSessionConfigGetErrorException(sessionId.longValue(), key, e);
    }
  }

  private class StoreDataListener implements NotificationListener {
    public void handleNotification(final Notification notification, final Object o) {
      StatisticData data = (StatisticData)notification.getUserData();
      if (sessionId != null &&
          data.getSessionId().equals(sessionId)) {
        try {
          store.storeStatistic(data);
        } catch (TCStatisticsStoreException e) {
          throw new TCRuntimeException(e);
        }
      }
    }
  }
}