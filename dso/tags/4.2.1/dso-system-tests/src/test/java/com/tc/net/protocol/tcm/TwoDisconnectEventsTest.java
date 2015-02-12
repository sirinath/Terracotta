/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import org.mockito.Mockito;

import com.tc.abortable.NullAbortableOperationManager;
import com.tc.cluster.DsoClusterImpl;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.lang.StartupHelper;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandlerImpl;
import com.tc.logging.TCLogging;
import com.tc.net.core.MockTCConnection;
import com.tc.net.core.TCConnection;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayerImpl;
import com.tc.net.protocol.tcm.msgs.PingMessage;
import com.tc.net.protocol.transport.ServerMessageTransport;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.DistributedObjectClient;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.bytecode.NullManager;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.platform.rejoin.RejoinManagerInternal;
import com.tc.server.TCServer;
import com.tc.server.TCServerImpl;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class TwoDisconnectEventsTest extends BaseDSOTestCase {

  public void testTwoDisconnectEvents() throws Exception {
    final PortChooser pc = new PortChooser();
    final int tsaPort = pc.chooseRandomPort();
    final int jmxPort = pc.chooseRandomPort();
    final TCServerImpl server = (TCServerImpl) startupServer(tsaPort, jmxPort);

    try {
      final DistributedObjectClient client = startupClient(tsaPort, jmxPort);
      try {
        // wait until client handshake is complete...
        waitUntilUnpaused(client);

        ClientMessageChannelImpl clientChannel = (ClientMessageChannelImpl) client.getChannel().channel();
        ServerMessageChannelImpl serverChannel = (ServerMessageChannelImpl) server.getDSOServer().getChannelManager()
            .getChannel(clientChannel.getChannelID());

        // setup server to send ping message
        serverChannel.addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);
        PingMessageSink serverSink = new PingMessageSink();
        ((CommunicationsManagerImpl) server.getDSOServer().getCommunicationsManager()).getMessageRouter()
            .routeMessageType(TCMessageType.PING_MESSAGE, serverSink);

        // set up client to receive ping message
        clientChannel.addClassMapping(TCMessageType.PING_MESSAGE, PingMessage.class);
        PingMessageSink clientSink = new PingMessageSink();
        ((CommunicationsManagerImpl) client.getCommunicationsManager()).getMessageRouter()
            .routeMessageType(TCMessageType.PING_MESSAGE, clientSink);

        // server ping client
        TCMessage msg = serverChannel.createMessage(TCMessageType.PING_MESSAGE);
        msg.send();
        while (clientSink.getReceivedCount() != 1) {
          ThreadUtil.reallySleep(500);
          System.out.println(".");
        }
        PingMessage pingReceived = clientSink.getReceivedPing();
        Assert.assertTrue(msg.getSourceNodeID().equals(server.getDSOServer().getServerNodeID()));
        Assert.assertTrue(msg.getDestinationNodeID().equals(pingReceived.getDestinationNodeID()));

        // client ping server
        msg = clientChannel.createMessage(TCMessageType.PING_MESSAGE);
        msg.send();
        while (serverSink.getReceivedCount() != 1) {
          ThreadUtil.reallySleep(500);
          System.out.println(".");
        }
        pingReceived = serverSink.getReceivedPing();
        Assert.assertTrue(msg.getSourceNodeID().equals(pingReceived.getSourceNodeID()));
        Assert.assertTrue(server.getDSOServer().getServerNodeID().equals(pingReceived.getDestinationNodeID()));

        // two transport disconnect events to client.
        ServerMessageChannelImpl smci = serverChannel;
        ServerMessageTransport smt;
        if (smci.getSendLayer() instanceof ServerMessageTransport) {
          smt = (ServerMessageTransport) smci.getSendLayer();
        } else {
          smt = (ServerMessageTransport) ((OnceAndOnlyOnceProtocolNetworkLayerImpl) smci.getSendLayer()).getSendLayer();
        }
        smt.setAllowConnectionReplace(true);

        // send first event
        TCConnection tccomm = new MockTCConnection();
        smt.attachNewConnection(tccomm);
        smt.closeEvent(new TCConnectionEvent(tccomm));

        // send second event
        tccomm = new MockTCConnection();
        smt.attachNewConnection(tccomm);
        smt.closeEvent(new TCConnectionEvent(tccomm));
        ThreadUtil.reallySleep(2000);

      } finally {
        client.getCommunicationsManager().shutdown();
        client.stopForTests();
      }
    } finally {
      server.stop();
    }
  }

  private void waitUntilUnpaused(final DistributedObjectClient client) {
    ClientHandshakeManager mgr = client.getClientHandshakeManager();
    mgr.waitForHandshake();
  }

  private class PingMessageSink implements TCMessageSink {
    Queue<PingMessage> queue = new LinkedBlockingQueue<PingMessage>();

    @Override
    public void putMessage(final TCMessage message) throws UnsupportedMessageTypeException {

      PingMessage ping = (PingMessage) message;

      try {
        message.hydrate();
      } catch (Exception e) {
        //
      }
      queue.add(ping);
    }

    public int getReceivedCount() {
      return queue.size();
    }

    public PingMessage getReceivedPing() {
      return queue.peek();
    }

  }

  protected TCServer startupServer(final int tsaPort, final int jmxPort) {
    StartAction start_action = new StartAction(tsaPort, jmxPort);
    new StartupHelper(group, start_action).startUp();
    final TCServer server = start_action.getServer();
    return server;
  }

  protected DistributedObjectClient startupClient(final int tsaPort, final int jmxPort)
      throws ConfigurationSetupException {
    configFactory().addServerToL1Config("127.0.0.1", tsaPort, jmxPort);
    L1ConfigurationSetupManager manager = super.createL1ConfigManager();

    RejoinManagerInternal mock = Mockito.mock(RejoinManagerInternal.class);
    DistributedObjectClient client = new DistributedObjectClient(new StandardDSOClientConfigHelperImpl(manager),
                                                                 new TCThreadGroup(new ThrowableHandlerImpl(TCLogging
                                                                     .getLogger(DistributedObjectClient.class))),
                                                                 new MockClassProvider(),
                                                                 new PreparedComponentsFromL2Connection(manager),
                                                                 NullManager.getInstance(),
                                                                 new DsoClusterImpl(mock),
                                                                 new NullAbortableOperationManager(),
                                                                 mock);
    client.start();
    return client;
  }

  protected final TCThreadGroup group = new TCThreadGroup(
                                                          new ThrowableHandlerImpl(TCLogging
                                                              .getLogger(DistributedObjectServer.class)));

  protected class StartAction implements StartupHelper.StartupAction {
    private final int tsaPort;
    private final int jmxPort;
    private TCServer  server = null;

    private StartAction(final int tsaPort, final int jmxPort) {
      this.tsaPort = tsaPort;
      this.jmxPort = jmxPort;
    }

    public int getTsaPort() {
      return tsaPort;
    }

    public int getJmxPort() {
      return jmxPort;
    }

    public TCServer getServer() {
      return server;
    }

    @Override
    public void execute() throws Throwable {
      ManagedObjectStateFactory.disableSingleton(true);
      TestConfigurationSetupManagerFactory factory = configFactory();
      L2ConfigurationSetupManager manager = factory.createL2TVSConfigurationSetupManager(null, true);
      manager.dsoL2Config().tsaPort().setIntValue(tsaPort);
      manager.dsoL2Config().tsaPort().setBind("127.0.0.1");

      manager.commonl2Config().jmxPort().setIntValue(jmxPort);
      manager.commonl2Config().jmxPort().setBind("127.0.0.1");

      server = new TCServerImpl(manager);
      server.start();
    }
  }
}
