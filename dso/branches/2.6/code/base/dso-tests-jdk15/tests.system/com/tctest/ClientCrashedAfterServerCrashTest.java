/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.cluster.Cluster;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.management.JMXConnectorProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.DistributedObjectClient;
import com.tc.object.PauseListener;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.bytecode.NullManager;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.objectserver.control.ServerControl;
import com.tc.stats.DSOMBean;
import com.tc.test.proxyconnect.ProxyConnectManager;
import com.tc.test.proxyconnect.ProxyConnectManagerImpl;
import com.tc.test.restart.RestartTestEnvironment;
import com.tc.test.restart.RestartTestHelper;
import com.tc.util.PortChooser;

import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

/**
 * This test is to check server behavior during boot-up after a crash. Server opens up a reconnect window of 120 seconds
 * (tc.config default value) and waits for connection request from previously connected clients. In the past we have
 * seen problems with server, if few of the previously connected clients fail to connect back within the reconnect
 * window. So, this test purposefully crashes the server and makes one of the previously connected client also crash
 * during the server boot-up and ensures server properly accepts connection request from all the previously connected
 * clients expect the one crashed.
 */

/**
 * Note: Doing server and client crashes manually in a unit test may not always run properly in the monkeys. Writing a
 * System test for the same is highly recommended as the below test is very hard to maintain. If you encounter more and
 * more problems maintaining this test, just disable this, for a System test is available for the same. Equivalent
 * System test is ClientAbscondAfterServerCrashTest
 * 
 * @author mgovinda
 */

public class ClientCrashedAfterServerCrashTest extends BaseDSOTestCase {

  private TestPauseListener pauseListener1;
  private TestPauseListener pauseListener2;

  public ClientCrashedAfterServerCrashTest() {
    //MNK-539 -- disabling this test till the fix review is over
    disableAllUntil("2008-06-03");
  }

  @Override
  public void setUp() {
    pauseListener1 = new TestPauseListener();
    pauseListener2 = new TestPauseListener();
  }

  public void testClientCrashAfterServerCrash() throws Exception {
    final boolean isCrashy = true;
    PortChooser portChooser = new PortChooser();
    List jvmArgs = new ArrayList();
    int proxyPort = portChooser.chooseRandomPort();

    RestartTestHelper helper = new RestartTestHelper(isCrashy,
                                                     new RestartTestEnvironment(this.getTempDirectory(), portChooser,
                                                                                RestartTestEnvironment.DEV_MODE),
                                                     jvmArgs);
    int dsoPort = helper.getServerPort();
    int jmxPort = helper.getAdminPort();
    ServerControl server = helper.getServerControl();

    ProxyConnectManager mgr = new ProxyConnectManagerImpl();
    mgr.setDsoPort(dsoPort);
    mgr.setProxyPort(proxyPort);
    mgr.setupProxy();

    server.start();
    mgr.proxyUp();

    // Client Configuration
    configFactory().addServerToL1Config(null, proxyPort, jmxPort);
    L1TVSConfigurationSetupManager manager = super.createL1ConfigManager();

    DSOClientConfigHelper configHelper = new StandardDSOClientConfigHelperImpl(manager);
    PreparedComponentsFromL2Connection components = new PreparedComponentsFromL2Connection(manager);
    DistributedObjectClient client1 = new DistributedObjectClient(configHelper,
                                                                  new TCThreadGroup(new ThrowableHandler(TCLogging
                                                                      .getLogger(DistributedObjectClient.class))),
                                                                  new MockClassProvider(), components, NullManager
                                                                      .getInstance(), new Cluster());

    DistributedObjectClient client2 = new DistributedObjectClient(configHelper,
                                                                  new TCThreadGroup(new ThrowableHandler(TCLogging
                                                                      .getLogger(DistributedObjectClient.class))),
                                                                  new MockClassProvider(), components, NullManager
                                                                      .getInstance(), new Cluster());

    // Start clients
    client1.setCreateDedicatedMBeanServer(true);
    client1.setPauseListener(pauseListener1);
    client1.start();

    client2.setCreateDedicatedMBeanServer(true);
    client2.setPauseListener(pauseListener2);
    client2.start();

    // Wait, till both are done with handshake
    pauseListener1.waitUntilUnpaused();
    pauseListener2.waitUntilUnpaused();

    // Ensure 2 clients are connected to server
    checkServerHasClients(2, jmxPort);

    // Now crash the server
    server.crash();

    Thread.sleep(3 * 1000);

    // (simulation) client1 crash. Close the client channel so that it doesn't start the Async Reconnect
    // client1.getCommunicationsManager().getConnectionManager().shutdown(); -- this doesn't stop Async reconnect
    client1.getChannel().close();
    System.out.println("Client 1 channel CLOSED");

    Thread.sleep(3 * 1000);

    // start the server back
    server.start();
    System.out.println("Server: I am back");

    // give time for jmx server to start up
    Thread.sleep(15 * 1000);
    System.out.println("JMX should have been up by now");

    // Wait till server accepts client2's reconnect request
    pauseListener2.waitUntilUnpaused();
    System.out.println("Client 2 UNPAUSED");

    checkServerHasClients(1, jmxPort);
  }

  private void checkServerHasClients(int clientCount, int jmxPort) throws Exception {
    JMXConnector jmxConnector = new JMXConnectorProxy("localhost", jmxPort);
    MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
    DSOMBean mbean = (DSOMBean) MBeanServerInvocationHandler.newProxyInstance(mbs, L2MBeanNames.DSO, DSOMBean.class,
                                                                              true);
    int actualClientCount = mbean.getClients().length;
    if (actualClientCount != clientCount) { throw new AssertionError(
                                                                     "Incorrect number of clients connected to the server: expected=["
                                                                         + clientCount + "] but was actual=["
                                                                         + actualClientCount + "]."); }

    System.out.println("***** " + clientCount + " clients are connected to the server.");
    jmxConnector.close();
  }

  private static final class TestPauseListener implements PauseListener {

    private boolean paused = true;

    public void waitUntilPaused() throws InterruptedException {
      waitUntilCondition(true);
    }

    public void waitUntilUnpaused() throws InterruptedException {
      waitUntilCondition(false);
    }

    public boolean isPaused() {
      synchronized (this) {
        return paused;
      }
    }

    private void waitUntilCondition(boolean b) throws InterruptedException {
      synchronized (this) {
        while (b != paused) {
          wait();
        }
      }
    }

    public void notifyPause() {
      synchronized (this) {
        paused = true;
        notifyAll();
      }
    }

    public void notifyUnpause() {
      synchronized (this) {
        paused = false;
        notifyAll();
      }
    }

  }

}
