/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.gatherer.impl;

import com.tc.management.JMXConnectorProxy;
import com.tc.statistics.beans.StatisticsGatewayMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
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

public class StatisticsGathererImpl implements StatisticsGatherer {
  private final StatisticsStore store;

  private JMXConnectorProxy proxy = null;
  private MBeanServerConnection mbeanServerConnection = null;
  private StatisticsGatewayMBean statGateway = null;
  private String sessionId = null;
  private StoreDataListener listener = null;

  public StatisticsGathererImpl(final StatisticsStore store) {
    Assert.assertNotNull("store can't be null", store);
    this.store = store;
  }

  public void connect(final String managerHostName, final int managerPort) throws TCStatisticsGathererException {
    if (statGateway != null) throw new TCStatisticsGathererAlreadyConnectedException();
    
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
    statGateway = (StatisticsGatewayMBean)MBeanServerInvocationHandler
        .newProxyInstance(mbeanServerConnection, StatisticsMBeanNames.STATISTICS_GATEWAY, StatisticsGatewayMBean.class, false);

    // enable the statistics envoy
    statGateway.enable();
  }

  public void disconnect() throws TCStatisticsGathererException {
    if (null == statGateway) throw new TCStatisticsGathererConnectionRequiredException();

    TCStatisticsGathererException exception = null;

    // make sure the session is closed
    try {
      closeSession();
    } catch (Exception e) {
      exception = new TCStatisticsGathererCloseSessionErrorException("Unexpected error while closing the capturing session '"+sessionId+"'.", e);
    }

    // disable the notification
    try {
      statGateway.disable();
    } catch (Exception e) {
      TCStatisticsGathererException ex = new TCStatisticsGathererCloseSessionErrorException("Unexpected error while disabling the statistics gateway.", e);
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
    statGateway = null;

    if (exception != null) {
      throw exception;
    }
  }

  public void createSession(final String sessionId) throws TCStatisticsGathererException {
    if (null == statGateway) throw new TCStatisticsGathererConnectionRequiredException();

    closeSession();

    // create a new capturing session
    statGateway.createSession(sessionId);
    this.sessionId = sessionId;

    // register the statistics data listener
    try {
      listener = new StoreDataListener();
      mbeanServerConnection.addNotificationListener(StatisticsMBeanNames.STATISTICS_GATEWAY, listener, new SessionBoundNotificationFilter(sessionId), store);
    } catch (Exception e) {
      throw new TCStatisticsGathererSessionCreationErrorException("Unexpected error while registering the notification listener for statistics emitting.", e);
    }
  }

  public void closeSession() throws TCStatisticsGathererException {
    if (sessionId != null) {
      stopCapturing();
      sessionId = null;

      // detach the notification listener
      try {
        mbeanServerConnection.removeNotificationListener(StatisticsMBeanNames.STATISTICS_GATEWAY, listener);
      } catch (Exception e) {
        throw new TCStatisticsGathererCloseSessionErrorException("Unexpected error while removing the statistics gateway notification listener.", e);
      }
    }
  }

  public String[] getSupportedStatistics() throws TCStatisticsGathererException {
    if (null == statGateway) throw new TCStatisticsGathererConnectionRequiredException();
    return statGateway.getSupportedStatistics();
  }

  public void enableStatistics(final String[] names) throws TCStatisticsGathererException {
    Assert.assertNotNull("names", names);

    if (null == sessionId) throw new TCStatisticsGathererSessionRequiredException();

    statGateway.disableAllStatistics(sessionId);
    for (int i = 0; i < names.length; i++) {
      statGateway.enableStatistic(sessionId, names[i]);
    }
  }

  public void startCapturing() throws TCStatisticsGathererException {
    if (null == sessionId) throw new TCStatisticsGathererSessionRequiredException();
    statGateway.startCapturing(sessionId);
  }

  public void stopCapturing() throws TCStatisticsGathererException {
    if (null == sessionId) throw new TCStatisticsGathererSessionRequiredException();
    statGateway.stopCapturing(sessionId);
  }

  public void setGlobalParam(final String key, final Object value) throws TCStatisticsGathererException {
    if (null == statGateway) throw new TCStatisticsGathererConnectionRequiredException();
    try {
      statGateway.setGlobalParam(key, value);
    } catch (Exception e) {
      throw new TCStatisticsGathererGlobalConfigSetErrorException(key, value, e);
    }
  }

  public Object getGlobalParam(final String key) throws TCStatisticsGathererException {
    if (null == statGateway) throw new TCStatisticsGathererConnectionRequiredException();
    try {
      return statGateway.getGlobalParam(key);
    } catch (Exception e) {
      throw new TCStatisticsGathererGlobalConfigGetErrorException(key, e);
    }
  }

  public void setSessionParam(final String key, final Object value) throws TCStatisticsGathererException {
    if (null == sessionId) throw new TCStatisticsGathererSessionRequiredException();
    try {
      statGateway.setSessionParam(sessionId, key, value);
    } catch (Exception e) {
      throw new TCStatisticsGathererSessionConfigSetErrorException(sessionId, key, value, e);
    }
  }

  public Object getSessionParam(final String key) throws TCStatisticsGathererException {
    if (null == sessionId) throw new TCStatisticsGathererSessionRequiredException();
    try {
      return statGateway.getSessionParam(sessionId, key);
    } catch (Exception e) {
      throw new TCStatisticsGathererSessionConfigGetErrorException(sessionId, key, e);
    }
  }
}