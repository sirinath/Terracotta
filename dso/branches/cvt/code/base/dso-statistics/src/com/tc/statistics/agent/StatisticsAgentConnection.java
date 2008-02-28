/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.agent;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticsManager;
import com.tc.statistics.agent.exceptions.TCStatisticsAgentConnectionAlreadyConnectedException;
import com.tc.statistics.agent.exceptions.TCStatisticsAgentConnectionConnectErrorException;
import com.tc.statistics.agent.exceptions.TCStatisticsAgentConnectionDisconnectErrorException;
import com.tc.statistics.agent.exceptions.TCStatisticsAgentConnectionException;
import com.tc.statistics.agent.exceptions.TCStatisticsAgentConnectionToNonAgentException;
import com.tc.statistics.beans.StatisticsEmitterMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.statistics.beans.StatisticsManagerMBean;
import com.tc.statistics.beans.exceptions.UnknownStatisticsSessionIdException;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.RuntimeMBeanException;

public class StatisticsAgentConnection implements StatisticsManager {
  private final static TCLogger logger        = CustomerLogging.getDSOGenericLogger();
  private final static TCLogger consoleLogger = CustomerLogging.getConsoleLogger();

  private boolean isServerAgent = false;
  private MBeanServerConnection serverConnection = null;
  private StatisticsManagerMBean statManager = null;
  private StatisticsEmitterMBean statEmitter = null;
  private NotificationListener listener = null;

  public synchronized void enable() {
    statManager.enable();
    statEmitter.enable();
  }

  public synchronized void disable() {
    statManager.disable();
    statEmitter.disable();
  }

  public boolean isServerAgent() {
    return isServerAgent;
  }

  public synchronized void reinitialize() {
    synchronized (statEmitter) {
      boolean was_emitter_enabled = statEmitter.isEnabled();

      statEmitter.disable();

      statManager.reinitialize();

      if (was_emitter_enabled) {
        statEmitter.enable();
      }
    }
  }

  public String[] getSupportedStatistics() {
    return statManager.getSupportedStatistics();
  }

  public void createSession(final String sessionId) {
    statManager.createSession(sessionId);
  }

  public void disableAllStatistics(final String sessionId) {
    try {
      statManager.disableAllStatistics(sessionId);
    } catch (RuntimeMBeanException e) {
      if (e.getCause() instanceof UnknownStatisticsSessionIdException) {
        UnknownStatisticsSessionIdException ussie = (UnknownStatisticsSessionIdException)e.getCause();
        String msg = "Unable to disable the statistics for session '"+ussie.getSessionId()+"' on node '"+ussie.getNodeName()+"'";
        logger.warn(msg);
        consoleLogger.warn(msg);
      } else {
        throw e;
      }
    }
  }

  public boolean enableStatistic(final String sessionId, final String name) {
    try {
      return statManager.enableStatistic(sessionId, name);
    } catch (RuntimeMBeanException e) {
      if (e.getCause() instanceof UnknownStatisticsSessionIdException) {
        UnknownStatisticsSessionIdException ussie = (UnknownStatisticsSessionIdException)e.getCause();
        String msg = "Unable to enable the statistic '"+name+"' for session '"+ussie.getSessionId()+"' on node '"+ussie.getNodeName()+"'";
        logger.warn(msg);
        consoleLogger.warn(msg);
        return false;
      } else {
        throw e;
      }
    }
  }

  public StatisticData[] captureStatistic(final String sessionId, final String name) {
    try {
      return statManager.captureStatistic(sessionId, name);
    } catch (RuntimeMBeanException e) {
      if (e.getCause() instanceof UnknownStatisticsSessionIdException) {
        UnknownStatisticsSessionIdException ussie = (UnknownStatisticsSessionIdException)e.getCause();
        String msg = "Unable to capture the statistic '"+name+"' for session '"+ussie.getSessionId()+"' on node '"+ussie.getNodeName()+"'";
        logger.warn(msg);
        consoleLogger.warn(msg);
        return null;
      } else {
        throw e;
      }
    }
  }

  public void startCapturing(final String sessionId) {
    try {
      statManager.startCapturing(sessionId);
    } catch (RuntimeMBeanException e) {
      if (e.getCause() instanceof UnknownStatisticsSessionIdException) {
        UnknownStatisticsSessionIdException ussie = (UnknownStatisticsSessionIdException)e.getCause();
        String msg = "Unable to start capturing for statistics session '"+ussie.getSessionId()+"' on node '"+ussie.getNodeName()+"'";
        logger.warn(msg);
        consoleLogger.warn(msg);
      } else {
        throw e;
      }
    }
  }

  public void stopCapturing(final String sessionId) {
    try {
      statManager.stopCapturing(sessionId);
    } catch (RuntimeMBeanException e) {
      if (e.getCause() instanceof UnknownStatisticsSessionIdException) {
        UnknownStatisticsSessionIdException ussie = (UnknownStatisticsSessionIdException)e.getCause();
        String msg = "Unable to stop capturing for statistics session '"+ussie.getSessionId()+"' on node '"+ussie.getNodeName()+"'";
        logger.warn(msg);
        consoleLogger.warn(msg);
      } else {
        throw e;
      }
    }
  }

