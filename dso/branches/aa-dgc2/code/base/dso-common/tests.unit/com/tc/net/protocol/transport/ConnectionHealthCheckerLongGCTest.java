/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import org.hyperic.sigar.NetConnection;
import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.NetInfo;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.async.api.Stage;
import com.tc.async.impl.StageManagerImpl;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.LogLevelImpl;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.core.event.TCConnectionEvent;
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
import com.tc.properties.L1ReconnectConfigImpl;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tc.util.SequenceGenerator;
import com.tc.util.concurrent.QueueFactory;
import com.tc.util.concurrent.ThreadUtil;

import java.util.HashSet;

public class ConnectionHealthCheckerLongGCTest extends TCTestCase {

  CommunicationsManager serverComms;
  CommunicationsManager clientComms;
  NetworkListener       serverLsnr;
  TCLogger              logger    = TCLogging.getLogger(ConnectionHealthCheckerImpl.class);
  TCPProxy              proxy     = null;
  int                   proxyPort = 0;

  protected void setUp(HealthCheckerConfig serverHCConf, HealthCheckerConfig clientHCConf) throws Exception {
    setUp(serverHCConf, clientHCConf, false);
  }

  protected void setUp(HealthCheckerConfig serverHCConf, HealthCheckerConfig clientHCConf, boolean enableReconnect)
      throws Exception {
    setUp(serverHCConf, clientHCConf, enableReconnect, false);
  }

  protected void setUp(HealthCheckerConfig serverHCConf, HealthCheckerConfig clientHCConf, boolean enableReconnect,
                       boolean transportDisconnectRemovesChannel) throws Exception {
    super.setUp();

    NetworkStackHarnessFactory networkStackHarnessFactory;

    logger.setLevel(LogLevelImpl.DEBUG);

    if (enableReconnect) {
      StageManagerImpl stageManager = new StageManagerImpl(new TCThreadGroup(new ThrowableHandler(TCLogging
          .getLogger(StageManagerImpl.class))), new QueueFactory(BoundedLinkedQueue.class.getName()));
      final Stage oooSendStage = stageManager.createStage("OOONetSendStage", new OOOEventHandler(), 1, 5000);
      final Stage oooReceiveStage = stageManager.createStage("OOONetReceiveStage", new OOOEventHandler(), 1, 5000);
      networkStackHarnessFactory = new OOONetworkStackHarnessFactory(
                                                                     new OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl(),
                                                                     oooSendStage.getSink(), oooReceiveStage.getSink(),
                                                                     new L1ReconnectConfigImpl());
    } else {
      networkStackHarnessFactory = new PlainNetworkStackHarnessFactory();
    }

    if (serverHCConf != null) {
      serverComms = new CommunicationsManagerImpl(new NullMessageMonitor(), networkStackHarnessFactory,
                                                  new NullConnectionPolicy(), serverHCConf);
    } else {
      serverComms = new CommunicationsManagerImpl(new NullMessageMonitor(), networkStackHarnessFactory,
                                                  new NullConnectionPolicy());
    }

    if (clientHCConf != null) {
      clientComms = new CommunicationsManagerImpl(new NullMessageMonitor(), networkStackHarnessFactory,
                                                  new NullConnectionPolicy(), clientHCConf);
    } else {
      clientComms = new CommunicationsManagerImpl(new NullMessageMonitor(), networkStackHarnessFactory,
                                                  new NullConnectionPolicy());

    }

    serverLsnr = serverComms.createListener(new NullSessionManager(), new TCSocketAddress(0),
                                            transportDisconnectRemovesChannel, new DefaultConnectionIdFactory());

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
    proxyPort = new PortChooser().chooseRandomPort();
    proxy = new TCPProxy(proxyPort, serverLsnr.getBindAddress(), serverPort, 0, false, null);
    proxy.start();

  }

  ClientMessageChannel createClientMsgCh() {
    return createClientMsgCh(this.clientComms);
  }

  ClientMessageChannel createClientMsgCh(CommunicationsManager clientComm) {
    ClientMessageChannel clientMsgCh = clientComm
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
    return (config.getPingIdleTimeMillis() + (2 * config.getPingIntervalMillis()));
  }

