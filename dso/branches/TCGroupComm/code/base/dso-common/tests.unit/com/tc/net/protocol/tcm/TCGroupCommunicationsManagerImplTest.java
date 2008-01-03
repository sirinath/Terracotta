/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.groups.NodeID;
import com.tc.net.groups.NodeIDImpl;
import com.tc.net.groups.NodeIdUuidImpl;
import com.tc.net.protocol.TCGroupNetworkStackHarnessFactory;
import com.tc.net.protocol.transport.DefaultConnectionIdFactory;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.object.session.NullSessionManager;

import java.util.HashSet;

import junit.framework.TestCase;

public class TCGroupCommunicationsManagerImplTest extends TestCase {

  MessageMonitor                    monitor        = new NullMessageMonitor();
  final NullSessionManager          sessionManager = new NullSessionManager();
  final TCMessageFactory            msgFactory     = new TCMessageFactoryImpl(sessionManager, monitor);
  final TCMessageRouter             msgRouter      = new TCMessageRouterImpl();

  final ServerMessageChannelFactory channelFactory = new ServerMessageChannelFactory() {
                                                     public MessageChannelInternal createNewChannel(ChannelID id) {
                                                       return new ServerMessageChannelImpl(id, msgRouter, msgFactory);
                                                     }
                                                   };

  public void testOneWayChannelOpenClose() throws Exception {
    NodeID nodeID1 = new NodeIdUuidImpl("node-client");
    NodeID nodeID2 = new NodeIdUuidImpl("node-server");
    CommunicationsManager clientComms = new TCGroupCommunicationsManagerImpl(monitor,
                                                                             new TCGroupNetworkStackHarnessFactory(),
                                                                             null, new NullConnectionPolicy(), 0,
                                                                             nodeID1);
    CommunicationsManager serverComms = new TCGroupCommunicationsManagerImpl(monitor,
                                                                             new TCGroupNetworkStackHarnessFactory(),
                                                                             null, new NullConnectionPolicy(), 0,
                                                                             nodeID2);
    try {
      NetworkListener lsnr = serverComms.createListener(sessionManager,
                                                        new TCSocketAddress(TCSocketAddress.LOOPBACK_ADDR, 0), true,
                                                        new DefaultConnectionIdFactory());
      lsnr.start(new HashSet());
      ChannelManager channelManager = lsnr.getChannelManager();
      assertEquals(0, channelManager.getChannels().length);

      ClientMessageChannel channel;
      channel = clientComms
          .createClientChannel(
                               sessionManager,
                               0,
                               TCSocketAddress.LOOPBACK_IP,
                               lsnr.getBindPort(),
                               3000,
                               new ConnectionAddressProvider(
                                                             new ConnectionInfo[] { new ConnectionInfo(
                                                                                                       "localhost",
                                                                                                       lsnr
                                                                                                           .getBindPort()) }));
      channel.open();
      assertTrue(channel.isConnected());
      assertTrue(channel.getChannelID().getNodeID().equals(nodeID2));

      assertEquals(1, channelManager.getChannels().length);

      MessageChannel serverChannel = channelManager.getChannels()[0];
      System.out.println("Group server node name:" + ((NodeIDImpl) serverChannel.getChannelID().getNodeID()).getName());
      System.out.println("Group client node name:" + ((NodeIDImpl) channel.getChannelID().getNodeID()).getName());
      assertTrue(nodeID1.equals(serverChannel.getChannelID().getNodeID()));

      clientComms.getConnectionManager().closeAllConnections(5000);
      assertFalse(channel.isConnected());

      for (int i = 0; i < 30; i++) {
        if (channelManager.getChannels().length == 0) {
          break;
        }
        Thread.sleep(100);
      }

      assertEquals(0, channelManager.getChannels().length);
    } finally {
      try {
        clientComms.shutdown();
      } finally {
        serverComms.shutdown();
      }
    }
  }

