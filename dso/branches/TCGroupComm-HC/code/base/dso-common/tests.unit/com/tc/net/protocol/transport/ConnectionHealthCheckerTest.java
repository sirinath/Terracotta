/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
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
import com.tc.object.session.NullSessionManager;
import com.tc.test.TCTestCase;
import com.tc.util.SequenceGenerator;
import com.tc.util.concurrent.ThreadUtil;

import java.util.HashSet;

public class ConnectionHealthCheckerTest extends TCTestCase {

  CommunicationsManager serverComms;
  CommunicationsManager clientComms;
  NetworkListener       serverLsnr;

  protected void setUp(HealthCheckerConfig serverHCConf, HealthCheckerConfig clientHCConf) throws Exception {
    super.setUp();

    serverComms = new CommunicationsManagerImpl(new NullMessageMonitor(), new PlainNetworkStackHarnessFactory(true),
                                                new NullConnectionPolicy(), serverHCConf);
    clientComms = new CommunicationsManagerImpl(new NullMessageMonitor(), new PlainNetworkStackHarnessFactory(true),
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
  }

  ClientMessageChannel createClientMsgCh() {
    return createClientMsgCh(null);
  }

  ClientMessageChannel createClientMsgCh(CommunicationsManager clientCommsMgr) {

    ClientMessageChannel clientMsgCh = (clientCommsMgr == null ? clientComms : clientCommsMgr)
        .createClientChannel(
                             new NullSessionManager(),
                             10,
                             TCSocketAddress.LOOPBACK_IP,
                             serverLsnr.getBindPort(),
                             1000,
                             new ConnectionAddressProvider(
                                                           new ConnectionInfo[] { new ConnectionInfo("localhost",
                                                                                                     serverLsnr
                                                                                                         .getBindPort()) }));
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

  public void testL2L1_ClientSide_PROBEchk() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigCustomImpl(1, 1, 1, "ClientCommsHC");
    this.setUp(null, hcConfig);
    ClientMessageChannel clientMsgCh = createClientMsgCh();
    clientMsgCh.open();

    // Verifications
    assertNull(serverComms.getConnHealthChecker());
    ConnectionHealthChecker connHC = clientComms.getConnHealthChecker();
    assertNotNull(connHC);

    assertTrue(connHC.isEnabled());
    while (!connHC.isRunning()) {
      System.out.println("Yet to start the connection health cheker thread...");
      ThreadUtil.reallySleep(1000);
    }

    SequenceGenerator sq = new SequenceGenerator();
    for (int i = 1; i <= 25; i++) {
      PingMessage ping = (PingMessage) clientMsgCh.createMessage(TCMessageType.PING_MESSAGE);
      ping.initialize(sq);
      ping.send();
    }

    assertEquals(1, connHC.getTotalConnsUnderMonitor());

    int IDLETIMEs = 2;
    long sleepTime = (hcConfig.getKeepAliveIdleTime() / hcConfig.getKeepAiveInterval() + 1)
                     * hcConfig.getKeepAiveInterval() * 1000;
    System.out.println("Sleeing for " + sleepTime * IDLETIMEs + " seconds");
    ThreadUtil.reallySleep(IDLETIMEs * sleepTime);
    System.out.println("Successfully sent " + connHC.getTotalProbesSentOnAllConns() + " Probes");
    assertTrue(connHC.getTotalProbesSentOnAllConns() > 0);

    clientMsgCh.close();
    closeCommsMgr();
  }

  public void testL2L1_ServerSide_PROBEchk() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigDefaultImpl("ServerCommsHC");
    this.setUp(hcConfig, null);
    ClientMessageChannel clientMsgCh = createClientMsgCh();
    clientMsgCh.open();

    // Verifications
    assertNull(clientComms.getConnHealthChecker());
    ConnectionHealthChecker connHC = serverComms.getConnHealthChecker();
    assertNotNull(connHC);

    assertTrue(connHC.isEnabled());
    while (!connHC.isRunning()) {
      System.out.println("Yet to start the connection health cheker thread...");
      ThreadUtil.reallySleep(1000);
    }
    assertEquals(1, connHC.getTotalConnsUnderMonitor());

    SequenceGenerator sq = new SequenceGenerator();
    for (int i = 1; i <= 25; i++) {
      PingMessage ping = (PingMessage) clientMsgCh.createMessage(TCMessageType.PING_MESSAGE);
      ping.initialize(sq);
      ping.send();
    }

    int IDLETIMEs = 2;
    long sleepTime = (hcConfig.getKeepAliveIdleTime() / hcConfig.getKeepAiveInterval() + 1)
                     * hcConfig.getKeepAiveInterval() * 1000;
    System.out.println("Sleeing for " + sleepTime * IDLETIMEs + " seconds");
    ThreadUtil.reallySleep(IDLETIMEs * sleepTime);
    System.out.println("Successfully sent " + connHC.getTotalProbesSentOnAllConns() + " Probes");
    assertTrue(connHC.getTotalProbesSentOnAllConns() > 0);

    clientMsgCh.close();
    System.out.println("ClientMessasgeChannel Closed");
    ThreadUtil.reallySleep(hcConfig.getKeepAliveIdleTime() * 1000);
    assertEquals(0, connHC.getTotalConnsUnderMonitor());

    closeCommsMgr();
  }

  public void testL2L1_Clientdisconnect() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigCustomImpl(5, 2, 2, "ServerCommsHC");
    this.setUp(hcConfig, null);
    ClientMessageChannel clientMsgCh = createClientMsgCh();
    clientMsgCh.open();

