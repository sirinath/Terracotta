/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics;

import com.tc.management.JMXConnectorProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.statistics.StatisticData;
import com.tc.statistics.beans.StatisticsEmitterMBean;
import com.tc.statistics.beans.StatisticsManagerMBean;
import com.tc.statistics.retrieval.actions.SRAShutdownTimestamp;
import com.tc.statistics.retrieval.actions.SRAStartupTimestamp;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.Notification;
import javax.management.NotificationListener;

public class StatisticsManagerTest extends TransparentTestBase {
  protected void duringRunningCluster() throws Exception {
    JMXConnectorProxy jmxc = new JMXConnectorProxy("localhost", getAdminPort());
    MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

    List data = new ArrayList();
    final boolean[] shutdown = new boolean[] {false};
    NotificationListener listener = new NotificationListener() {
      public void handleNotification(Notification notification, Object o) {
        StatisticData data = (StatisticData)notification.getUserData();
        ((List)o).add(data);
        if (SRAShutdownTimestamp.ACTION_NAME.equals(data.getName())) {
          shutdown[0] = true;
          synchronized (this) {
            this.notifyAll();
          }
        }
      }
    };

    StatisticsManagerMBean stat_manager = (StatisticsManagerMBean)MBeanServerInvocationHandler
        .newProxyInstance(mbsc, L2MBeanNames.STATISTICS_MANAGER, StatisticsManagerMBean.class, false);
    StatisticsEmitterMBean stat_emitter = (StatisticsEmitterMBean)MBeanServerInvocationHandler
        .newProxyInstance(mbsc, L2MBeanNames.STATISTICS_EMITTER, StatisticsEmitterMBean.class, false);
    mbsc.addNotificationListener(L2MBeanNames.STATISTICS_EMITTER, listener, null, data);
    stat_emitter.enable();

    long sessionid = stat_manager.createCaptureSession();

    // register all the supported statistics
    String[] statistics = stat_manager.getSupportedStatistics();
    for (int i = 0; i < statistics.length; i++) {
      stat_manager.enableStatistic(sessionid, statistics[i]);
    }

    // start capturing
    stat_manager.startCapturing(sessionid);

    // wait for 10 seconds
    Thread.sleep(10000);

    // stop capturing and wait for the last data
    synchronized (listener) {
      stat_manager.stopCapturing(sessionid);
      while (!shutdown[0]) {
        listener.wait(2000);
      }
    }

    // disable the notification and detach the listener
    stat_emitter.disable();
    mbsc.removeNotificationListener(L2MBeanNames.STATISTICS_EMITTER, listener);

    // check the data
    assertTrue(data.size() > 2);
    assertEquals(SRAStartupTimestamp.ACTION_NAME, ((StatisticData)data.get(0)).getName());
    assertEquals(SRAShutdownTimestamp.ACTION_NAME, ((StatisticData)data.get(data.size() - 1)).getName());
    Set received_data_names = new HashSet();
    for (int i = 1; i < data.size() - 1; i++) {
      StatisticData stat_data = (StatisticData)data.get(i);
      received_data_names.add(stat_data.getName());
    }
    // check that there's at least one data element name per registered statistic
    assertTrue(received_data_names.size() > statistics.length);
  }

  protected Class getApplicationClass() {
    return StatisticsManagerTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(StatisticsManagerTestApp.NODE_COUNT);
    t.initializeTestRunner();
  }
}