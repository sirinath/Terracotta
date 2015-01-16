/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.logging.TCLogger;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.protocol.NullProtocolAdaptor;

public class TestConnectionHealthCheckerContextImpl extends ConnectionHealthCheckerContextImpl {

  public TestConnectionHealthCheckerContextImpl(MessageTransportBase mtb, HealthCheckerConfig config,
                                                TCConnectionManager connMgr) {
    super(mtb, config, connMgr);
  }

  @Override
  protected TCConnection getNewConnection(TCConnectionManager connectionManager) {
    TCConnection connection = connectionManager.createConnection(new NullProtocolAdaptor());
    return connection;
  }

  @Override
  protected HealthCheckerSocketConnect getHealthCheckerSocketConnector(TCConnection connection,
                                                                       MessageTransportBase transportBase,
                                                                       TCLogger loger, HealthCheckerConfig cnfg) {

    int callbackPort = transportBase.getRemoteCallbackPort();
    if (TransportHandshakeMessage.NO_CALLBACK_PORT == callbackPort) { return new NullHealthCheckerSocketConnectImpl(); }

    TCSocketAddress sa = new TCSocketAddress(transportBase.getRemoteAddress().getAddress(), callbackPort);
    return new TestHealthCheckerSocketConnectImpl(sa, connection, transportBase.getRemoteAddress()
        .getCanonicalStringForm()
                                                                  + "(callbackport:" + callbackPort + ")", loger, cnfg
        .getSocketConnectTimeout());
  }
}
