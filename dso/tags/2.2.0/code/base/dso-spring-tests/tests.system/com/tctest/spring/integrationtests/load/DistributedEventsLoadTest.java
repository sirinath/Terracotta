/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.load;

import com.tctest.spring.bean.EventManager;
import com.tctest.spring.integrationtests.framework.AbstractDeploymentTest;
import com.tctest.spring.integrationtests.framework.Deployment;
import com.tctest.spring.integrationtests.framework.TestCallback;
import com.tctest.spring.integrationtests.framework.WebApplicationServer;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class DistributedEventsLoadTest extends AbstractDeploymentTest {

  private static final boolean DEBUG                         = false;
  private static final int     NUM_ITERATION                 = 10;

  private static final String  REMOTE_SERVICE_NAME           = "EventManager";
  private static final String  BEAN_DEFINITION_FILE_FOR_TEST = "classpath:/com/tctest/spring/distributedevents.xml";
  private static final String  CONFIG_FILE_FOR_TEST          = "/tc-config-files/event-tc-config.xml";
  private static final String  CONTEXT                       = "distributed-events";
  private static final String  BEAN_NAME                     = "eventManager";
  private Deployment           deployment;

  protected void setUp() throws Exception {
    super.setUp();
    if (deployment == null) {
      deployment = makeWAR();
    }
  }

  public void testTwoNodeDistributedEventsLoad() throws Throwable {
    publishDistributedEvents(2);
  }

  public void testFourNodeDistributedEventsLoad() throws Throwable {
    publishDistributedEvents(4);
  }

  public void testEightNodeDistributedEventsLoad() throws Throwable {
    publishDistributedEvents(8);
  }

  private void publishDistributedEvents(final int nodeCount) throws Throwable {
    List servers = new ArrayList();
    final List eventManagers = new ArrayList();
    WebApplicationServer server;

    for (int i = 0; i < nodeCount; i++) {
      server = makeWebApplicationServer(CONFIG_FILE_FOR_TEST);
      server.addWarDeployment(deployment, CONTEXT);
      server.start();
      servers.add(server);
      eventManagers.add(server.getProxy(EventManager.class, REMOTE_SERVICE_NAME));
    }

    debugPrintln("publishDistributedEvents():  num_iteration per node = " + NUM_ITERATION);

    long totalTime = 0;
    long startTime = System.currentTimeMillis();

    debugPrintln("publishDistributedEvents():  startTime = " + startTime);

    for (int i = 0; i < NUM_ITERATION * nodeCount; i++) {
      debugPrintln("publishDistributedEvents():  i % nodeCount = " + (i % nodeCount));

      ((EventManager) eventManagers.get(i % nodeCount))
          .publishEvent("foo" + i, "bar" + i);
    }

    waitForSuccess(8 * 60, new TestCallback() {
      public void check() {
        for (Iterator iter = eventManagers.iterator(); iter.hasNext();) {
          EventManager em = (EventManager) iter.next();
          assertEquals(NUM_ITERATION * nodeCount, em.size());
        }
      }
    });

    long endTime = 0;

    for (Iterator iter = eventManagers.iterator(); iter.hasNext();) {
      EventManager em = (EventManager) iter.next();
      Date date = em.getLastEventTime();

      debugPrintln("eventManager = " + em.toString() + " lastEventTime = " + date.getTime());

      if (date.getTime() > endTime) {
        endTime = date.getTime();
      }
    }

    totalTime = endTime - startTime;

    printData(nodeCount, totalTime);
  }

  private Deployment makeWAR() throws Exception {

    return makeDeploymentBuilder(CONTEXT + ".war").addBeanDefinitionFile(BEAN_DEFINITION_FILE_FOR_TEST)
        .addRemoteService(REMOTE_SERVICE_NAME, BEAN_NAME, EventManager.class)
        .addDirectoryOrJARContainingClass(EventManager.class).addDirectoryContainingResource(CONFIG_FILE_FOR_TEST)
        .makeDeployment();
  }

  private void printData(int nodeCount, long totalTime) {
    System.out.println("**%% TERRACOTTA TEST STATISTICS %%**: nodes=" + nodeCount + " iteration="
                       + (NUM_ITERATION * nodeCount) + " time=" + totalTime + " nanoseconds  avg="
                       + (totalTime / (NUM_ITERATION * nodeCount)) + " nanoseconds/iteration");
  }

  private void debugPrintln(String s) {
    if (DEBUG) {
      System.out.println("XXXXX " + s);
    }
  }

}
