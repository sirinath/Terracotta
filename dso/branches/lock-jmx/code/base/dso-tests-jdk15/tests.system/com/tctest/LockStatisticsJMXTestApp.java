/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.management.JMXConnectorProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.LockStatisticsMonitorMBean;
import com.tc.management.lock.stats.LockSpec;
import com.tc.management.lock.stats.LockStatElement;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.CyclicBarrier;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

public class LockStatisticsJMXTestApp extends AbstractTransparentApp {
  private static final LiteralValues   LITERAL_VALUES   = new LiteralValues();

  public static final String           CONFIG_FILE      = "config-file";
  public static final String           PORT_NUMBER      = "port-number";
  public static final String           HOST_NAME        = "host-name";
  public static final String           JMX_PORT         = "jmx-port";

  private final ApplicationConfig      config;

  private final int                    initialNodeCount = getParticipantCount();
  private final CyclicBarrier          barrier          = new CyclicBarrier(initialNodeCount);
  private final CyclicBarrier          barrier2         = new CyclicBarrier(2);
  private final HashMap<Integer, Long> indexToNodeMap   = new HashMap();

  private MBeanServerConnection        mbsc             = null;
  private JMXConnector                 jmxc;
  private LockStatisticsMonitorMBean   statMBean;

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
    spec.addRoot("indexToNodeMap", "indexToNodeMap");
  }

  public void run() {
    try {
      int index = barrier.await();

      synchronized (indexToNodeMap) {
        indexToNodeMap.put(new Integer(index), new Long(ManagerUtil.getClientID()));
      }
//      String lockName = "lock0";
//      
//      testLockAggregateWaitTime(lockName, index);
//      
//      lockName = "lock1";
//
//      testBasicStatistics(lockName, index);

      String lockName = "lock2";

      enableStackTraces(lockName, index, 2, 1);

      testCollectClientStatistics(lockName, index, 1);
      
//      enableStackTraces(lockName, index, 2, getClientLockStatCollectionFrequency());
//
//      testStackTracesStatistics(lockName, index, 2);
//      
//      testLockHeldTime("lock3", "lock4", index);
//      
//      testLockWaitingTime("lock5", "lock6", index);
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

  private void enableStackTraces(String lockName, int index, int traceDepth, int gatherInterval ) throws Throwable {
    if (index == 0) {
      connect();
      statMBean.setLockStatisticsConfig(traceDepth, gatherInterval);
      disconnect();
    }

    barrier.await();
  }
  
  private int getClientLockStatCollectionFrequency() {
    TCProperties tcProperties = TCPropertiesImpl.getProperties().getPropertiesFor("l1.lock");
    return tcProperties.getInt("collectFrequency");
  }

  private void testCollectClientStatistics(String lockName, int index, int traceDepth) throws Throwable {
    if (index == 0) {
      waitForAllToMoveOn();
      connect();
      Thread.sleep(2000);
      verifyClientStat(ByteCodeUtil.generateLiteralLockName(LITERAL_VALUES.valueFor(lockName), lockName), 1,
                        traceDepth);
      disconnect();
    } else {
      //int clientLockStatCollectFrequency = getClientLockStatCollectionFrequency();
      int clientLockStatCollectFrequency = 2;
      for (int i = 0; i < clientLockStatCollectFrequency; i++) {
        ManagerUtil.monitorEnter(lockName, LockLevel.READ);
      }
      for (int i = 0; i < clientLockStatCollectFrequency; i++) {
        ManagerUtil.monitorExit(lockName);
      }

      waitForAllToMoveOn();
    }
    waitForAllToMoveOn();
  }
  
  private void testLockWaitingTime(String lockName1, String lockName2, int index) throws Throwable {
    if (index == 0) {
      connect();
      waitForAllToMoveOn();
      long waitTime1 = getLockWaitTime(lockName1, indexToNodeMap.get(new Integer(2)).longValue());
      long waitTime2 = getLockWaitTime(lockName2, indexToNodeMap.get(new Integer(2)).longValue());
      
      Assert.assertTrue(waitTime2 > waitTime1);
    } else if (index == 1) {
      ManagerUtil.monitorEnter(lockName1, LockLevel.WRITE);
      waitForTwoToMoveOn();
      Thread.sleep(2000);
      ManagerUtil.monitorExit(lockName1);
      
      ManagerUtil.monitorEnter(lockName2, LockLevel.WRITE);
      waitForTwoToMoveOn();
      Thread.sleep(4000);
      ManagerUtil.monitorExit(lockName2);
      waitForAllToMoveOn();
    } else if (index == 2) {
      waitForTwoToMoveOn();
      ManagerUtil.monitorEnter(lockName1, LockLevel.WRITE);
      ManagerUtil.monitorExit(lockName1);
      waitForTwoToMoveOn();
      ManagerUtil.monitorEnter(lockName2, LockLevel.WRITE);
      ManagerUtil.monitorExit(lockName2);
      waitForAllToMoveOn();
    }
    
    waitForAllToMoveOn();
  }
  
  private void testLockAggregateWaitTime(String lockName, int index) throws Throwable {
    if (index == 0) {
      connect();
      waitForAllToMoveOn();
      long avgWaitTimeInMillis = getAggregateAverageWaitTime(lockName);
      long avgHeldTimeInMillis = getAggregateAverageHeldTime(lockName);
      
      System.out.println("avgHeldTimeInMillis: " + avgHeldTimeInMillis);
      System.out.println("avgWaitTimeInMillis: " + avgWaitTimeInMillis);
      Assert.assertTrue(avgWaitTimeInMillis > 1000);
      Assert.assertTrue(avgHeldTimeInMillis > 2000);
    } else if (index == 1) {
      ManagerUtil.monitorEnter(lockName, LockLevel.WRITE);
      waitForTwoToMoveOn();
      Thread.sleep(2000);
      ManagerUtil.monitorExit(lockName);
      waitForTwoToMoveOn();
      ManagerUtil.monitorEnter(lockName, LockLevel.WRITE);
      Thread.sleep(3000);
      ManagerUtil.monitorExit(lockName);
      waitForAllToMoveOn();
    } else if (index == 2) {
      waitForTwoToMoveOn();
      ManagerUtil.monitorEnter(lockName, LockLevel.WRITE);
      waitForTwoToMoveOn();
      Thread.sleep(2000);
      ManagerUtil.monitorExit(lockName);
      waitForAllToMoveOn();
    }
    waitForAllToMoveOn();
  }
  
  private void testLockHeldTime(String lockName1, String lockName2, int index) throws Throwable {
    if (index == 0) {
      connect();
      waitForAllToMoveOn();
      long heldTime1 = getLockHeldTime(lockName1, indexToNodeMap.get(new Integer(1)).longValue());
      long heldTime2 = getLockHeldTime(lockName2, indexToNodeMap.get(new Integer(1)).longValue());
      
      Assert.assertTrue(heldTime2 > heldTime1);
    } else if (index == 1) {
      ManagerUtil.monitorEnter(lockName1, LockLevel.WRITE);
      waitForTwoToMoveOn();
      Thread.sleep(2000);
      ManagerUtil.monitorExit(lockName1);
      
      ManagerUtil.monitorEnter(lockName2, LockLevel.WRITE);
      waitForTwoToMoveOn();
      Thread.sleep(4000);
      ManagerUtil.monitorExit(lockName2);
      waitForAllToMoveOn();
    } else if (index == 2) {
      waitForTwoToMoveOn();
      ManagerUtil.monitorEnter(lockName1, LockLevel.WRITE);
      ManagerUtil.monitorExit(lockName1);
      waitForTwoToMoveOn();
      ManagerUtil.monitorEnter(lockName2, LockLevel.WRITE);
      ManagerUtil.monitorExit(lockName2);
      waitForAllToMoveOn();
    }
    
    waitForAllToMoveOn();
  }

  private void testBasicStatistics(String lockName, int index) throws Throwable {
    if (index == 0) {
      connect();
      waitForAllToMoveOn();

      verifyLockRequest(lockName, 1);
      verifyLockHolder(lockName, indexToNodeMap.get(new Integer(1)).longValue());
      verifyLockAwarded(lockName, indexToNodeMap.get(new Integer(1)).longValue(), true);
      waitForAllToMoveOn();
      
      Thread.sleep(1000);
      verifyLockContended(lockName, 1);
      verifyLockHolder(lockName, indexToNodeMap.get(new Integer(2)).longValue());
      verifyLockAwarded(lockName, indexToNodeMap.get(new Integer(2)).longValue(), false);
      waitForTwoToMoveOn();

      waitForAllToMoveOn();
      verifyLockRequest(lockName, 2);
      verifyLockContended(lockName, 0);
      verifyLockHolder(lockName, indexToNodeMap.get(new Integer(1)).longValue());
      verifyLockHolder(lockName, indexToNodeMap.get(new Integer(2)).longValue());
      verifyLockAwarded(lockName, indexToNodeMap.get(new Integer(2)).longValue(), true);
      
      waitForAllToMoveOn();
      
      waitForAllToMoveOn();
      
      verifyLockHop(lockName, 3);
      verifyClientStat(lockName, -1, -1);
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
//    Collection c = statMBean.getTopRequestedLocks(10);
//    for (Iterator<LockStat> i = c.iterator(); i.hasNext();) {
//      LockStat s = i.next();
//      if (s.getLockID().asString().endsWith(lockName)) {
//        Assert.assertEquals(expectedValue, s.getNumOfLockRequested());
//        break;
//      }
//    }
  }

  private void verifyLockHolder(String lockName, long expectedValue) {
//    Collection c = statMBean.getTopHeld(100);
//    for (Iterator<LockHolder> i = c.iterator(); i.hasNext();) {
//      LockHolder s = i.next();
//      if (s.getLockID().asString().endsWith(lockName)) {
//        if (((ClientID) s.getNodeID()).getChannelID().toLong() == expectedValue) { return; }
//      }
//    }
//    throw new AssertionError("Client " + expectedValue + " does not seem to hold lock " + lockName);
  }
  
  private void verifyLockAwarded(String lockName, long expectedValue, boolean isAwarded) {
//    Collection c = statMBean.getTopHeld(100);
//    for (Iterator<LockHolder> i = c.iterator(); i.hasNext();) {
//      LockHolder s = i.next();
//      if (s.getLockID().asString().endsWith(lockName)) {
//        if (((ClientID) s.getNodeID()).getChannelID().toLong() == expectedValue) {
//          if (isAwarded && s.getTimeAcquired() > 0) {
//            return;
//          }
//          if (!isAwarded && s.getTimeAcquired() > 0) {
//            throw new AssertionError("Client " + expectedValue + " should not have acquire the lock " + lockName);
//          }
//        }
//      }
//    }
//    
//    if (isAwarded) {
//      throw new AssertionError("Client " + expectedValue + " does not seem to acquire the lock " + lockName);
//    }
  }
  
  private long getAggregateAverageHeldTime(String lockName) {
//    Collection c = statMBean.getTopAvgHeldingLocks(500);
//    for (Iterator<LockStat> i=c.iterator(); i.hasNext(); ) {
//      LockStat s = i.next();
//      if (s.getLockID().asString().endsWith(lockName)) {
//        return s.getAvgHeldTimeInMillis();
//      }
//    }
    return -1;
  }
  
  private long getAggregateAverageWaitTime(String lockName) {
//    Collection c = statMBean.getTopAvgWaitingLocks(500);
//    for (Iterator<LockStat> i=c.iterator(); i.hasNext(); ) {
//      LockStat s = i.next();
//      if (s.getLockID().asString().endsWith(lockName)) {
//        return s.getAvgWaitTimeInMillis();
//      }
//    }
    return -1;
  }
  
  private long getLockHeldTime(String lockName, long channelID) {
//    Collection c = statMBean.getTopHeld(500);
//    for (Iterator<LockHolder> i = c.iterator(); i.hasNext();) {
//      LockHolder s = i.next();
//      if (s.getLockID().asString().endsWith(lockName)) {
//        if (((ClientID) s.getNodeID()).getChannelID().toLong() == channelID) {
//          return s.getAndSetHeldTimeInMillis();
//        }
//      }
//    }
    throw new AssertionError(lockName + " does not exist.");
  }
  
  private long getLockWaitTime(String lockName, long channelID) {
//    Collection c = statMBean.getTopHeld(500);
//    for (Iterator<LockHolder> i = c.iterator(); i.hasNext();) {
//      LockHolder s = i.next();
//      if (s.getLockID().asString().endsWith(lockName)) {
//        if (((ClientID) s.getNodeID()).getChannelID().toLong() == channelID) {
//          return s.getAndSetWaitTimeInMillis();
//        }
//      }
//    }
    throw new AssertionError(lockName + " does not exist.");
  }

  private void verifyLockContended(String lockName, int expectedValue) {
//    Collection c = statMBean.getTopContendedLocks(10);
//    for (Iterator<LockStat> i = c.iterator(); i.hasNext();) {
//      LockStat s = i.next();
//      if (s.getLockID().asString().endsWith(lockName)) {
//        Assert.assertEquals(expectedValue, s.getNumOfPendingRequests());
//        break;
//      }
//    }
  }

  private void verifyLockHop(String lockName, int expectedValue) {
//    Collection c = statMBean.getTopLockHops(10);
//    for (Iterator<LockStat> i = c.iterator(); i.hasNext();) {
//      LockStat s = i.next();
//      if (s.getLockID().asString().endsWith(lockName)) {
//        Assert.assertEquals(expectedValue, s.getNumOfLockHopRequests());
//        break;
//      }
//    }
  }

  private void verifyClientStat(String lockName, int numOfClientsStackTraces, int traceDepth) {
    Collection c = statMBean.getLockSpecs();
    for (Iterator i=c.iterator(); i.hasNext();) {
      LockSpec lsi = (LockSpec)i.next();
      if (lsi.getLockID().asString().equals(lockName)) {
        System.err.println("lockID: " + lsi.getLockID());
        System.err.println(lsi.children().size());
        System.err.println(lsi.children());
        
        Assert.assertEquals(numOfClientsStackTraces, lsi.children().size());
        assertStackTracesDepth(lsi.children(), traceDepth);
      }
    }
  }
  
  private boolean assertStackTracesDepth(Collection traces, int expectedDepthOfStackTraces) {
    if (traces.size() == 0 && expectedDepthOfStackTraces == 0) { return true; }
    if (traces.size() == 0 || expectedDepthOfStackTraces == 0) { return false; }

    LockStatElement lse = (LockStatElement) traces.iterator().next();
    return assertStackTracesDepth(lse.children(), expectedDepthOfStackTraces - 1);
  }

  private static void echo(String msg) {
    System.err.println(msg);
  }

}
