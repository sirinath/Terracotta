/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.cluster.DsoClusterImpl;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.ClientIDLogger;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.JMXConnectorProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.DistributedObjectClient;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.bytecode.NullManager;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.handshakemanager.ClientHandshakeManagerImpl;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.logging.NullRuntimeLogger;
import com.tc.object.logging.RuntimeLogger;
import com.tc.object.msg.ClientHandshakeAckMessage;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.net.DSOClientMessageChannel;
import com.tc.object.session.SessionManager;
import com.tc.objectserver.control.ServerControl;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.stats.DSOMBean;
import com.tc.test.proxyconnect.ProxyConnectManager;
import com.tc.test.proxyconnect.ProxyConnectManagerImpl;
import com.tc.test.restart.RestartTestEnvironment;
import com.tc.test.restart.RestartTestHelper;
import com.tc.util.PortChooser;
import com.tc.util.ProductInfo;
import com.tcclient.cluster.DsoClusterInternal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

public class ClientHandshakeNoAckAndDisconnect extends BaseDSOTestCase {
  private final TCProperties             tcProps;
  private final int                      CLIENT_COUNT = 2;
  private DistributedObjectClient[]      client;
  private L1TVSConfigurationSetupManager manager;
  private final AtomicInteger            ackIgnored   = new AtomicInteger(0);
  private final AtomicInteger            clientIndex  = new AtomicInteger(-1);

  public ClientHandshakeNoAckAndDisconnect() {
    disableAllUntil("2009-05-01");
    tcProps = TCPropertiesImpl.getProperties();
    tcProps.setProperty(TCPropertiesConsts.L1_L2_CONFIG_VALIDATION_ENABLED, "false");
    System.out.println("L1 and L2 config match check disabled temporarily as we use proxy");
  }

  private class CustomDistributedObjectClient extends DistributedObjectClient {

    public CustomDistributedObjectClient(DSOClientConfigHelper config, TCThreadGroup threadGroup,
                                         ClassProvider classProvider,
                                         PreparedComponentsFromL2Connection connectionComponents, Manager manager,
                                         DsoClusterInternal dsoCluster, RuntimeLogger runtimeLogger) {
      super(config, threadGroup, classProvider, connectionComponents, manager, dsoCluster, runtimeLogger);
    }

    @Override
    protected ClientHandshakeManagerImpl createClientHandshakeManager(
                                                                      DSOClientMessageChannel chanel,
                                                                      Stage pauseStage,
                                                                      SessionManager sessionManager,
                                                                      DsoClusterInternal dsoClustr,
                                                                      ProductInfo info,
                                                                      List<ClientHandshakeCallback> clientHandshakeCallbacks) {

      return new CustomClientHandshakeManagerImpl(new ClientIDLogger(chanel.getClientIDProvider(), TCLogging
          .getLogger(ClientHandshakeManagerImpl.class)), chanel, chanel.getClientHandshakeMessageFactory(), pauseStage
          .getSink(), sessionManager, dsoClustr, info.version(), Collections
          .unmodifiableCollection(clientHandshakeCallbacks));

    }

    private class CustomClientHandshakeManagerImpl extends ClientHandshakeManagerImpl {
      private int ackCount = 0;

      public CustomClientHandshakeManagerImpl(TCLogger logger, DSOClientMessageChannel channel,
                                              ClientHandshakeMessageFactory chmf, Sink pauseSink,
                                              SessionManager sessionManager, DsoClusterInternal dsoCluster,
                                              String clientVersion, Collection<ClientHandshakeCallback> callbacks) {
        super(logger, channel, chmf, pauseSink, sessionManager, dsoCluster, clientVersion, callbacks);
      }

      @Override
      public void acknowledgeHandshake(ClientHandshakeAckMessage handshakeAck) {
        ackCount++;
        if (ackCount == 1) {
          ackIgnored.incrementAndGet();
          System.out.println("XXX IGNORING HANDSHAKE ACK FROM SERVER");
        } else {
          super.acknowledgeHandshake(handshakeAck);
        }
      }

    }

  }

  public void testBasic() throws Exception {
    final boolean isCrashy = true; // so persistence will be on
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

    // config for client
    configFactory().addServerToL1Config(null, proxyPort, jmxPort);
    manager = super.createL1ConfigManager();
    DSOClientConfigHelper configHelper = new StandardDSOClientConfigHelperImpl(manager);
    PreparedComponentsFromL2Connection components = new PreparedComponentsFromL2Connection(manager);

    client = new DistributedObjectClient[CLIENT_COUNT];
    for (int i = 0; i < CLIENT_COUNT; i++) {
      client[i] = new CustomDistributedObjectClient(configHelper, new TCThreadGroup(new ThrowableHandler(TCLogging
          .getLogger(DistributedObjectClient.class))), new MockClassProvider(), components, NullManager.getInstance(),
                                                    new DsoClusterImpl(), new NullRuntimeLogger());
      client[i].setCreateDedicatedMBeanServer(true);
    }

    Runnable r = new Runnable() {
      public void run() {
        int index = clientIndex.incrementAndGet();
        System.out.println("XXX Starting Client " + index);
        client[index].start();
        System.out.println("XXX Client started successfully");
      }
    };

    for (int i = 0; i < CLIENT_COUNT; i++) {
      new Thread(r).start();
    }

    while (ackIgnored.get() != CLIENT_COUNT) {
      System.out.println("XXX Waiting for Server's Handshake ACK to reach Client");
      Thread.sleep(2000);
    }

    // lets disconnect Server now
    server.crash();

    Thread.sleep(1 * 1000);

    server.start();

    // wait until client handshake is complete...
    waitUntilUnpaused(client);

    checkServerHasClients(CLIENT_COUNT, jmxPort);
  }

  private void waitUntilUnpaused(final DistributedObjectClient[] clienty) {
    for (int i = 0; i < clienty.length; i++) {
      ClientHandshakeManager mgr = clienty[i].getClientHandshakeManager();
      mgr.waitForHandshake();
      System.out.println("XXX Unpaused for Client " + i);
    }
  }

  private void checkServerHasClients(final int clientCount, final int jmxPort) throws Exception {
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

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    tcProps.setProperty(TCPropertiesConsts.L1_L2_CONFIG_VALIDATION_ENABLED, "true");
    System.out.println("Re-enabling L1 and L2 config match check");

  }
}
