/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.statistics;

import com.tc.management.JMXConnectorProxy;
import com.tc.statistics.StatisticData;
import com.tc.statistics.beans.StatisticsGatewayMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.statistics.retrieval.impl.StatisticsRetrieverImpl;
import com.tc.util.UUID;
import com.tc.util.runtime.ThreadDumpUtil;
import com.tctest.TransparentTestIface;

import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;

import junit.framework.Assert;

public class StatisticsRetrievalThreadsTest extends AbstractStatisticsTransparentTestBase {

  @Override
  protected Class getApplicationClass() {
    return StatisticsManagerNoActionsTestApp.class;
  }

  @Override
  public void doSetUp(final TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(StatisticsRetrievalThreadsTestApp.NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected void loadPostActions() {
    addPostAction(new StatisticsPostAction(this));
  }

  private static class StatisticsPostAction extends BaseStatisticsPostAction {

    public StatisticsPostAction(AbstractStatisticsTransparentTestBase test) {
      super(test);
    }

    @Override
    public void execute() throws Exception {
      JMXConnectorProxy jmxc = new JMXConnectorProxy("localhost", test.getAdminPort());
      MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

      StatisticsGatewayMBean stat_gateway = (StatisticsGatewayMBean) MBeanServerInvocationHandler
          .newProxyInstance(mbsc, StatisticsMBeanNames.STATISTICS_GATEWAY, StatisticsGatewayMBean.class, false);

      waitForAllNodesToConnectToGateway(stat_gateway, StatisticsRetrievalThreadsTestApp.NODE_COUNT + 1);

      List<StatisticData> data = new ArrayList<StatisticData>();
      CollectingNotificationListener listener = new CollectingNotificationListener(
                                                                                   StatisticsGatewayNoActionsTestApp.NODE_COUNT + 1);
      mbsc.addNotificationListener(StatisticsMBeanNames.STATISTICS_GATEWAY, listener, null, data);
      stat_gateway.enable();

      for (int i = 0; i < 5; i++) {
        String sessionid = UUID.getUUID().toString();
        stat_gateway.createSession(sessionid);

        // start capturing
        stat_gateway.startCapturing(sessionid);

        // stop capturing and wait for the last data
        synchronized (listener) {
          stat_gateway.stopCapturing(sessionid);
          while (!listener.getShutdown()) {
            listener.wait(2000);
          }
          listener.reset();
        }
      }

      String thread_dump = ThreadDumpUtil.getThreadDump();
      Assert.assertFalse(thread_dump.contains(StatisticsRetrieverImpl.TIMER_NAME));

      // disable the notification and detach the listener
      stat_gateway.disable();
      mbsc.removeNotificationListener(StatisticsMBeanNames.STATISTICS_GATEWAY, listener);
    }
  }
}