/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.statistics;

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.statistics.beans.StatisticsGatewayMBean;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.test.JMXUtils;
import com.tctest.TransparentTestBase;

import java.io.IOException;
import java.util.ArrayList;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

public abstract class AbstractStatisticsTransparentTestBase extends TransparentTestBase {

  @Override
  protected void setExtraJvmArgs(final ArrayList jvmArgs) {
    // REST interface is disabled out the box now (see DEV-6875)
    TCPropertiesImpl.getProperties().setProperty(TCPropertiesConsts.CVT_REST_INTERFACE_ENABLED, "true");
    System.setProperty(TCPropertiesImpl.tcSysProp(TCPropertiesConsts.CVT_REST_INTERFACE_ENABLED), "true");
    jvmArgs.add("-D" + TCPropertiesImpl.tcSysProp(TCPropertiesConsts.CVT_REST_INTERFACE_ENABLED) + "=true");
  }

  protected void waitForAllNodesToConnectToGateway(final int nodeCount) throws IOException, InterruptedException {
    final JMXConnector jmxc = JMXUtils.getJMXConnector("localhost", getAdminPort());
    final MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

    final StatisticsGatewayMBean stat_gateway = MBeanServerInvocationHandler
        .newProxyInstance(mbsc, StatisticsMBeanNames.STATISTICS_GATEWAY, StatisticsGatewayMBean.class, false);

    waitForAllNodesToConnectToGateway(stat_gateway, nodeCount);
  }

  protected void waitForAllNodesToConnectToGateway(final StatisticsGatewayMBean statGateway, final int nodeCount)
      throws InterruptedException {
    int currentNodeCount;
    Thread.sleep(2000);
    while ((currentNodeCount = statGateway.getConnectedAgentChannelIDs().length) < nodeCount) {
      Thread.sleep(500);
      System.out.println("Currently " + currentNodeCount + " nodes connected to gateway, waiting for " + nodeCount);
    }
  }
}