  public long getMinSleepTimeToStartLongGCTest(HealthCheckerConfig config) {
    assertNotNull(config);
    /* Interval time is doubled to give grace period */
    long exact_time = config.getPingIdleTimeMillis() + (config.getPingIntervalMillis() * config.getPingProbes());
    long grace_time = config.getPingIntervalMillis();
    return (exact_time + grace_time);
  }

  public long getMinScoketConnectResultTime(HealthCheckerConfig config) {
    assertNotNull(config);
    long extraTime = (config.getPingIntervalMillis() * config.getPingProbes())
                     + (config.getSocketConnectTimeout() * config.getPingIntervalMillis());
    long grace_time = config.getPingIntervalMillis();
    return extraTime + grace_time;
  }

  public long getMinScoketConnectResultTimeAfterSocketConnectStart(HealthCheckerConfig config) {
    assertNotNull(config);
    long extraTime = (config.getSocketConnectTimeout() * config.getPingIntervalMillis());
    long grace_time = config.getPingIntervalMillis();
    return extraTime + grace_time;
  }

  public long getINITstageScoketConnectResultTime(HealthCheckerConfig config) {
    assertNotNull(config);
    long extraTime = config.getPingIdleTimeMillis()
                     + (config.getSocketConnectTimeout() * config.getPingIntervalMillis());
    long grace_time = config.getPingIntervalMillis();
    return extraTime + grace_time;
  }

  public long getFullExtraCheckTime(HealthCheckerConfig config) {
    assertNotNull(config);
    long extraTime = config.getPingIntervalMillis()
                     * ((config.getSocketConnectTimeout() * config.getPingProbes()) * config.getSocketConnectMaxCount());
    return extraTime;
  }

  public void testL2SocketConnectL1Fail() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigImpl(5000, 2000, 1, "ServerCommsHC-Test31", true);
    this.setUp(hcConfig, null);
    ((CommunicationsManagerImpl) clientComms).setConnHealthChecker(new ConnectionHealthCheckerDummyImpl());
    ClientMessageChannel clientMsgCh = createClientMsgCh();
    clientMsgCh.open();

    // Verifications
    ConnectionHealthCheckerImpl connHC = (ConnectionHealthCheckerImpl) ((CommunicationsManagerImpl) serverComms)
        .getConnHealthChecker();
    assertNotNull(connHC);

    while (!connHC.isRunning() || (connHC.getTotalConnsUnderMonitor() != 1)) {
      System.out.println("Yet to start the connection health cheker thread...");
      ThreadUtil.reallySleep(1000);
    }

    SequenceGenerator sq = new SequenceGenerator();
    for (int i = 1; i <= 5; i++) {
      PingMessage ping = (PingMessage) clientMsgCh.createMessage(TCMessageType.PING_MESSAGE);
      ping.initialize(sq);
      ping.send();
    }

    System.out.println("Sleeping for " + getMinSleepTimeToStartLongGCTest(hcConfig));
    ThreadUtil.reallySleep(getMinSleepTimeToStartLongGCTest(hcConfig));

    /*
     * L2 should have started the Extra Check by now; But L2 will not be able to do socket connect to L1.
     */
    ThreadUtil.reallySleep(getMinScoketConnectResultTime(hcConfig));

