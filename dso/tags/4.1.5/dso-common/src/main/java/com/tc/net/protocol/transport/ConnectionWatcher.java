/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

public class ConnectionWatcher implements MessageTransportListener {

  protected final ClientMessageTransport      cmt;
  protected final ClientConnectionEstablisher cce;
  protected final MessageTransportListener    target;

  /**
   * Listens to events from a MessageTransport, acts on them, and passes events through to target
   */
  public ConnectionWatcher(ClientMessageTransport cmt, MessageTransportListener target, ClientConnectionEstablisher cce) {
    this.cmt = cmt;
    this.target = target;
    this.cce = cce;
  }

  @Override
  public void notifyTransportClosed(MessageTransport transport) {
    cce.quitReconnectAttempts();
    target.notifyTransportClosed(transport);
  }

  @Override
  public void notifyTransportDisconnected(MessageTransport transport, final boolean forcedDisconnect) {
    cce.asyncReconnect(cmt);
    target.notifyTransportDisconnected(transport, forcedDisconnect);
  }

  @Override
  public void notifyTransportConnectAttempt(MessageTransport transport) {
    target.notifyTransportConnectAttempt(transport);
  }

  @Override
  public void notifyTransportConnected(MessageTransport transport) {
    target.notifyTransportConnected(transport);
  }

  @Override
  public void notifyTransportReconnectionRejected(MessageTransport transport) {
    target.notifyTransportReconnectionRejected(transport);
  }
}
