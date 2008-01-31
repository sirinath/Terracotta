/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.async.api.Stage;
import com.tc.async.impl.StageManagerImpl;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OOOEventHandler;
import com.tc.net.protocol.delivery.OOONetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageSink;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.UnsupportedMessageTypeException;
import com.tc.net.protocol.tcm.msgs.PingMessage;
import com.tc.net.proxy.TCPProxy;
import com.tc.object.session.NullSessionManager;
import com.tc.test.TCTestCase;
import com.tc.util.SequenceGenerator;
import com.tc.util.concurrent.ThreadUtil;

import java.util.HashSet;

public class ConnectionHealthCheckerExtraCheckTest extends TCTestCase {

  CommunicationsManager serverComms;
  CommunicationsManager clientComms;
  NetworkListener       serverLsnr;
  TCPProxy              proxy     = null;
  int                   proxyPort = 0;

  protected void setUp(HealthCheckerConfig serverHCConf, HealthCheckerConfig clientHCConf) throws Exception {
    super.setUp();

    NetworkStackHarnessFactory networkStackHarnessFactory;
    if (false /* TCPropertiesImpl.getProperties().getBoolean("l1.reconnect.enabled") */) {
      StageManagerImpl stageManager = new StageManagerImpl(new TCThreadGroup(new ThrowableHandler(TCLogging
          .getLogger(StageManagerImpl.class))));
      final Stage oooStage = stageManager.createStage("OOONetStage", new OOOEventHandler(), 1, 5000);
      networkStackHarnessFactory = new OOONetworkStackHarnessFactory(
                                                                     new OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl(),
                                                                     oooStage.getSink());
    } else {
      networkStackHarnessFactory = new PlainNetworkStackHarnessFactory();
    }

    serverComms = new CommunicationsManagerImpl(new NullMessageMonitor(), networkStackHarnessFactory,
                                                new NullConnectionPolicy(), serverHCConf);
    clientComms = new CommunicationsManagerImpl(new NullMessageMonitor(), networkStackHarnessFactory,
                                                new NullConnectionPolicy(), clientHCConf);

    serverLsnr = serverComms.createListener(new NullSessionManager(), new TCSocketAddress(0), false,
                                            new DefaultConnectionIdFactory());

    serverLsnr.addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);
    serverLsnr.routeMessageType(TCMessageType.PING_MESSAGE, new TCMessageSink() {

      public void putMessage(TCMessage message) throws UnsupportedMessageTypeException {

        PingMessage pingMsg = (PingMessage) message;
        try {
          pingMsg.hydrate();
          System.out.println("Server RECEIVE - PING seq no " + pingMsg.getSequence());
        } catch (Exception e) {
          System.out.println("Server Exception during PingMessage hydrate:");
          e.printStackTrace();
        }

        PingMessage pingRplyMsg = pingMsg.createResponse();
        pingRplyMsg.send();
      }
    });

    serverLsnr.start(new HashSet());