    /* By Now, the client should have been chucked out */
    assertEquals(0, connHC.getTotalConnsUnderMonitor());

  }

  public void testL1SocketConnectL2() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigImpl(5000, 2000, 2, "ClientCommsHC-Test32", true);
    this.setUp(null, hcConfig);
    ((CommunicationsManagerImpl) serverComms).setConnHealthChecker(new ConnectionHealthCheckerDummyImpl());
    ClientMessageChannel clientMsgCh = createClientMsgCh();
    clientMsgCh.open();

    // Verifications
    ConnectionHealthCheckerImpl connHC = (ConnectionHealthCheckerImpl) ((CommunicationsManagerImpl) clientComms)
        .getConnHealthChecker();
    assertNotNull(connHC);

    while (!connHC.isRunning() || (connHC.getTotalConnsUnderMonitor() != 1)) {
      System.out.println("Yet to start the connection health cheker thread...");
      ThreadUtil.reallySleep(1000);
    }

    SequenceGenerator sq = new SequenceGenerator();
    for (int i = 1; i <= 5; i++) {
      PingMessage ping = (PingMessage) clientMsgCh.createMessage(TCMessageType.PING_MESSAGE);
      ping.initialize(sq);
      ping.send();
    }

    System.out.println("Sleeping for " + getMinSleepTimeToStartLongGCTest(hcConfig));
    ThreadUtil.reallySleep(getMinSleepTimeToStartLongGCTest(hcConfig));

    /*
     * L1 should have started the Extra Check by now; L1 should be able to do socket connect to L2.
     */
    ThreadUtil.reallySleep(getMinScoketConnectResultTime(hcConfig));
    assertEquals(1, connHC.getTotalConnsUnderMonitor());

    /*
     * Though L1 is able to connect, we can't trust the client for too long
     */
    ThreadUtil.reallySleep(getFullExtraCheckTime(hcConfig));
    assertEquals(0, connHC.getTotalConnsUnderMonitor());

  }

  // MNK-831
  private int getNetInfoEstablishedConnectionsCount(int bindPort) throws SigarException {
    int establishedConnections = 0;
    Sigar s = new Sigar();
    NetInfo info = s.getNetInfo();
    NetInterfaceConfig config = s.getNetInterfaceConfig(null);
    System.out.println(info.toString());
    System.out.println(config.toString());

    int flags = NetFlags.CONN_TCP | NetFlags.TCP_ESTABLISHED;

    NetConnection[] connections = s.getNetConnectionList(flags);

    System.out.println("XXX Established connections if any");
    for (int i = 0; i < connections.length; i++) {
      long port = connections[i].getLocalPort();
      long remotePort = connections[i].getRemotePort();
      // not checking bind address
      if ((bindPort == port || bindPort == remotePort) && connections[i].getState() == NetFlags.TCP_ESTABLISHED) {
        establishedConnections++;
        System.out.println("XXX " + connections[i]);
      }
    }
    return establishedConnections;
  }

  public void testL1SocketConnectTimeoutL2() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigImpl(4000, 2000, 2, "ClientCommsHC-Test33", true);
    this.setUp(null, null);

    // set up custom HealthCheckers
    ((CommunicationsManagerImpl) serverComms).setConnHealthChecker(new ConnectionHealthCheckerDummyImpl());
    ((CommunicationsManagerImpl) clientComms)
        .setConnHealthChecker(new TestConnectionHealthCheckerImpl(hcConfig, clientComms.getConnectionManager()));

    ClientMessageChannel clientMsgCh = createClientMsgCh();
    clientMsgCh.open();

    // Verifications
    ConnectionHealthCheckerImpl connHC = (ConnectionHealthCheckerImpl) ((CommunicationsManagerImpl) clientComms)
        .getConnHealthChecker();
    assertNotNull(connHC);

    while (!connHC.isRunning() || (connHC.getTotalConnsUnderMonitor() != 1)) {
      System.out.println("Yet to start the connection health cheker thread...");
      ThreadUtil.reallySleep(1000);
    }

    // HC INIT stage: callback port verification should fail. wait for it. NOTE: we don't want to wait for ping-probe
    // cycles as the INIT stage directly starts socket connect
    System.out.println("Sleeping for " + getINITstageScoketConnectResultTime(hcConfig));
    ThreadUtil.reallySleep(getINITstageScoketConnectResultTime(hcConfig));

    // now the config is upgraded by some factor as the callbackport verification failed. That is
    int factor = ConnectionHealthCheckerContextImpl.CONFIG_UPGRADE_FACTOR;
    HealthCheckerConfig upgradedHcConfig = new HealthCheckerConfigImpl(factor * 4000, factor * 2000, factor * 2,
                                                                       "ClientCommsHC-Test33", true);

    try {
      while (getNetInfoEstablishedConnectionsCount(serverLsnr.getBindPort()) != 2) {
        System.out.println("XXX waiting for conn estd count to be 2");
        ThreadUtil.reallySleep(1000);
      }
    } catch (SigarException se) {
      // ignore
    }

    // HC START stage:
    System.out.println("Sleeping for " + getMinSleepTimeToStartLongGCTest(upgradedHcConfig));
    ThreadUtil.reallySleep(getMinSleepTimeToStartLongGCTest(upgradedHcConfig));

    /*
     * L1 should have started the Extra Check by now; Since socket connect already timedout in the INIT stage,
     * SOCKET_CONNECT stage should come out immediately as DEAD as the callback port has been reset to -1 by INIT.
     */
    ThreadUtil.reallySleep(upgradedHcConfig.getPingIntervalMillis() * 2);
    assertEquals(0, connHC.getTotalConnsUnderMonitor());

    try {
      while (getNetInfoEstablishedConnectionsCount(serverLsnr.getBindPort()) != 0) {
        System.out.println("XXX waiting for conn estd count to be 0");
        ThreadUtil.reallySleep(1000);
      }
    } catch (SigarException se) {
      // ignore
    }
  }

  public void testL1SocketConnectTimeoutL2AndL1Reconnect() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigImpl(4000, 1000, 2, "ClientCommsHC-Test34", true);
    this.setUp(null, null, true);

    // set up custom HealthCheckers
    ((CommunicationsManagerImpl) serverComms).setConnHealthChecker(new ConnectionHealthCheckerDummyImpl());
    ((CommunicationsManagerImpl) clientComms)
        .setConnHealthChecker(new TestConnectionHealthCheckerImpl(hcConfig, clientComms.getConnectionManager()));

    ClientMessageChannel clientMsgCh = createClientMsgCh();
    clientMsgCh.open();

    // Verifications
    ConnectionHealthCheckerImpl connHC = (ConnectionHealthCheckerImpl) ((CommunicationsManagerImpl) clientComms)
        .getConnHealthChecker();
    assertNotNull(connHC);

    while (!connHC.isRunning() || (connHC.getTotalConnsUnderMonitor() != 1)) {
      System.out.println("Yet to start the connection health cheker thread...");
      ThreadUtil.reallySleep(1000);
    }

    SequenceGenerator sq = new SequenceGenerator();
    for (int i = 1; i <= 5; i++) {
      PingMessage ping = (PingMessage) clientMsgCh.createMessage(TCMessageType.PING_MESSAGE);
      ping.initialize(sq);
      ping.send();
    }

    // HC INIT stage: callback port verification should fail. wait for it. NOTE: we don't want to wait for ping-probe
    // cycles as the INIT stage directly starts socket connect
    System.out.println("Sleeping for " + getINITstageScoketConnectResultTime(hcConfig));
    ThreadUtil.reallySleep(getINITstageScoketConnectResultTime(hcConfig));

    // now the config is upgraded by some factor as the callbackport verification failed. That is
    int factor = ConnectionHealthCheckerContextImpl.CONFIG_UPGRADE_FACTOR;
    HealthCheckerConfig upgradedHcConfig = new HealthCheckerConfigImpl(factor * 4000, factor * 1000, factor * 2,
                                                                       "ClientCommsHC-Test33", true);

    try {
      while (getNetInfoEstablishedConnectionsCount(serverLsnr.getBindPort()) != 2) {
        System.out.println("XXX waiting for conn estd count to be 2");
        ThreadUtil.reallySleep(1000);
      }
    } catch (SigarException se) {
      // ignore
    }

    proxy.setDelay(3000);

    int count = 0;
    while (!connHC.isRunning() || (connHC.getTotalConnsUnderMonitor() != 0)) {
      count++;
      if (count % 10 == 0) System.out.println("waiting for client to disconnect");
      ThreadUtil.reallySleep(1000);
    }

    proxy.setDelay(0);
    /*
     * Client is suppose to start the reconnect immediately after its disconnect. Lets remove the obstacle from the road
     */
    count = 0;
    while (!connHC.isRunning() || (connHC.getTotalConnsUnderMonitor() != 1)) {
      count++;
      if (count % 10 == 0) System.out.println("waiting for client to rejoin");
      ThreadUtil.reallySleep(1000);
    }

    // let the init socket connect settle down
    long sleepTime = getINITstageScoketConnectResultTime(upgradedHcConfig);
    System.out.println("Sleeping for " + sleepTime);
    ThreadUtil.reallySleep(sleepTime);

    /*
     * Client disconnected after it found socket connect timeout. After the successful reconnect there should be no
     * connection leak. DEV-1963
     */
    try {
      while (getNetInfoEstablishedConnectionsCount(serverLsnr.getBindPort()) != 2) {
        System.out.println("XXX waiting for conn estd count to be 2");
        ThreadUtil.reallySleep(1000);
      }
    } catch (SigarException se) {
      // ignore
    }
  }

  public void testL2SocketConnectL1FailTooLongGC() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigImpl(5000, 2000, 2, "ServerCommsHC-Test35", true);
    HealthCheckerConfig clientHcConfig = new HealthCheckerConfigClientImpl(15000, 5000, 2, "ClientCommsHC-Test35",
                                                                           false, 5, 5, "0.0.0.0", 0);
    this.setUp(hcConfig, clientHcConfig);

    ClientMessageChannel clientMsgCh = createClientMsgCh();
    clientMsgCh.open();

    // Verifications
    ConnectionHealthCheckerImpl connHC = (ConnectionHealthCheckerImpl) ((CommunicationsManagerImpl) serverComms)
        .getConnHealthChecker();
    assertNotNull(connHC);

    while (!connHC.isRunning() || (connHC.getTotalConnsUnderMonitor() != 1)) {
      System.out.println("Yet to start the connection health cheker thread...");
      ThreadUtil.reallySleep(1000);
    }

    /* Setting a LONG delay on the ROAD :D */
    proxy.setDelay(100 * 1000);

    System.out.println("Sleeping for " + getMinSleepTimeToStartLongGCTest(hcConfig));
    ThreadUtil.reallySleep(getMinSleepTimeToStartLongGCTest(hcConfig));

    /*
     * L2 should have started the Extra Check by now; L2 SocketConnect to L1 should pass.
     */
    assertEquals(1, connHC.getTotalConnsUnderMonitor());

    /*
     * Though L2 is able to connect, we can't trust the client for too long
     */
    ThreadUtil.reallySleep(getFullExtraCheckTime(hcConfig));
    assertEquals(0, connHC.getTotalConnsUnderMonitor());
  }

  public void testL2SocketConnectL1Pass() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigImpl(5000, 2000, 1, "ServerCommsHC-Test36", true,
                                                               Integer.MAX_VALUE, 2);
    HealthCheckerConfig clientHcConfig = new HealthCheckerConfigClientImpl("ClientCommsHC-Test36");

    this.setUp(hcConfig, clientHcConfig);
    ((CommunicationsManagerImpl) clientComms).setConnHealthChecker(new ConnectionHealthCheckerDummyImpl());
    ClientMessageChannel clientMsgCh = createClientMsgCh();
    clientMsgCh.open();

    // Verifications
    ConnectionHealthCheckerImpl connHC = (ConnectionHealthCheckerImpl) ((CommunicationsManagerImpl) serverComms)
        .getConnHealthChecker();
    assertNotNull(connHC);

    while (!connHC.isRunning() || (connHC.getTotalConnsUnderMonitor() != 1)) {
      System.out.println("Yet to start the connection health cheker thread...");
      ThreadUtil.reallySleep(1000);
    }

    SequenceGenerator sq = new SequenceGenerator();
    for (int i = 1; i <= 5; i++) {
      PingMessage ping = (PingMessage) clientMsgCh.createMessage(TCMessageType.PING_MESSAGE);
      ping.initialize(sq);
      ping.send();
    }

    System.out.println("Sleeping for " + getMinSleepTimeToStartLongGCTest(hcConfig));
    ThreadUtil.reallySleep(getMinSleepTimeToStartLongGCTest(hcConfig));

    /*
     * L2 should have started the Extra Check by now; With HCListener turnd on in client, L2 will be able to do socket
     * connect.
     */
    ThreadUtil.reallySleep(getMinScoketConnectResultTime(hcConfig));
    assertEquals(1, connHC.getTotalConnsUnderMonitor());
  }

  public void testL2SocketConnectMultipleL1PassAndFail() throws Exception {
    HealthCheckerConfig serverHcConfig = new HealthCheckerConfigImpl(5000, 2000, 3, "ServerCommsHC-Test37", true);
    HealthCheckerConfig clientHcConfig = new HealthCheckerConfigClientImpl(60000, 2000, 2,
                                                                           "ClientCommsHC-normal-Test37", false, 3, 2,
                                                                           "0.0.0.0", 0);

    this.setUp(serverHcConfig, clientHcConfig);

    // hooking in custom HC
    ((CommunicationsManagerImpl) serverComms)
        .setConnHealthChecker(new LongGCTestConnectionHealthCheckerImpl(serverHcConfig, serverComms
            .getConnectionManager()));

    // normal L1
    ((CommunicationsManagerImpl) clientComms).setConnHealthChecker(new ConnectionHealthCheckerDummyImpl());
    ClientMessageChannel clientMsgCh = createClientMsgCh();
    clientMsgCh.open();

    // socket connect should fail for this L1 and INIT stage should upgrade its config by a constant factor (3).
    CommunicationsManager badClientComms = new CommunicationsManagerImpl(new NullMessageMonitor(),
                                                                         new PlainNetworkStackHarnessFactory(),
                                                                         new NullConnectionPolicy(), clientHcConfig);
    ((CommunicationsManagerImpl) badClientComms).setConnHealthChecker(new ConnectionHealthCheckerDummyImpl());
    ClientMessageChannel clientMsgCh2 = createClientMsgCh(badClientComms);
    clientMsgCh2.open();

    // Verifications
    ConnectionHealthCheckerImpl connHC = (ConnectionHealthCheckerImpl) ((CommunicationsManagerImpl) serverComms)
        .getConnHealthChecker();
    assertNotNull(connHC);

    while (!connHC.isRunning() || (connHC.getTotalConnsUnderMonitor() != 2)) {
      System.out.println("Yet to start the connection health cheker thread...");
      ThreadUtil.reallySleep(1000);
    }

    SequenceGenerator sq = new SequenceGenerator();
    for (int i = 1; i <= 5; i++) {
      PingMessage ping = (PingMessage) clientMsgCh.createMessage(TCMessageType.PING_MESSAGE);
      ping.initialize(sq);
      ping.send();
    }

    long sleepTime = getMinSleepTimeToStartLongGCTest(serverHcConfig);
    System.out.println("XXX sleeping for " + sleepTime);
    ThreadUtil.reallySleep(sleepTime);

    // By this time, socket connect check should have been started.

    sleepTime = getMinScoketConnectResultTime(serverHcConfig);
    System.out.println("XXX sleeping for " + sleepTime);
    ThreadUtil.reallySleep(sleepTime);

    // By this time, first client should have been chucked out. The second client survives as its config has been
    // upgraded
    Assert.assertEquals(1, connHC.getTotalConnsUnderMonitor());
  }

  public void testL1CallbackPortListenerSwitchOff() throws Exception {
    HealthCheckerConfig serverHcConfig = new HealthCheckerConfigImpl(5000, 2000, 3, "ServerCommsHC-Test37", true);
    HealthCheckerConfig clientHcConfig = new HealthCheckerConfigClientImpl(6000, 2000, 2,
                                                                           "ClientCommsHC-normal-Test37", false, 3, 2,
                                                                           "0.0.0.0", -1);

    this.setUp(serverHcConfig, clientHcConfig);

    ClientMessageChannel clientMsgCh = createClientMsgCh();
    clientMsgCh.open();

    // Verifications
    ConnectionHealthCheckerImpl connHC = (ConnectionHealthCheckerImpl) ((CommunicationsManagerImpl) serverComms)
        .getConnHealthChecker();
    assertNotNull(connHC);

    while (!connHC.isRunning() || (connHC.getTotalConnsUnderMonitor() != 1)) {
      System.out.println("Yet to start the connection health cheker thread...");
      ThreadUtil.reallySleep(1000);
    }

    ThreadUtil.reallySleep(3000);
    // Client comms mgr should not start callbackport listener as it is disabled.
    assertNull(((CommunicationsManagerImpl) clientComms).getCallbackPortListener());
  }

  protected void closeCommsMgr() throws Exception {
    if (serverLsnr != null) serverLsnr.stop(1000);
    if (serverComms != null) serverComms.shutdown();
    if (clientComms != null) clientComms.shutdown();
  }

  public void tearDown() throws Exception {
    super.tearDown();
    logger.setLevel(LogLevelImpl.INFO);
    closeCommsMgr();
  }

  class LongGCTestConnectionHealthCheckerImpl extends TestConnectionHealthCheckerImpl {

    public LongGCTestConnectionHealthCheckerImpl(HealthCheckerConfig healthCheckerConfig,
                                                 TCConnectionManager connManager) {
      super(healthCheckerConfig, connManager);
    }

    @Override
    protected HealthCheckerMonitorThreadEngine getHealthMonitorThreadEngine(HealthCheckerConfig config,
                                                                            TCConnectionManager connectionManager,
                                                                            TCLogger loger) {
      return new LongGCTestHealthCheckerMonitorThreadEngine(config, connectionManager, loger);
    }

    class LongGCTestHealthCheckerMonitorThreadEngine extends TestHealthCheckerMonitorThreadEngine {

      public LongGCTestHealthCheckerMonitorThreadEngine(HealthCheckerConfig healthCheckerConfig,
                                                        TCConnectionManager connectionManager, TCLogger logger) {
        super(healthCheckerConfig, connectionManager, logger);
      }

      @Override
      protected ConnectionHealthCheckerContext getHealthCheckerContext(MessageTransportBase transport,
                                                                       HealthCheckerConfig conf,
                                                                       TCConnectionManager connManager) {

        return new LongGCTestConnectionHealthCheckerContextImpl(transport, conf, connManager);
      }
    }

    class LongGCTestConnectionHealthCheckerContextImpl extends TestConnectionHealthCheckerContextImpl {

      public LongGCTestConnectionHealthCheckerContextImpl(MessageTransportBase mtb, HealthCheckerConfig config,
                                                          TCConnectionManager connMgr) {
        super(mtb, config, connMgr);
      }

      @Override
      protected HealthCheckerSocketConnect getHealthCheckerSocketConnector(TCConnection connection,
                                                                           MessageTransportBase transportBase,
                                                                           TCLogger loger, HealthCheckerConfig cnfg) {
        int callbackPort = transportBase.getRemoteCallbackPort();
        if (TransportHandshakeMessage.NO_CALLBACK_PORT == callbackPort) { return new NullHealthCheckerSocketConnectImpl(); }

        TCSocketAddress sa = new TCSocketAddress(transportBase.getRemoteAddress().getAddress(), callbackPort);
        return new LongGCTestHealthCheckerSocketConnectImpl(sa, connection, transportBase.getRemoteAddress()
            .getCanonicalStringForm()
                                                                            + "(callbackport:" + callbackPort + ")",
                                                            loger, cnfg.getSocketConnectTimeout());

      }
    }

    private SynchronizedInt connectCount = new SynchronizedInt(0);

    class LongGCTestHealthCheckerSocketConnectImpl extends HealthCheckerSocketConnectImpl {

      public LongGCTestHealthCheckerSocketConnectImpl(TCSocketAddress peerNode, TCConnection conn,
                                                      String remoteNodeDesc, TCLogger logger, int timeoutInterval) {
        super(peerNode, conn, remoteNodeDesc, logger, timeoutInterval);
      }

      @Override
      public synchronized void connectEvent(TCConnectionEvent event) {
        connectCount.increment();
        if (connectCount.get() <= 1) {
          System.out.println("LongGCTestHealthCheckerSocketConnectImpl: supering connect event");
          super.connectEvent(event);
        } else {
          System.out.println("LongGCTestHealthCheckerSocketConnectImpl: ignoring connect event as designed");
        }
      }

    }
  }

}
