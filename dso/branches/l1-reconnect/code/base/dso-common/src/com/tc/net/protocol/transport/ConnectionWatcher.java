/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

public class ConnectionWatcher implements MessageTransportListener {

  private final ClientMessageTransport      cmt;
  private final ClientConnectionEstablisher cce;

  public ConnectionWatcher(ClientMessageTransport cmt, ClientConnectionEstablisher cce) {
    this.cmt = cmt;
    this.cce = cce;
  }

  public void notifyTransportClosed(MessageTransport transport) {
    cce.quitReconnectAttempts();
  }

  public void notifyTransportDisconnected(MessageTransport transport) {
    this.cce.asyncReconnect(cmt);
  }

  public void notifyTransportConnectAttempt(MessageTransport transport) {
    // no-op
  }

  public void notifyTransportConnected(MessageTransport transport) {
    // no-op
  }
}
