/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.bytes.TCByteBuffer;
import com.tc.exception.ImplementMe;
import com.tc.net.ServerID;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.transport.DefaultConnectionIdFactory;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.object.session.NullSessionManager;
import com.tc.util.concurrent.ThreadUtil;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

public class ChannelManagerTest extends TestCase {

  MessageMonitor                    monitor        = new NullMessageMonitor();
  final NullSessionManager          sessionManager = new NullSessionManager();
  final TCMessageFactory            msgFactory     = new TCMessageFactoryImpl(sessionManager, monitor);
  final TCMessageRouter             msgRouter      = new TCMessageRouterImpl();

  final ServerMessageChannelFactory channelFactory = new ServerMessageChannelFactory() {
                                                     public MessageChannelInternal createNewChannel(ChannelID id) {
                                                       return new ServerMessageChannelImpl(id, msgRouter, msgFactory,
                                                                                           new ServerID("test:9520",
                                                                                                        new byte[] { 1,
      3, 5, 7                                                                                          }));
                                                     }
                                                   };

  public void testEvents() {
    Events events = new Events();

    ChannelManagerImpl channelManager = new ChannelManagerImpl(false, channelFactory);

    // make sure things work even w/o an event listener attached
    channelManager.createNewChannel(new ChannelID(1));

    channelManager.addEventListener(events);
    assertEquals(0, events.channels.size());
    MessageChannelInternal c1 = channelManager.createNewChannel(new ChannelID(2));
    channelManager.notifyChannelEvent(new ChannelEventImpl(ChannelEventType.TRANSPORT_CONNECTED_EVENT, c1));
    c1.setSendLayer(new NullNetworkLayer());
    assertEquals(1, events.channels.size());
    assertTrue(events.channels.contains(c1));
    c1.close();
    assertEquals(0, events.channels.size());

    try {
      channelManager.addEventListener(null);
      fail();
    } catch (IllegalArgumentException iae) {
      // expected
    }
  }

  public void testCreateChannel() throws Exception {
    ChannelManagerImpl channelManager = new ChannelManagerImpl(false, channelFactory);

    int channelCount = 0;
    long sequence = 1;

    MessageChannelInternal channel1 = channelManager.createNewChannel(new ChannelID(sequence++));
    channel1.setSendLayer(new NullNetworkLayer());
    assertEquals(++channelCount, channelManager.getChannels().length);
    assertTrue(channel1.isOpen());

    MessageChannelInternal channel2 = channelManager.createNewChannel(new ChannelID(sequence++));
    channel2.setSendLayer(new NullNetworkLayer());
    assertEquals(++channelCount, channelManager.getChannels().length);
    assertTrue(channel2.isOpen());

    MessageChannelInternal channel3 = channelManager.createNewChannel(new ChannelID(sequence++));
    channel3.setSendLayer(new NullNetworkLayer());
    assertEquals(++channelCount, channelManager.getChannels().length);
    assertTrue(channel3.isOpen());

    // Make sure that sending a transport disconnected event to the channel does
    // NOT remove the channel from the
    // channel manager.
    channel1.notifyTransportDisconnected(null);
    assertEquals(channelCount, channelManager.getChannels().length);
    assertFalse(channel1.isClosed());

    // this is the same as the test just above, but more explicitly excercising
    // the ChannelManager event notification
    // interface.
    channelManager.notifyChannelEvent(new ChannelEventImpl(ChannelEventType.TRANSPORT_DISCONNECTED_EVENT, channel2));
    assertEquals(channelCount, channelManager.getChannels().length);
    assertFalse(channel2.isClosed());

    // Make sure that closing the channel causes it to be removed from the
    // channel manager.
    channel3.close();
    assertEquals(--channelCount, channelManager.getChannels().length);
    assertTrue(channel3.isClosed());
  }

  public void testTransportDisconnectRemovesChannel() throws Exception {
    CommunicationsManager clientComms = new CommunicationsManagerImpl("TestCommsMgr-Client", monitor,
                                                                      new PlainNetworkStackHarnessFactory(),
                                                                      new NullConnectionPolicy(), 0);
    CommunicationsManager serverComms = new CommunicationsManagerImpl("TestCommsMgr-Server", monitor,
                                                                      new PlainNetworkStackHarnessFactory(),
                                                                      new NullConnectionPolicy(), 0);
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

      while (!channelManager.getChannels()[0].isConnected()) {
        System.out.println("waiting for server to send final Tx ACK for client connection");
        ThreadUtil.reallySleep(1000);
      }
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

  static class Events implements ChannelManagerEventListener {
    Set channels = new HashSet();

    public void channelCreated(MessageChannel channel) {
      channels.add(channel);
    }

    public void channelRemoved(MessageChannel channel) {
      channels.remove(channel);
    }
  }

  static class NullNetworkLayer implements NetworkLayer {
    public void setSendLayer(NetworkLayer layer) {
      return;
    }

    public void setReceiveLayer(NetworkLayer layer) {
      return;
    }

    public void send(TCNetworkMessage message) {
      return;
    }

    public void receive(TCByteBuffer[] msgData) {
      return;
    }

    public boolean isConnected() {
      return false;
    }

    public NetworkStackID open() {
      return null;
    }

    public void close() {
      return;
    }

    public short getStackLayerFlag() {
      // its a test
      // do nothing
      throw new ImplementMe();
    }

    public String getStackLayerName() {
      throw new ImplementMe();
    }

    public NetworkLayer getReceiveLayer() {
      throw new ImplementMe();
    }
  }

}
