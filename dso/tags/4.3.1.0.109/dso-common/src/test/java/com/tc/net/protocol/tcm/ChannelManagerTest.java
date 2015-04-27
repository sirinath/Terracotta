/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.protocol.tcm;

import org.mockito.Mockito;

import com.tc.license.ProductID;
import com.tc.net.ServerID;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OOONetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl;
import com.tc.net.protocol.transport.ConnectionHealthCheckerLongGCTest;
import com.tc.net.protocol.transport.DefaultConnectionIdFactory;
import com.tc.net.protocol.transport.HealthCheckerConfig;
import com.tc.net.protocol.transport.HealthCheckerConfigImpl;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.net.proxy.TCPProxy;
import com.tc.object.session.NullSessionManager;
import com.tc.properties.L1ReconnectConfigImpl;
import com.tc.properties.ReconnectConfig;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
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
                                                     @Override
                                                    public MessageChannelInternal createNewChannel(ChannelID id, final ProductID productId) {
                                                       return new ServerMessageChannelImpl(id, msgRouter, msgFactory,
                                                                                           new ServerID("test:9520",
                                                                                           new byte[] { 1, 3, 5, 7}),
                                                                                           productId);
                                                     }
                                                   };

  public void testEvents() {
    Events events = new Events();

    ChannelManagerImpl channelManager = new ChannelManagerImpl(false, channelFactory);

    // make sure things work even w/o an event listener attached
    channelManager.createNewChannel(new ChannelID(1), null);

    channelManager.addEventListener(events);
    assertEquals(0, events.channels.size());
    MessageChannelInternal c1 = channelManager.createNewChannel(new ChannelID(2), null);
    channelManager.notifyChannelEvent(new ChannelEventImpl(ChannelEventType.TRANSPORT_CONNECTED_EVENT, c1));
    c1.setSendLayer(Mockito.mock(NetworkLayer.class));
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

    MessageChannelInternal channel1 = channelManager.createNewChannel(new ChannelID(sequence++), null);
    channel1.setSendLayer(Mockito.mock(NetworkLayer.class));
    assertEquals(++channelCount, channelManager.getChannels().length);
    assertTrue(channel1.isOpen());

    MessageChannelInternal channel2 = channelManager.createNewChannel(new ChannelID(sequence++), null);
    channel2.setSendLayer(Mockito.mock(NetworkLayer.class));
    assertEquals(++channelCount, channelManager.getChannels().length);
    assertTrue(channel2.isOpen());

    MessageChannelInternal channel3 = channelManager.createNewChannel(new ChannelID(sequence++), null);
    channel3.setSendLayer(Mockito.mock(NetworkLayer.class));
    assertEquals(++channelCount, channelManager.getChannels().length);
    assertTrue(channel3.isOpen());

    // Make sure that sending a transport disconnected event to the channel does
    // NOT remove the channel from the
    // channel manager.
    channel1.notifyTransportDisconnected(null, false);
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
    CommunicationsManager clientComms = new CommunicationsManagerImpl("TestCommsMgr-Client-1", monitor,
                                                                      new PlainNetworkStackHarnessFactory(),
                                                                      new NullConnectionPolicy(), 0);
    CommunicationsManager serverComms = new CommunicationsManagerImpl("TestCommsMgr-Server-1", monitor,
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
          .createClientChannel(sessionManager,
                               0,
                               TCSocketAddress.LOOPBACK_IP,
                               lsnr.getBindPort(),
                               3000,
                               new ConnectionAddressProvider(
                                                             new ConnectionInfo[] { new ConnectionInfo(
                                                                                                       "localhost",
                                                                                                       lsnr.getBindPort()) }));
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

  public void testChannelRemoteAddressAcrossReconnect() throws Exception {
    ReconnectConfig reconnectCoinfig = new L1ReconnectConfigImpl(true, 30000, 5000, 16, 32);
    NetworkStackHarnessFactory networkStackHarnessFactory = new OOONetworkStackHarnessFactory(
                                                                                              new OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl(),
                                                                                              reconnectCoinfig);

    CommunicationsManager clientComms = new CommunicationsManagerImpl("TestCommsMgr-Client-2", monitor,
                                                                      networkStackHarnessFactory,
                                                                      new NullConnectionPolicy(),
                                                                      new HealthCheckerConfigImpl(TCPropertiesImpl
                                                                          .getProperties()
                                                                          .getPropertiesFor("l1.healthcheck.l2"),
                                                                                                  "Test Client HC"));
    CommunicationsManager serverComms = new CommunicationsManagerImpl("TestCommsMgr-Server-2", monitor,
                                                                      networkStackHarnessFactory,
                                                                      new NullConnectionPolicy(),
                                                                      new HealthCheckerConfigImpl(TCPropertiesImpl
                                                                          .getProperties()
                                                                          .getPropertiesFor("l2.healthcheck.l1"),
                                                                                                  "Test Server HC"));
    try {
      NetworkListener lsnr = serverComms.createListener(sessionManager,
                                                        new TCSocketAddress(TCSocketAddress.LOOPBACK_ADDR, 0), false,
                                                        new DefaultConnectionIdFactory());
      lsnr.start(new HashSet());
      ChannelManager serverChannelManager = lsnr.getChannelManager();
      assertEquals(0, serverChannelManager.getChannels().length);

      ClientMessageChannel clientChannel;
      clientChannel = clientComms
          .createClientChannel(sessionManager,
                               -1,
                               TCSocketAddress.LOOPBACK_IP,
                               lsnr.getBindPort(),
                               3000,
                               new ConnectionAddressProvider(
                                                             new ConnectionInfo[] { new ConnectionInfo(
                                                                                                       "localhost",
                                                                                                       lsnr.getBindPort()) }));
      clientChannel.open();

      while (!clientChannel.isConnected()) {
        System.out.println("XXX waiting for client connect");
        ThreadUtil.reallySleep(1000);
      }

      while (!serverChannelManager.getChannels()[0].isConnected()) {
        System.out.println("XXX 1waiting for server to accept client fully");
        ThreadUtil.reallySleep(1000);
      }

      TCSocketAddress serverLocalAddress1 = serverChannelManager.getChannels()[0].getLocalAddress();
      TCSocketAddress serverRemoteAddress1 = serverChannelManager.getChannels()[0].getRemoteAddress();
      TCSocketAddress clientLocalAddress1 = clientChannel.getLocalAddress();
      TCSocketAddress clientRemoteAddress1 = clientChannel.getRemoteAddress();

      System.out.println("XXX 1Server Address : " + serverLocalAddress1 + " / " + serverRemoteAddress1);
      System.out.println("XXX 1Client Address : " + clientLocalAddress1 + " / " + clientRemoteAddress1);

      // OnceAndOnlyOnceProtocolNetworkLayerImpl oooLayer = (OnceAndOnlyOnceProtocolNetworkLayerImpl)
      // (((AbstractMessageChannel) serverChannelManager
      // .getChannels()[0]).getSendLayer());
      // oooLayer.setNewSessionID();
      serverComms.getConnectionManager().closeAllConnections(5000);

      ThreadUtil.reallySleep(reconnectCoinfig.getReconnectTimeout() + 10000);

      while (!clientChannel.isConnected()) {
        System.out.println("XXX 2waiting for client connect");
        ThreadUtil.reallySleep(1000);
      }

      while (!serverChannelManager.getChannels()[0].isConnected()) {
        System.out.println("XXX 2waiting for server to accept client fully");
        ThreadUtil.reallySleep(1000);
      }

      TCSocketAddress serverLocalAddress2 = serverChannelManager.getChannels()[0].getLocalAddress();
      TCSocketAddress serverRemoteAddress2 = serverChannelManager.getChannels()[0].getRemoteAddress();
      TCSocketAddress clientLocalAddress2 = clientChannel.getLocalAddress();
      TCSocketAddress clientRemoteAddress2 = clientChannel.getRemoteAddress();

      System.out.println("XXX 2Server Address : " + serverLocalAddress2 + " / " + serverRemoteAddress2);
      System.out.println("XXX 2Client Address : " + clientLocalAddress2 + " / " + clientRemoteAddress2);

      Assert.assertEquals(serverLocalAddress1, serverLocalAddress2);
      Assert.eval(!serverRemoteAddress1.equals(serverRemoteAddress2));

      Assert.eval(!clientLocalAddress1.equals(clientLocalAddress2));
      Assert.assertEquals(clientRemoteAddress1, clientRemoteAddress2);

    } finally {
      try {
        clientComms.shutdown();
      } finally {
        serverComms.shutdown();
      }
    }

  }

  public void testClientSkipRestoreConnection() throws Exception {

    NetworkStackHarnessFactory networkStackHarnessFactory = new OOONetworkStackHarnessFactory(
                                                                                              new OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl(),
                                                                                              new L1ReconnectConfigImpl(
                                                                                                                        true,
                                                                                                                        120000,
                                                                                                                        5000,
                                                                                                                        16,
                                                                                                                        32));

    HealthCheckerConfig hcConfig = new HealthCheckerConfigImpl(5000, 1000, 3, "Client-HC", false);
    CommunicationsManager clientComms = new CommunicationsManagerImpl("TestCommsMgr-Client-3", monitor,
                                                                      networkStackHarnessFactory,
                                                                      new NullConnectionPolicy(), hcConfig);
    CommunicationsManager serverComms = new CommunicationsManagerImpl("TestCommsMgr-Server-3", monitor,
                                                                      networkStackHarnessFactory,
                                                                      new NullConnectionPolicy(), 0);
    try {
      NetworkListener lsnr = serverComms.createListener(sessionManager,
                                                        new TCSocketAddress(TCSocketAddress.LOOPBACK_ADDR, 0), true,
                                                        new DefaultConnectionIdFactory());
      lsnr.start(new HashSet());
      ChannelManager channelManager = lsnr.getChannelManager();
      assertEquals(0, channelManager.getChannels().length);

      int serverPort = lsnr.getBindPort();
      int proxyPort = new PortChooser().chooseRandomPort();
      TCPProxy proxy = new TCPProxy(proxyPort, lsnr.getBindAddress(), serverPort, 0, false, null);
      proxy.start();

      ClientMessageChannel channel;
      channel = clientComms
          .createClientChannel(sessionManager, -1, TCSocketAddress.LOOPBACK_IP, proxyPort, 3000,
                               new ConnectionAddressProvider(new ConnectionInfo[] { new ConnectionInfo("localhost",
                                                                                                       proxyPort) }));
      channel.open();

      while (!channelManager.getChannels()[0].isConnected()) {
        System.out.println("waiting for server to send final Tx ACK for client connection");
        ThreadUtil.reallySleep(1000);
      }

      while (!channel.isConnected()) {
        System.out.println("waiting for client channel connect");
        ThreadUtil.reallySleep(1000);
      }

      proxy.setDelay(100 * 1000);

      long sleepTime = ConnectionHealthCheckerLongGCTest.getMinSleepTimeToStartLongGCTest(hcConfig)
                       + ConnectionHealthCheckerLongGCTest
                           .getMinScoketConnectResultTimeAfterSocketConnectStart(hcConfig);

      System.err.println("XXX sleeping for " + sleepTime);
      ThreadUtil.reallySleep(sleepTime);
      System.err.println("XXX proxy set delay 0");
      proxy.setDelay(0);

      ThreadUtil.reallySleep(30000);
      while (channel.isConnected()) {
        ThreadUtil.reallySleep(1000);
        System.err.println("XXX Client is still connected");
      }
      assertEquals(0, channelManager.getChannels().length);

      channel.close();
    } finally {
      clientComms.shutdown();
      serverComms.shutdown();
    }
  }

  public void testServerSkipOpenReconnectWindow() throws Exception {

    NetworkStackHarnessFactory networkStackHarnessFactory = new OOONetworkStackHarnessFactory(
                                                                                              new OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl(),
                                                                                              new L1ReconnectConfigImpl(
                                                                                                                        true,
                                                                                                                        120000,
                                                                                                                        5000,
                                                                                                                        16,
                                                                                                                        32));

    HealthCheckerConfig hcConfig = new HealthCheckerConfigImpl(5000, 1000, 3, "Client-HC", false);
    CommunicationsManager clientComms = new CommunicationsManagerImpl("TestCommsMgr-Client-4", monitor,
                                                                      networkStackHarnessFactory,
                                                                      new NullConnectionPolicy(), 0);
    CommunicationsManager serverComms = new CommunicationsManagerImpl("TestCommsMgr-Server-4", monitor,
                                                                      networkStackHarnessFactory,
                                                                      new NullConnectionPolicy(), hcConfig);
    try {
      NetworkListener lsnr = serverComms.createListener(sessionManager,
                                                        new TCSocketAddress(TCSocketAddress.LOOPBACK_ADDR, 0), true,
                                                        new DefaultConnectionIdFactory());
      lsnr.start(new HashSet());
      ChannelManager channelManager = lsnr.getChannelManager();
      assertEquals(0, channelManager.getChannels().length);

      int serverPort = lsnr.getBindPort();
      int proxyPort = new PortChooser().chooseRandomPort();
      TCPProxy proxy = new TCPProxy(proxyPort, lsnr.getBindAddress(), serverPort, 0, false, null);
      proxy.start();

      ClientMessageChannel channel;
      channel = clientComms
          .createClientChannel(sessionManager, -1, TCSocketAddress.LOOPBACK_IP, proxyPort, 3000,
                               new ConnectionAddressProvider(new ConnectionInfo[] { new ConnectionInfo("localhost",
                                                                                                       proxyPort) }));
      channel.open();

      while (!channelManager.getChannels()[0].isConnected()) {
        System.out.println("waiting for server to send final Tx ACK for client connection");
        ThreadUtil.reallySleep(1000);
      }

      while (!channel.isConnected()) {
        System.out.println("waiting for client channel connected");
        ThreadUtil.reallySleep(1000);
      }

      proxy.setDelay(100 * 1000);

      long sleepTime = ConnectionHealthCheckerLongGCTest.getMinSleepTimeToStartLongGCTest(hcConfig)
                       + ConnectionHealthCheckerLongGCTest
                           .getMinScoketConnectResultTimeAfterSocketConnectStart(hcConfig);

      System.out.println("XXX sleeping for " + sleepTime);
      ThreadUtil.reallySleep(sleepTime);
      proxy.setDelay(0);

      proxy.closeClientConnections(true, false);
      ThreadUtil.reallySleep(15000);
      assertEquals(false, channel.isConnected());
      assertEquals(0, channelManager.getChannels().length);

    } finally {
      clientComms.shutdown();
      serverComms.shutdown();
    }
  }

  static class Events implements ChannelManagerEventListener {
    Set channels = new HashSet();

    @Override
    public void channelCreated(MessageChannel channel) {
      channels.add(channel);
    }

    @Override
    public void channelRemoved(MessageChannel channel) {
      channels.remove(channel);
    }
  }

}