    // Verifications
    assertNull(clientComms.getConnHealthChecker());
    ConnectionHealthChecker connHC = serverComms.getConnHealthChecker();
    assertNotNull(connHC);

    assertTrue(connHC.isEnabled());
    while (!connHC.isRunning()) {
      System.out.println("Yet to start the connection health cheker thread...");
      ThreadUtil.reallySleep(1000);
    }
    assertEquals(1, connHC.getTotalConnsUnderMonitor());

    SequenceGenerator sq = new SequenceGenerator();
    for (int i = 1; i <= 25; i++) {
      PingMessage ping = (PingMessage) clientMsgCh.createMessage(TCMessageType.PING_MESSAGE);
      ping.initialize(sq);
      ping.send();
    }

    int IDLETIMEs = 2;
    long sleepTime = (hcConfig.getKeepAliveIdleTime() / hcConfig.getKeepAiveInterval() + 1)
                     * hcConfig.getKeepAiveInterval() * 1000;
    System.out.println("Sleeing for " + sleepTime * IDLETIMEs + " seconds");
    ThreadUtil.reallySleep(IDLETIMEs * sleepTime);
    System.out.println("Successfully sent " + connHC.getTotalProbesSentOnAllConns() + " Probes");
    assertTrue(connHC.getTotalProbesSentOnAllConns() > 0);

    clientMsgCh.close();
    System.out.println("ClientMessasgeChannel Closed");

    System.out.println("Trying to send more data to closed client channel");
    PingMessage ping = (PingMessage) clientMsgCh.createMessage(TCMessageType.PING_MESSAGE);
    ping.initialize(sq);
    ping.send();

    ThreadUtil.reallySleep(2 * hcConfig.getKeepAiveInterval() * 1000);
    assertEquals(0, connHC.getTotalConnsUnderMonitor());

    closeCommsMgr();
  }

  public void testL2L1_TwoClientdisconnect() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigCustomImpl(4, 2, 5, "ServerCommsHC");
    HealthCheckerConfig hcConfig2 = new HealthCheckerConfigCustomImpl(10, 4, 3, "ClientCommsHC");
    this.setUp(hcConfig, null);

    CommunicationsManager clientComms1 = new CommunicationsManagerImpl(new NullMessageMonitor(),
                                                                       new PlainNetworkStackHarnessFactory(true),
                                                                       new NullConnectionPolicy(), hcConfig2);
    CommunicationsManager clientComms2 = new CommunicationsManagerImpl(new NullMessageMonitor(),
                                                                       new PlainNetworkStackHarnessFactory(true),
                                                                       new NullConnectionPolicy(), hcConfig2);
    ClientMessageChannel clientMsgCh1 = createClientMsgCh(clientComms1);
    clientMsgCh1.open();

    ClientMessageChannel clientMsgCh2 = createClientMsgCh(clientComms2);
    clientMsgCh2.open();

    // Verifications
    ConnectionHealthChecker connHC = serverComms.getConnHealthChecker();

    assertNotNull(connHC);
    assertTrue(connHC.isEnabled());
    while (!connHC.isRunning() && (connHC.getTotalConnsUnderMonitor() != 2)) {
      System.out.println("Yet to start the connection health cheker thread...");
      ThreadUtil.reallySleep(1000);
    }

    SequenceGenerator sq = new SequenceGenerator();
    for (int i = 1; i <= 25; i++) {
      PingMessage ping = (PingMessage) clientMsgCh1.createMessage(TCMessageType.PING_MESSAGE);
      ping.initialize(sq);
      ping.send();
      ping = (PingMessage) clientMsgCh2.createMessage(TCMessageType.PING_MESSAGE);
      ping.initialize(sq);
      ping.send();
    }

    int IDLETIMEs = 2;
    long sleepTime = (hcConfig.getKeepAliveIdleTime() / hcConfig.getKeepAiveInterval() + 1)
                     * hcConfig.getKeepAiveInterval() * 1000;
    System.out.println("Sleeing for " + sleepTime * IDLETIMEs + " seconds");
    ThreadUtil.reallySleep(IDLETIMEs * sleepTime);
    System.out.println("Successfully sent " + connHC.getTotalProbesSentOnAllConns() + " Probes");
    assertTrue(connHC.getTotalProbesSentOnAllConns() > 0);

    clientMsgCh1.close();
    System.out.println("ClientMessasgeChannel 1 Closed");

    ThreadUtil.reallySleep(hcConfig.getKeepAliveIdleTime() * 1000);
    ThreadUtil.reallySleep(2 * hcConfig.getKeepAiveInterval() * 1000);
    assertEquals(1, connHC.getTotalConnsUnderMonitor());

    closeCommsMgr();
  }

  public void testL2L1WrongConfig() throws Exception {
    HealthCheckerConfig hcConfig = new HealthCheckerConfigCustomImpl(30, 40, 3, "ServerCommsHC");
    this.setUp(hcConfig, null);

    // Verifications
    assertNull(clientComms.getConnHealthChecker());
    ConnectionHealthChecker connHC = serverComms.getConnHealthChecker();
    assertNotNull(connHC);
    closeCommsMgr();

    hcConfig = new HealthCheckerConfigCustomImpl(30, 0, 3, "ClientCommsHC");
    this.setUp(null, hcConfig);

    // Verifications
    assertNull(serverComms.getConnHealthChecker());
    connHC = clientComms.getConnHealthChecker();
    assertNotNull(connHC);
    closeCommsMgr();

  }

  protected void closeCommsMgr() throws Exception {
    if (serverLsnr != null) serverLsnr.stop(1000);
    if (serverComms != null) serverComms.shutdown();
    if (clientComms != null) clientComms.shutdown();
  }

}
