/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.logging.TCLogger;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.protocol.NullProtocolAdaptor;

public class TestConnectionHealthCheckerContextImpl extends ConnectionHealthCheckerContextImpl {

  private final TCConnectionManager  connectionManager;
  private final MessageTransportBase transport;
  private final HealthCheckerConfig  config;
  private final TCLogger             logger;

  public TestConnectionHealthCheckerContextImpl(MessageTransportBase mtb, HealthCheckerConfig config,
                                                TCConnectionManager connMgr) {
    super(mtb, config, connMgr);
    this.transport = mtb;
    this.config = config;
    this.logger = getLogger();
    this.connectionManager = connMgr;
  }

  protected TCConnection getNewConnection() {
    TCConnection connection = connectionManager.createConnection(new NullProtocolAdaptor());
    return connection;
  }

  protected HealthCheckerSocketConnect getHealthCheckerSocketConnector(TCConnection presentConnection) {

    int callbackPort = transport.getRemoteCallbackPort();
    if (TransportHandshakeMessage.NO_CALLBACK_PORT == callbackPort) { return new NullHealthCheckerSocketConnectImpl(); }

    TCConnection conn = connectionManager.createConnection(new NullProtocolAdaptor());
    TCSocketAddress sa = new TCSocketAddress(transport.getRemoteAddress().getAddress(), callbackPort);
    return new TestHealthCheckerSocketConnectImpl(sa, conn, transport.getRemoteAddress().getCanonicalStringForm(), logger,
                                              config.getSocketConnectTimeout());
  }
}
