/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.async.api.Sink;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.net.protocol.transport.MessageTransportFactory;
import com.tc.net.protocol.transport.WireProtocolMessageSink;
import com.tc.object.session.SessionProvider;

/**
 * CommsMgr provides Listener and Channel endpoints for exchanging <code>TCMessage</code> type messages
 */
public interface CommunicationsManager {
  public TCConnectionManager getConnectionManager();

  public void shutdown();

  public boolean isInShutdown();

  public NetworkListener[] getAllListeners();

  /**
   * Creates a client message channel to the given host/port.
   *
   * @param maxReconnectTries The number of times the channel will attempt to reestablish communications with the server
   *        if the connection is lost. If n==0, the channel will not attempt to reestablish communications. If n>0, the
   *        channel will attempt to reestablish communications n times. If n<0 the channel will always try to
   *        reestablish communications.
   * @param hostname The hostname to connect to.
   * @param port The remote port to connect to.
   * @param timeout The maximum time (in milliseconds) to wait for the underlying connection to be established before
   *        giving up.
   */
  public ClientMessageChannel createClientChannel(SessionProvider sessionProvider, final int maxReconnectTries,
                                                  String hostname, int port, final int timeout,
                                                  ConnectionAddressProvider addressProvider, int callbackPort);

  public ClientMessageChannel createClientChannel(SessionProvider sessionProvider, final int maxReconnectTries,
                                                  String hostname, int port, final int timeout,
                                                  ConnectionAddressProvider addressProvider);

  public ClientMessageChannel createClientChannel(SessionProvider sessionProvider, final int maxReconnectTries,
                                                  String hostname, int port, final int timeout,
                                                  ConnectionAddressProvider addressProvider,
                                                  MessageTransportFactory transportFactory);

  public NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress addr,
                                        boolean transportDisconnectRemovesChannel,
                                        ConnectionIDFactory connectionIdFactory);

  public NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress addr,
                                        boolean transportDisconnectRemovesChannel,
                                        ConnectionIDFactory connectionIdFactory,
                                        WireProtocolMessageSink wireProtoMsgSink);

  public NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress addr,
                                        boolean transportDisconnectRemovesChannel,
                                        ConnectionIDFactory connectionIdFactory, boolean reuseAddress);

  public NetworkListener createListener(SessionProvider sessionProvider, TCSocketAddress address,
                                        boolean transportDisconnectRemovesChannel,
                                        ConnectionIDFactory connectionIDFactory, Sink httpSink);
}