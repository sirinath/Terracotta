/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.async.api.Sink;
import com.tc.async.impl.NullSink;
import com.tc.logging.TCLogging;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.Constants;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.core.TCConnectionManagerJDK14;
import com.tc.net.core.TCListener;
import com.tc.net.groups.NodeID;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.MessageTransportFactory;
import com.tc.net.protocol.transport.ServerMessageTransport;
import com.tc.net.protocol.transport.ServerStackProvider;
import com.tc.net.protocol.transport.TransportHandshakeErrorHandler;
import com.tc.net.protocol.transport.TransportHandshakeMessageFactory;
import com.tc.net.protocol.transport.TransportHandshakeMessageFactoryImpl;
import com.tc.net.protocol.transport.WireProtocolAdaptorFactoryImpl;
import com.tc.net.protocol.transport.WireProtocolMessageSink;
import com.tc.object.session.SessionProvider;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Communications manager for setting up listners and creating client connections
 * 
 * @author teck
 */
public class TCGroupCommunicationsManagerImpl extends CommunicationsManagerImpl {
  private final TCConnectionManager              connectionManager;
  private final NetworkStackHarnessFactory       stackHarnessFactory;
  private final TransportHandshakeMessageFactory transportHandshakeMessageFactory;
  private final ConnectionPolicy                 connectionPolicy;

  // for TC-Group_Comm to exchange NodeID at handshaking
  public TCGroupCommunicationsManagerImpl(MessageMonitor monitor, NetworkStackHarnessFactory stackHarnessFactory,
                                          TCConnectionManager connMgr, ConnectionPolicy connectionPolicy,
                                          int workerCommCount, NodeID nodeID) {

    super(monitor, stackHarnessFactory, connMgr, connectionPolicy, workerCommCount);
    this.connectionPolicy = connectionPolicy;
    this.stackHarnessFactory = stackHarnessFactory;
    this.transportHandshakeMessageFactory = new TransportHandshakeMessageFactoryImpl(nodeID);
    if (null == connMgr) {
      this.connectionManager = new TCConnectionManagerJDK14(workerCommCount);
    } else {
      this.connectionManager = connMgr;
    }
  }

  /**
   * Creates a network listener with a default network stack.
   */
  public NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress addr,
                                        boolean transportDisconnectRemovesChannel,
                                        ConnectionIDFactory connectionIdFactory) {
    return createListener(sessionProvider, addr, transportDisconnectRemovesChannel, connectionIdFactory, true, new NullSink(), null);
  }

  /**
   * Creates a network listener with a default network stack.
   */
  protected NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress addr,
                                           boolean transportDisconnectRemovesChannel,
                                           ConnectionIDFactory connectionIdFactory, boolean reuseAddr, Sink httpSink,
                                           WireProtocolMessageSink wireProtoMsgSnk) {
    return super.createListener(sessionProvider, addr, transportDisconnectRemovesChannel, connectionIdFactory,
                                reuseAddr, httpSink, wireProtoMsgSnk);

  }

  TCListener createCommsListener(TCSocketAddress addr, final ServerMessageChannelFactory channelFactory,
                                 boolean resueAddr, Set initialConnectionIDs, ConnectionIDFactory connectionIdFactory,
                                 Sink httpSink, WireProtocolMessageSink wireProtocolMessageSink) throws IOException {

    MessageTransportFactory transportFactory = new MessageTransportFactory() {

      public MessageTransport createNewTransport() {
        throw new AssertionError();
      }

      public MessageTransport createNewTransport(ConnectionID connectionID, TransportHandshakeErrorHandler handler,
                                                 TransportHandshakeMessageFactory handshakeMessageFactory,
                                                 List transportListeners) {
        MessageTransport rv = new ServerMessageTransport(connectionID, handler, handshakeMessageFactory);
        rv.addTransportListeners(transportListeners);
        return rv;
      }

      public MessageTransport createNewTransport(ConnectionID connectionId, TCConnection connection,
                                                 TransportHandshakeErrorHandler handler,
                                                 TransportHandshakeMessageFactory handshakeMessageFactory,
                                                 List transportListeners) {
        MessageTransport rv = new ServerMessageTransport(connectionId, connection, handler, handshakeMessageFactory);
        rv.addTransportListeners(transportListeners);
        return rv;
      }

    };

    ServerStackProvider stackProvider = new ServerStackProvider(
                                                                              TCLogging
                                                                                  .getLogger(ServerStackProvider.class),
                                                                              initialConnectionIDs,
                                                                              stackHarnessFactory,
                                                                              channelFactory,
                                                                              transportFactory,
                                                                              this.transportHandshakeMessageFactory,
                                                                              connectionIdFactory,
                                                                              this.connectionPolicy,
                                                                              new WireProtocolAdaptorFactoryImpl(
                                                                                                                 httpSink),
                                                                              wireProtocolMessageSink);
    return connectionManager.createListener(addr, stackProvider, Constants.DEFAULT_ACCEPT_QUEUE_DEPTH, resueAddr);
  }

}