    int serverPort = serverLsnr.getBindPort();
    proxyPort = serverPort + 1;
    proxy = new TCPProxy(proxyPort, serverLsnr.getBindAddress(), serverPort, 0, false, null);
    proxy.start();
  }

  ClientMessageChannel createClientMsgCh() {
    return createClientMsgChProxied(null);
  }

  ClientMessageChannel createClientMsgChProxied(CommunicationsManager clientCommsMgr) {

    ClientMessageChannel clientMsgCh = (clientCommsMgr == null ? clientComms : clientCommsMgr)
        .createClientChannel(new NullSessionManager(), 0, serverLsnr.getBindAddress().getHostAddress(), proxyPort,
                             1000, new ConnectionAddressProvider(new ConnectionInfo[] { new ConnectionInfo(serverLsnr
                                 .getBindAddress().getHostAddress(), proxyPort) }));

    clientMsgCh.addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);
    clientMsgCh.routeMessageType(TCMessageType.PING_MESSAGE, new TCMessageSink() {

      public void putMessage(TCMessage message) throws UnsupportedMessageTypeException {
        PingMessage pingMsg = (PingMessage) message;
        try {
          pingMsg.hydrate();
          System.out.println(" Client RECEIVE - PING seq no " + pingMsg.getSequence());
        } catch (Exception e) {
          System.out.println("Client Exception during PingMessage hydrate:");
          e.printStackTrace();
        }
      }
    });
    return clientMsgCh;
  }

  public long getMinSleepTimeToSendFirstProbe(HealthCheckerConfig config) {
    assertNotNull(config);
    /* Interval time is doubled to give grace period */
    return ((config.getKeepAliveIdleTime() + 2 * config.getKeepAliveInterval()) * 1000);
  }

  public long getMinSleepTimeToConirmDeath(HealthCheckerConfig config) {
    assertNotNull(config);
    /* Interval time is doubled to give grace period */
    long exact_time = (config.getKeepAliveIdleTime() + (config.getKeepAliveInterval() * config.getKeepAliveProbes())) * 1000;
    long grace_time = config.getKeepAliveInterval() * 1000;
    return (exact_time + grace_time);
  }

  public long getMinExtraCheckTime(HealthCheckerConfig config) {
    assertNotNull(config);
    long extraTimeSecs = (config.getKeepAliveInterval() * config.getKeepAliveProbes()) + config.getKeepAliveInterval();
    return (extraTimeSecs * 1000);
  }
  
  public long getFullExtraCheckTime(HealthCheckerConfig config) {
    assertNotNull(config);
    long extraTimeSecs = config.getKeepAliveInterval() * config.getKeepAliveProbes()
                         * ConnectionHealthChecker.MAX_EXTRACHECK_VALIDITY;

    return (extraTimeSecs * 1000);
  }

  public void testL2ExtraCheckL1() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigImpl(10, 4, 2, "ServerCommsHC-Test31", true /* EXTRA CHECK ON */);
    ((HealthCheckerConfigImpl) hcConfig).setDummy(); // Sends Dummy Ping Probe
    this.setUp(hcConfig, new NullHealthCheckerConfigImpl());
    ClientMessageChannel clientMsgCh = createClientMsgCh();
    clientMsgCh.open();

    // Verifications
    assertFalse(((CommunicationsManagerImpl) clientComms).getConnHealthChecker().isEnabled());
    ConnectionHealthChecker connHC = ((CommunicationsManagerImpl) serverComms).getConnHealthChecker();
    assertNotNull(connHC);

    assertTrue(connHC.isEnabled());
    while (!connHC.isRunning() && (connHC.getTotalConnsUnderMonitor() != 1)) {
      System.out.println("Yet to start the connection health cheker thread...");
      ThreadUtil.reallySleep(1000);
    }

    SequenceGenerator sq = new SequenceGenerator();
    for (int i = 1; i <= 5; i++) {
      PingMessage ping = (PingMessage) clientMsgCh.createMessage(TCMessageType.PING_MESSAGE);
      ping.initialize(sq);
      ping.send();
    }

    System.out.println("Sleeping for " + getMinSleepTimeToConirmDeath(hcConfig));
    ThreadUtil.reallySleep(getMinSleepTimeToConirmDeath(hcConfig));

    /*
     * L2 should have started the Extra Check by now; But L2 will not be able to do socket connect to L1.
     */
    ThreadUtil.reallySleep(getMinExtraCheckTime(hcConfig));

    /* By Now, the client should have been chuked out */
    assertEquals(0, connHC.getTotalConnsUnderMonitor());

    closeCommsMgr();
  }

  public void testL1ExtraCheckL2() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigImpl(10, 4, 2, "ServerCommsHC-Test32", true /* EXTRA CHECK ON */);
    ((HealthCheckerConfigImpl) hcConfig).setDummy(); // Sends Dummy Ping Probe
    this.setUp(new NullHealthCheckerConfigImpl(), hcConfig);
    ClientMessageChannel clientMsgCh = createClientMsgCh();
    clientMsgCh.open();

    // Verifications
    assertFalse(((CommunicationsManagerImpl) serverComms).getConnHealthChecker().isEnabled());
    ConnectionHealthChecker connHC = ((CommunicationsManagerImpl) clientComms).getConnHealthChecker();
    assertNotNull(connHC);

    assertTrue(connHC.isEnabled());
    while (!connHC.isRunning() && (connHC.getTotalConnsUnderMonitor() != 1)) {
      System.out.println("Yet to start the connection health cheker thread...");
      ThreadUtil.reallySleep(1000);
    }

    SequenceGenerator sq = new SequenceGenerator();
    for (int i = 1; i <= 5; i++) {
      PingMessage ping = (PingMessage) clientMsgCh.createMessage(TCMessageType.PING_MESSAGE);
      ping.initialize(sq);
      ping.send();
    }

    System.out.println("Sleeping for " + getMinSleepTimeToConirmDeath(hcConfig));
    ThreadUtil.reallySleep(getMinSleepTimeToConirmDeath(hcConfig));

    /*
     * L1 should have started the Extra Check by now; L1 should be able to do socket connect to L2.
     */
    ThreadUtil.reallySleep(getMinExtraCheckTime(hcConfig));
    assertEquals(1, connHC.getTotalConnsUnderMonitor());
    
    /*
     * Though L1 is able to connect, we can't trust the client for too long
     */
    ThreadUtil.reallySleep(getFullExtraCheckTime(hcConfig));
    assertEquals(0, connHC.getTotalConnsUnderMonitor());

    closeCommsMgr();
  }
  
  public void testL2ExtraCheckL1WithProxyDelay() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigImpl(5, 2, 2, "ServerCommsHC-Test33", true /* EXTRA CHECK ON */);
    this.setUp(hcConfig, new NullHealthCheckerConfigImpl());

    ClientMessageChannel clientMsgCh = createClientMsgCh();
    clientMsgCh.open();

    // Verifications
    assertFalse(((CommunicationsManagerImpl) clientComms).getConnHealthChecker().isEnabled());
    ConnectionHealthChecker connHC = ((CommunicationsManagerImpl) serverComms).getConnHealthChecker();
    assertNotNull(connHC);

    assertTrue(connHC.isEnabled());
    while (!connHC.isRunning() && (connHC.getTotalConnsUnderMonitor() != 1)) {
      System.out.println("Yet to start the connection health cheker thread...");
      ThreadUtil.reallySleep(1000);
    }

    SequenceGenerator sq = new SequenceGenerator();
    for (int i = 1; i <= 5; i++) {
      PingMessage ping = (PingMessage) clientMsgCh.createMessage(TCMessageType.PING_MESSAGE);
      ping.initialize(sq);
      ping.send();
    }

    /* Setting a LONG delay on the ROAD :D */
    proxy.setDelay(100 * 1000);

    System.out.println("Sleeping for " + getMinSleepTimeToConirmDeath(hcConfig));
    ThreadUtil.reallySleep(getMinSleepTimeToConirmDeath(hcConfig));

    /*
     * L2 should have started the Extra Check by now; But L2 will not be able to do socket connect to L1.
     */
    ThreadUtil.reallySleep(getMinExtraCheckTime(hcConfig));

    /* By Now, the client should be chuked out */
    assertEquals(0, connHC.getTotalConnsUnderMonitor());

    closeCommsMgr();
  }

  protected void closeCommsMgr() throws Exception {
    if (serverLsnr != null) serverLsnr.stop(1000);
    if (serverComms != null) serverComms.shutdown();
    if (clientComms != null) clientComms.shutdown();
  }

}
