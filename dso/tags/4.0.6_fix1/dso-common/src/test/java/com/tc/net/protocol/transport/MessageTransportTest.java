/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.tc.exception.ImplementMe;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.core.MockConnectionManager;
import com.tc.net.core.MockTCConnection;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageRouterImpl;
import com.tc.object.session.NullSessionManager;
import com.tc.test.TCTestCase;
import com.tc.util.TCAssertionError;

import java.util.Collections;

/**
 * Test case for MessageTransportImpl
 */
public class MessageTransportTest extends TCTestCase {
  private SynchronizedRef                  clientErrorRef;
  private SynchronizedRef                  serverErrorRef;
  private ClientHandshakeMessageResponder  clientResponder;
  private ServerHandshakeMessageResponder  serverResponder;
  private LinkedQueue                      clientResponderReceivedQueue;
  private LinkedQueue                      clientResponderSentQueue;
  private LinkedQueue                      serverResponderReceivedQueue;
  private LinkedQueue                      serverResponderSentQueue;
  private TransportEventMonitor            clientEventMonitor;
  private TransportEventMonitor            serverEventMonitor;
  private ClientMessageTransport           clientTransport;
  private ServerMessageTransport           serverTransport;
  private ConnectionID                     connectionId;
  private MockTCConnection                 clientConnection;
  private MockConnectionManager            connManager;
  private CommunicationsManagerImpl        commsManager;
  private NetworkListener                  lsnr;
  private TransportHandshakeMessageFactory transportHandshakeMessageFactory;
  private MockTCConnection                 serverConnection;

  @Override
  public void setUp() throws Exception {
    this.clientResponderReceivedQueue = new LinkedQueue();
    this.clientResponderSentQueue = new LinkedQueue();
    this.serverResponderReceivedQueue = new LinkedQueue();
    this.serverResponderSentQueue = new LinkedQueue();
    this.clientErrorRef = new SynchronizedRef(null);
    this.serverErrorRef = new SynchronizedRef(null);
    DefaultConnectionIdFactory connectionIDProvider = new DefaultConnectionIdFactory();
    this.connectionId = connectionIDProvider.nextConnectionId(JvmIDUtil.getJvmID());

    this.transportHandshakeMessageFactory = new TransportMessageFactoryImpl();
    serverConnection = new MockTCConnection();
    serverConnection.isConnected(true);

    clientConnection = new MockTCConnection();
    clientConnection.remoteAddress = new TCSocketAddress("localhost", 0);
    clientConnection.localAddress = new TCSocketAddress("localhost", 0);
    connManager = new MockConnectionManager();
    connManager.setConnection(clientConnection);
    commsManager = new CommunicationsManagerImpl("TestCommsMgr", new NullMessageMonitor(), new TCMessageRouterImpl(),
                                                 new PlainNetworkStackHarnessFactory(), connManager,
                                                 new NullConnectionPolicy(), 0, new DisabledHealthCheckerConfigImpl(),
                                                 new TransportHandshakeErrorNullHandler(), Collections.EMPTY_MAP,
                                                 Collections.EMPTY_MAP, null);
    lsnr = commsManager.createListener(new NullSessionManager(), new TCSocketAddress(0), true,
                                       new DefaultConnectionIdFactory());
    lsnr.start(Collections.EMPTY_SET);
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    assertTrue(this.clientErrorRef.get() == null);
    assertTrue(this.serverErrorRef.get() == null);
    lsnr.stop(5000);
    commsManager.shutdown();
  }

  public void testDoubleOpen() throws Exception {

    createClientTransport(0);
    createServerTransport();

    assertFalse(clientTransport.wasOpened());

    // make sure that when you open two different transports, the mock
    // connection manager
    // records two create connection requests.

    clientTransport.open();
    createClientTransport(0);
    clientTransport.open();
    assertEquals(2, connManager.getCreateConnectionCallCount());

    // now open the same transport again and make sure it throws an assertion
    // error.
    try {
      clientTransport.open();
      fail("Should have thrown an assertion error.");
    } catch (TCAssertionError e) {
      // expected.
    }
    assertEquals(2, connManager.getCreateConnectionCallCount());
  }

  public void testDoubleClose() throws Exception {
    createClientTransport(0);

    clientTransport.open();
    clientTransport.close();
    assertEquals(1, clientConnection.getCloseCallCount());

    try {
      clientTransport.close();
    } catch (TCAssertionError e) {
      fail("Should not have thrown an AssertionError");
    }
  }

  public void testClientTransportEvents() throws Exception {
    createClientTransport(1);

    // add an extra event monitor to make sure that, if there are multiple
    // listeners,
    // they all get the event.
    TransportEventMonitor extraMonitor = new TransportEventMonitor();
    this.clientTransport.addTransportListener(extraMonitor);

    clientTransport.open();
    assertTrue(clientEventMonitor.waitForConnect(1000));
    assertTrue(extraMonitor.waitForConnect(1000));

    TCConnectionEvent event = new TCConnectionEvent(clientConnection);

    clientTransport.closeEvent(event);
    assertTrue(clientEventMonitor.waitForDisconnect(1000));
    assertTrue(extraMonitor.waitForDisconnect(1000));
    assertTrue(clientEventMonitor.waitForConnectAttempt(1000));
    assertTrue(extraMonitor.waitForConnectAttempt(1000));

    clientTransport.close();
    assertTrue(clientEventMonitor.waitForClose(1000));
    assertTrue(extraMonitor.waitForClose(1000));
  }

