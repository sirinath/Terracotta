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
      assertTrue(channel.getChannelID().getNodeID().equals(nodeID1));

      assertEquals(1, channelManager.getChannels().length);
      
      MessageChannel serverChannel = channelManager.getChannels()[0];
      System.out.println("Group server node name:"+((NodeIDImpl)serverChannel.getChannelID().getNodeID()).getName());
      System.out.println("Group client node name:"+((NodeIDImpl)channel.getChannelID().getNodeID()).getName());
      assertTrue(channel.getChannelID().getNodeID().equals(serverChannel.getChannelID().getNodeID()));
      
      
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

}
