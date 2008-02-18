/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.beans.impl;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.management.AbstractTerracottaMBean;
import com.tc.statistics.StatisticsGateway;
import com.tc.statistics.beans.StatisticsGatewayMBean;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.management.MBeanServerConnection;
import javax.management.NotCompliantMBeanException;
import javax.management.MBeanNotificationInfo;
import javax.management.NotificationListener;
import javax.management.Notification;

public class StatisticsGatewayImpl extends AbstractTerracottaMBean implements StatisticsGatewayMBean, StatisticsGateway, NotificationListener {

  private final SynchronizedLong sequenceNumber = new SynchronizedLong(0L);

  private List agents = new CopyOnWriteArrayList();

  public StatisticsGatewayImpl() throws NotCompliantMBeanException {
    super(StatisticsGatewayMBean.class, true, false);
  }

  public MBeanNotificationInfo[] getNotificationInfo() {
    return StatisticsEmitterImpl.NOTIFICATION_INFO;
  }

  public synchronized void addStatisticsAgent(final MBeanServerConnection mbeanServerConnection) {
    StatisticsAgentConnection agent = new StatisticsAgentConnection();
    agent.connect(mbeanServerConnection, this);
    agents.add(agent);
  }

  public synchronized void cleanup() {
    List old_agents = agents;
    agents = new CopyOnWriteArrayList();

    Iterator it = old_agents.iterator();
    while (it.hasNext()) {
      ((StatisticsAgentConnection)it.next()).disconnect();
    }
  }

  protected synchronized void enabledStateChanged() {
    Iterator it = agents.iterator();
    while (it.hasNext()) {
      StatisticsAgentConnection agent = (StatisticsAgentConnection)it.next();
      if (isEnabled()) {
        agent.enable();
      } else {
        agent.disable();
      }
    }
  }

  public void reset() {
  }

  public String[] getSupportedStatistics() {
    Set combinedStats = new TreeSet();

    Iterator it = agents.iterator();
    while (it.hasNext()) {
      StatisticsAgentConnection agent = (StatisticsAgentConnection)it.next();
      String[] agentStats = agent.getSupportedStatistics();
      for (int i = 0; i < agentStats.length; i++) {
        combinedStats.add(agentStats[i]);
      }
    }

    String[] result = new String[combinedStats.size()];
    combinedStats.toArray(result);

    return result;
  }

  public void createSession(final String sessionId) {
    Iterator it = agents.iterator();
    while (it.hasNext()) {
      StatisticsAgentConnection agent = (StatisticsAgentConnection)it.next();
      agent.createSession(sessionId);
    }
  }

  public void disableAllStatistics(final String sessionId) {
    Iterator it = agents.iterator();
    while (it.hasNext()) {
      StatisticsAgentConnection agent = (StatisticsAgentConnection)it.next();
      agent.disableAllStatistics(sessionId);
    }
  }

  public boolean enableStatistic(final String sessionId, final String name) {
    boolean result = false;
    Iterator it = agents.iterator();
    while (it.hasNext()) {
      StatisticsAgentConnection agent = (StatisticsAgentConnection)it.next();
      if (agent.enableStatistic(sessionId, name)) {
        result = true;
      }
    }
    return result;
  }

  public void startCapturing(final String sessionId) {
    Iterator it = agents.iterator();
    while (it.hasNext()) {
      StatisticsAgentConnection agent = (StatisticsAgentConnection)it.next();
      agent.startCapturing(sessionId);
    }
  }

  public void stopCapturing(final String sessionId) {
    Iterator it = agents.iterator();
    while (it.hasNext()) {
      StatisticsAgentConnection agent = (StatisticsAgentConnection)it.next();
      agent.stopCapturing(sessionId);
    }
  }

  public void setGlobalParam(final String key, final Object value) {
    Iterator it = agents.iterator();
    while (it.hasNext()) {
      StatisticsAgentConnection agent = (StatisticsAgentConnection)it.next();
      agent.setGlobalParam(key, value);
    }
  }

  public Object getGlobalParam(final String key) {
    Iterator it = agents.iterator();
    while (it.hasNext()) {
      StatisticsAgentConnection agent = (StatisticsAgentConnection)it.next();
      if (agent.isServerAgent()) {
        return agent.getGlobalParam(key);
      }
    }

    throw new RuntimeException("Can't find server agent");
  }

  public void setSessionParam(final String sessionId, final String key, final Object value) {
    Iterator it = agents.iterator();
    while (it.hasNext()) {
      StatisticsAgentConnection agent = (StatisticsAgentConnection)it.next();
      agent.setSessionParam(sessionId, key, value);
    }
  }

  public Object getSessionParam(final String sessionId, final String key) {
    Iterator it = agents.iterator();
    while (it.hasNext()) {
      StatisticsAgentConnection agent = (StatisticsAgentConnection)it.next();
      if (agent.isServerAgent()) {
        return agent.getSessionParam(sessionId, key);
      }
    }

    throw new RuntimeException("Can't find server agent");
  }

  public void handleNotification(Notification notification, Object o) {
    notification.setSequenceNumber(sequenceNumber.increment());
    sendNotification(notification);
  }
}