  public void testServerTransportEvents() throws Exception {
    createServerTransport();
    assertFalse(serverEventMonitor.waitForConnect(500));

    // to establish connection, the status checked at closing
    TransportHandshakeMessage ack = this.transportHandshakeMessageFactory.createAck(connectionId, this.serverTransport
        .getConnection());
    this.serverTransport.receiveTransportMessage(ack);

    // add an extra event monitor to make sur ethat, if there are multiple
    // listeners,
    // they all get the event.
    TransportEventMonitor extraMonitor = new TransportEventMonitor();
    serverTransport.addTransportListener(extraMonitor);

    TCConnectionEvent event = new TCConnectionEvent(serverConnection);

    serverTransport.closeEvent(event);
    assertTrue(serverEventMonitor.waitForDisconnect(1000));
    assertTrue(extraMonitor.waitForDisconnect(1000));

    assertFalse(serverEventMonitor.waitForConnectAttempt(1000));
    assertFalse(extraMonitor.waitForConnectAttempt(1000));

    serverTransport.close();
    assertTrue(serverEventMonitor.waitForClose(1000));
    assertTrue(extraMonitor.waitForClose(1000));
  }

  public void testCloseBeforeOpen() throws Exception {
    createClientTransport(0);
    clientTransport.close();
  }

  public void testWasOpened() throws Exception {
    createClientTransport(0);
    assertFalse(clientTransport.wasOpened());

    clientTransport.open();
    assertTrue(clientTransport.wasOpened());

    clientTransport.close();
    assertTrue(clientTransport.wasOpened());
  }

  public void testOpenCloseAndIsConnected() throws Exception {
    createClientTransport(0);
    assertFalse(clientTransport.isConnected());

    clientTransport.open();
    assertTrue(clientTransport.isConnected());

    clientTransport.close();
    assertTrue(clientEventMonitor.waitForClose(1000));
    assertFalse(clientTransport.isOpen.get());

    createServerTransport();
    TransportHandshakeMessage ack = this.transportHandshakeMessageFactory.createAck(connectionId, this.serverTransport
        .getConnection());
    this.serverTransport.receiveTransportMessage(ack);
    assertTrue(serverEventMonitor.waitForConnect(1000));
    assertTrue(serverTransport.isConnected());
  }

  public void testTransportConnectedScope() throws Exception {
    HoldsStatusLockListener listener = new HoldsStatusLockListener();
    createServerTransport();
    serverTransport.addTransportListener(listener);
    TransportHandshakeMessage ack = this.transportHandshakeMessageFactory.createAck(connectionId, this.serverTransport
        .getConnection());
    this.serverTransport.receiveTransportMessage(ack);
    assertFalse(listener.holdsStatusLock);
  }

  private class HoldsStatusLockListener implements MessageTransportListener {
    boolean holdsStatusLock = false;

    @Override
    public void notifyTransportConnected(MessageTransport transport) {
      holdsStatusLock = Thread.holdsLock(serverTransport.status);
    }

    @Override
    public void notifyTransportDisconnected(MessageTransport transport, boolean forcedDisconnect) {
      //
    }

    @Override
    public void notifyTransportConnectAttempt(MessageTransport transport) {
      //
    }

    @Override
    public void notifyTransportClosed(MessageTransport transport) {
      //
    }

    @Override
    public void notifyTransportReconnectionRejected(MessageTransport transport) {
      //
    }
  }

  private void createServerTransport() throws Exception {
    this.serverTransport = new ServerMessageTransport(this.connectionId, this.serverConnection,
                                                      createHandshakeErrorHandler(),
                                                      this.transportHandshakeMessageFactory);

    this.serverResponder = new ServerHandshakeMessageResponder(this.serverResponderSentQueue,
                                                               this.serverResponderReceivedQueue,
                                                               this.transportHandshakeMessageFactory,
                                                               this.connectionId, serverTransport, this.serverErrorRef);
    this.serverConnection.setMessageSink(this.serverResponder);
    this.serverEventMonitor = new TransportEventMonitor();
    this.serverTransport.addTransportListener(this.serverEventMonitor);
  }

  private void createClientTransport(int maxReconnectTries) throws Exception {
    final ConnectionInfo connInfo = new ConnectionInfo(TCSocketAddress.LOOPBACK_IP, 0);
    final ClientConnectionEstablisher cce = new ClientConnectionEstablisher(
                                                                            connManager,
                                                                            new ConnectionAddressProvider(
                                                                                                          new ConnectionInfo[] { connInfo }),
                                                                            maxReconnectTries, 0,
                                                                            ReconnectionRejectedHandlerL1.SINGLETON);

    this.clientTransport = new ClientMessageTransport(cce, createHandshakeErrorHandler(),
                                                      this.transportHandshakeMessageFactory,
                                                      new WireProtocolAdaptorFactoryImpl(),
                                                      TransportHandshakeMessage.NO_CALLBACK_PORT);
    this.clientResponder = new ClientHandshakeMessageResponder(this.clientResponderSentQueue,
                                                               this.clientResponderReceivedQueue,
                                                               this.transportHandshakeMessageFactory,
                                                               this.connectionId, this.clientTransport,
                                                               this.clientErrorRef);
    this.clientConnection.setMessageSink(this.clientResponder);
    this.clientEventMonitor = new TransportEventMonitor();
    this.clientTransport.addTransportListener(this.clientEventMonitor);
  }

  private TransportHandshakeErrorHandler createHandshakeErrorHandler() {
    return new TransportHandshakeErrorHandler() {

      @Override
      public void handleHandshakeError(TransportHandshakeErrorContext e) {
        new ImplementMe(e.toString()).printStackTrace();
      }

    };
  }

}
