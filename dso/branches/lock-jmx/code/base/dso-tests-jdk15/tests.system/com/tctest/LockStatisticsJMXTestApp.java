/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.management.JMXConnectorProxy;
import com.tc.management.L2LockStatsManagerImpl.LockStat;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.LockStatisticsMonitorMBean;
import com.tc.object.LiteralValues;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CyclicBarrier;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

public class LockStatisticsJMXTestApp extends AbstractTransparentApp {
  private static final LiteralValues LITERAL_VALUES = new LiteralValues();

  public static final String         CONFIG_FILE      = "config-file";
  public static final String         PORT_NUMBER      = "port-number";
  public static final String         HOST_NAME        = "host-name";
  public static final String         JMX_PORT         = "jmx-port";

  private final ApplicationConfig    config;

  private final int                  initialNodeCount = getParticipantCount();
  private final CyclicBarrier        barrier          = new CyclicBarrier(initialNodeCount);
  private final CyclicBarrier        barrier2          = new CyclicBarrier(2);

  private MBeanServerConnection      mbsc             = null;
  private JMXConnector               jmxc;
  private LockStatisticsMonitorMBean statMBean;

  public LockStatisticsJMXTestApp(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
    this.config = config;
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {

    String testClass = LockStatisticsJMXTestApp.class.getName();
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "$*");

    // roots
    spec.addRoot("barrier", "barrier");
    spec.addRoot("barrier2", "barrier2");
  }

  public void run() {
    try {
      int index = barrier.await();
      String lockName = "lock1";

      testBasicStatistics(lockName, index);
      
      lockName = "lock2";
      
      enableStackTraces(lockName, index);
      
      testStackTracesStatistics(lockName, index);
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void connect() throws Exception {
    echo("connecting to jmx server....");
    jmxc = new JMXConnectorProxy("localhost", Integer.parseInt(config.getAttribute(JMX_PORT)));
    mbsc = jmxc.getMBeanServerConnection();
    echo("obtained mbeanserver connection");
    statMBean = (LockStatisticsMonitorMBean) MBeanServerInvocationHandler
        .newProxyInstance(mbsc, L2MBeanNames.LOCK_STATISTICS, LockStatisticsMonitorMBean.class, false);
  }

  private void disconnect() throws Exception {
    if (jmxc != null) {
      jmxc.close();
    }
  }
  
  private void enableStackTraces(String lockName, int index) throws Throwable {
    if (index == 0) {
      connect();
      statMBean.enableClientStat(ByteCodeUtil.generateLiteralLockName(LITERAL_VALUES.valueFor(lockName), lockName));
      disconnect();
    }
    
    barrier.await();
  }
  
  private void testStackTracesStatistics(String lockName, int index) throws Throwable {
    if (index == 0) {
      waitForAllToMoveOn();
      connect();
      Thread.sleep(2000);
      verifyStackTraces(ByteCodeUtil.generateLiteralLockName(LITERAL_VALUES.valueFor(lockName), lockName), 2);
      disconnect();
    } else {
      TCProperties tcProperties = TCPropertiesImpl.getProperties().getPropertiesFor("l1.lock.stacktrace");
      int batch = tcProperties.getInt("batch");
      
      for (int i=0; i<batch; i++) {
        ManagerUtil.monitorEnter(lockName, LockLevel.READ);
      }
      for (int i=0; i<batch; i++) {
        ManagerUtil.monitorExit(lockName);
      }
      
      waitForAllToMoveOn();
    }
    waitForAllToMoveOn();
  }

  private void testBasicStatistics(String lockName, int index) throws Throwable {
    if (index == 0) {
      connect();
      waitForAllToMoveOn();

      verifyLockRequest(lockName, 1);
      waitForAllToMoveOn();
      
      Thread.sleep(1000);
      verifyLockContented(lockName, 1);
      waitForTwoToMoveOn();

      waitForAllToMoveOn();
      verifyLockRequest(lockName, 2);
      verifyLockContented(lockName, 0);
      
      waitForAllToMoveOn();
      
      waitForAllToMoveOn();
      
      verifyLockHop(lockName, 2);
      verifyStackTraces(lockName, 0);
      waitForAllToMoveOn();
      disconnect();
      
    } else if (index == 1) {
      ManagerUtil.monitorEnter(lockName, LockLevel.WRITE);
      waitForAllToMoveOn();
      waitForAllToMoveOn();
      
      waitForTwoToMoveOn();
      ManagerUtil.monitorExit(lockName);
      
      waitForAllToMoveOn();
      
      waitForAllToMoveOn();
      
      ManagerUtil.monitorEnter(lockName, LockLevel.WRITE);
      waitForTwoToMoveOn();
      Thread.sleep(1000);
      ManagerUtil.monitorExit(lockName);
      waitForTwoToMoveOn();
      waitForAllToMoveOn();
      waitForAllToMoveOn();
    } else if (index == 2) {
      waitForAllToMoveOn();
      waitForAllToMoveOn();
      ManagerUtil.monitorEnter(lockName, LockLevel.WRITE);
      
      waitForAllToMoveOn();
      
      waitForAllToMoveOn();
      
      Thread.sleep(1000);
      ManagerUtil.monitorExit(lockName);
      waitForTwoToMoveOn();
      ManagerUtil.monitorEnter(lockName, LockLevel.WRITE);
      waitForTwoToMoveOn();
      waitForAllToMoveOn();
      waitForAllToMoveOn();
      ManagerUtil.monitorExit(lockName);
    }

    waitForAllToMoveOn();
  }
  
  private void waitForAllToMoveOn() throws Exception {
    barrier.await();
  }
  
  private void waitForTwoToMoveOn() throws Exception {
    barrier2.await();
  }

  private void verifyLockRequest(String lockName, int expectedValue) {
    Collection c = statMBean.getTopRequested(10);
    for (Iterator<LockStat> i = c.iterator(); i.hasNext();) {
      LockStat s = i.next();
      if (s.getLockID().asString().endsWith(lockName)) {
        Assert.assertEquals(expectedValue, s.getNumOfLockRequested());
        break;
      }
    }
  }
  
  private void verifyLockContented(String lockName, int expectedValue) {
    Collection c = statMBean.getTopContentedLocks(10);
    for (Iterator<LockStat> i = c.iterator(); i.hasNext();) {
      LockStat s = i.next();
      if (s.getLockID().asString().endsWith(lockName)) {
        Assert.assertEquals(expectedValue, s.getNumOfPendingRequests());
        break;
      }
    }
  }
  
  private void verifyLockHop(String lockName, int expectedValue) {
    Collection c = statMBean.getTopLockHops(10);
    for (Iterator<LockStat> i = c.iterator(); i.hasNext();) {
      LockStat s = i.next();
      if (s.getLockID().asString().endsWith(lockName)) {
        Assert.assertEquals(expectedValue, s.getNumOfPingPongRequests());
        break;
      }
    }
  }
  
  private void verifyStackTraces(String lockName, int expectedValue) {
    Collection c = statMBean.getStackTraces(lockName);
    Assert.assertEquals(expectedValue, c.size());
  }

  private static void echo(String msg) {
    System.out.println(msg);
  }

}