  public void setGlobalParam(final String key, final Object value) {
    statManager.setGlobalParam(key, value);
  }

  public Object getGlobalParam(final String key) {
    return statManager.getGlobalParam(key);
  }

  public void setSessionParam(final String sessionId, final String key, final Object value) {
    try {
      statManager.setSessionParam(sessionId, key, value);
    } catch (RuntimeMBeanException e) {
      if (e.getCause() instanceof UnknownStatisticsSessionIdException) {
        UnknownStatisticsSessionIdException ussie = (UnknownStatisticsSessionIdException)e.getCause();
        String msg = "Unable to set the parameter '"+key+"' to '"+value+"' for session '"+ussie.getSessionId()+"' on node '"+ussie.getNodeName()+"'";
        logger.warn(msg);
        consoleLogger.warn(msg);
      } else {
        throw e;
      }
    }
  }

  public Object getSessionParam(final String sessionId, final String key) {
    try {
      return statManager.getSessionParam(sessionId, key);
    } catch (RuntimeMBeanException e) {
      if (e.getCause() instanceof UnknownStatisticsSessionIdException) {
        UnknownStatisticsSessionIdException ussie = (UnknownStatisticsSessionIdException)e.getCause();
        String msg = "Unable to get the session parameter '"+key+"' for session '"+ussie.getSessionId()+"' on node '"+ussie.getNodeName()+"'";
        logger.warn(msg);
        consoleLogger.warn(msg);
        return null;
      } else {
        throw e;
      }
    }
  }

  public void connect(final MBeanServerConnection serverConnection, final NotificationListener listener) throws TCStatisticsAgentConnectionException {
    Assert.assertNotNull("serverConnection", serverConnection);
    if (statManager != null) throw new TCStatisticsAgentConnectionAlreadyConnectedException();

    this.serverConnection = serverConnection;
    setupManagerMBean(serverConnection);
    ObjectName emitter_name = setupEmitterMBean(serverConnection);
    
    // register the statistics data listener
    this.listener = listener;
    try {
      serverConnection.addNotificationListener(emitter_name, listener, null, null);
    } catch (Exception e) {
      throw new TCStatisticsAgentConnectionConnectErrorException("Unexpected error while registering the notification listener for statistics emitting.", e);
    }
  }

  private ObjectName setupManagerMBean(MBeanServerConnection mbeanServerConnection) throws TCStatisticsAgentConnectionException {
    // setup the statistics manager mbean
    try {
      ObjectName manager_name = findMBeanName(mbeanServerConnection, StatisticsMBeanNames.STATISTICS_MANAGER);
      if (null == manager_name) {
        throw new TCStatisticsAgentConnectionToNonAgentException();
      }

      statManager = (StatisticsManagerMBean)MBeanServerInvocationHandler
        .newProxyInstance(mbeanServerConnection, manager_name, StatisticsManagerMBean.class, false);

      return manager_name;
    } catch (Exception e) {
      throw new TCStatisticsAgentConnectionConnectErrorException("Unexpected error while finding the manager mbean of the agent.", e);
    }
  }

  private ObjectName setupEmitterMBean(MBeanServerConnection mbeanServerConnection) throws TCStatisticsAgentConnectionException {
    // setup the statistics emitter mbean
    try {
      ObjectName emitter_name = findMBeanName(mbeanServerConnection, StatisticsMBeanNames.STATISTICS_EMITTER);
      if (null == emitter_name) {
        throw new TCStatisticsAgentConnectionToNonAgentException();
      }

      statEmitter = (StatisticsEmitterMBean)MBeanServerInvocationHandler
        .newProxyInstance(mbeanServerConnection, emitter_name, StatisticsEmitterMBean.class, false);

      return emitter_name;
    } catch (Exception e) {
      throw new TCStatisticsAgentConnectionException("Unexpected error while finding the emitter mbean of the agent.", e);
    }
  }

  private ObjectName findMBeanName(MBeanServerConnection mbeanServerConnection, ObjectName baseName) throws IOException, MalformedObjectNameException {
    ObjectName manager_name = null;

    // try to find the statistics manager, by first using the standard name and if that doesn't yield any
    // result by doing a search that includes at least all the properties of the standard name
    Set manager_names = mbeanServerConnection.queryNames(baseName, null);
    if (manager_names.size() > 0) {
      manager_name = (ObjectName)manager_names.iterator().next();
    } else {
      manager_names = mbeanServerConnection.queryNames(new ObjectName(baseName.getCanonicalName() + ",*"), null);
      if (manager_names.size() > 0) {
        manager_name = (ObjectName)manager_names.iterator().next();
      }
    }
    return manager_name;
  }

  public void disconnect() throws TCStatisticsAgentConnectionException {
    if (null == statManager) throw new AssertionError("the agent is not connected");

    try {
      serverConnection.removeNotificationListener(StatisticsMBeanNames.STATISTICS_EMITTER, listener);
    } catch (Exception e) {
      throw new TCStatisticsAgentConnectionDisconnectErrorException("Unexpected error while removing the notification listener for statistics emitting.", e);
    } finally {
      listener = null;
      statEmitter = null;
      statManager = null;
    }
  }
}