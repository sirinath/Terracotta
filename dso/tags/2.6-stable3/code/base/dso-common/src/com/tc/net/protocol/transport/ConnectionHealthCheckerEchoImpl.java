/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

/**
 * Default Health Checker which is tied to the communications manager. All it does is, attaching a ECHO context to the
 * ESTABLISHED TC Connection
 * 
 * @author Manoj
 */
public class ConnectionHealthCheckerEchoImpl implements ConnectionHealthChecker {
  private MessageTransportBase transportBase;

  public void start() {
    // keep mum
  }

  public void stop() {
    // keep mum
  }

  public void notifyTransportClosed(MessageTransport transport) {
    // who cares
  }

  public void notifyTransportConnectAttempt(MessageTransport transport) {
    // who cares
  }

  public void notifyTransportConnected(MessageTransport transport) {
    this.transportBase = (MessageTransportBase) transport;
    ConnectionHealthCheckerContext context = new ConnectionHealthCheckerContextEchoImpl(transportBase);
    transportBase.setHealthCheckerContext(context);
  }

  public void notifyTransportDisconnected(MessageTransport transport) {
    // who cares
  }

}