  public void testTwoWayChannelOpenClose() throws Exception {
    NodeID nodeID1 = new NodeIdUuidImpl("node1");
    NodeID nodeID2 = new NodeIdUuidImpl("node2");
    
    CommunicationsManager clientComms1 = new TCGroupCommunicationsManagerImpl(monitor,
                                                                        new TCGroupNetworkStackHarnessFactory(), null,
                                                                        new NullConnectionPolicy(), 0, nodeID1);
    CommunicationsManager serverComms1 = clientComms1;
    CommunicationsManager clientComms2 = new TCGroupCommunicationsManagerImpl(monitor,
                                                                        new TCGroupNetworkStackHarnessFactory(), null,
                                                                        new NullConnectionPolicy(), 0, nodeID2);
    CommunicationsManager serverComms2 = clientComms2;
    try {
      NetworkListener lsnr1 = serverComms1.createListener(sessionManager, new TCSocketAddress(TCSocketAddress.LOOPBACK_ADDR,
                                                                                        0), true,
                                                    new DefaultConnectionIdFactory());
      lsnr1.start(new HashSet());
      ChannelManager channelManager1 = lsnr1.getChannelManager();
      assertEquals(0, channelManager1.getChannels().length);

      NetworkListener lsnr2 = serverComms2.createListener(sessionManager, new TCSocketAddress(TCSocketAddress.LOOPBACK_ADDR,
                                                                                        0), true,
                                                    new DefaultConnectionIdFactory());
      lsnr2.start(new HashSet());
      ChannelManager channelManager2 = lsnr2.getChannelManager();
      assertEquals(0, channelManager2.getChannels().length);

      ClientMessageChannel channel1;
      channel1 = clientComms1
          .createClientChannel(
                               sessionManager,
                               0,
                               TCSocketAddress.LOOPBACK_IP,
                               lsnr2.getBindPort(),
                               3000,
                               new ConnectionAddressProvider(
                                                             new ConnectionInfo[] { new ConnectionInfo(
                                                                                                       "localhost",
                                                                                                       lsnr2
                                                                                                           .getBindPort()) }));
      ClientMessageChannel channel2;
      channel2 = clientComms2
          .createClientChannel(
                               sessionManager,
                               0,
                               TCSocketAddress.LOOPBACK_IP,
                               lsnr1.getBindPort(),
                               3000,
                               new ConnectionAddressProvider(
                                                             new ConnectionInfo[] { new ConnectionInfo(
                                                                                                       "localhost",
                                                                                                       lsnr1
                                                                                                           .getBindPort()) }));

      channel1.open();
      assertTrue(channel1.isConnected());
      assertTrue(channel1.getChannelID().getNodeID().equals(nodeID2));
      assertEquals(1, channelManager2.getChannels().length);

      channel2.open();
      assertTrue(channel2.isConnected());
      assertTrue(channel2.getChannelID().getNodeID().equals(nodeID1));
      assertEquals(1, channelManager1.getChannels().length);

      MessageChannel serverChannel1 = channelManager1.getChannels()[0];
      MessageChannel serverChannel2 = channelManager2.getChannels()[0];

      System.out
          .println("Group server node name:" + ((NodeIDImpl) serverChannel1.getChannelID().getNodeID()).getName());
      System.out.println("Group client node name:" + ((NodeIDImpl) channel1.getChannelID().getNodeID()).getName());
      assertTrue(nodeID1.equals(serverChannel2.getChannelID().getNodeID()));

      System.out
          .println("Group server node name:" + ((NodeIDImpl) serverChannel2.getChannelID().getNodeID()).getName());
      System.out.println("Group client node name:" + ((NodeIDImpl) channel2.getChannelID().getNodeID()).getName());
      assertTrue(nodeID2.equals(serverChannel1.getChannelID().getNodeID()));

      clientComms1.getConnectionManager().closeAllConnections(5000);
      assertFalse(channel1.isConnected());

      clientComms2.getConnectionManager().closeAllConnections(5000);
      assertFalse(channel2.isConnected());

      for (int i = 0; i < 30; i++) {
        if (channelManager1.getChannels().length == 0) {
          if (channelManager2.getChannels().length == 0) {
            break;
          }
        }
        Thread.sleep(100);
      }

      assertEquals(0, channelManager1.getChannels().length);
      assertEquals(0, channelManager2.getChannels().length);

    } finally {
      try {
        clientComms1.shutdown();
        clientComms2.shutdown();
      } finally {
        serverComms1.shutdown();
        serverComms2.shutdown();
      }
    }
  }

}
