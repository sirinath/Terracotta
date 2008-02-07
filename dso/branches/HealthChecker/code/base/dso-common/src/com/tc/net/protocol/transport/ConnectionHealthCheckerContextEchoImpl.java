/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

/**
 * ECHO HealthChecker Context. On receiving a PING probe, it sends back the PING_REPLY.
 * 
 * @author Manoj
 */
public class ConnectionHealthCheckerContextEchoImpl implements ConnectionHealthCheckerContext {
  private final MessageTransportBase             transport;
  private final HealthCheckerProbeMessageFactory messageFactory;

  public ConnectionHealthCheckerContextEchoImpl(MessageTransportBase mtb) {
    this.transport = mtb;
    this.messageFactory = new TransportMessageFactoryImpl();
  }

  public boolean doSocketConnect() {
    throw new AssertionError("Echo HealthChecker");
  }

  public boolean receiveProbe(HealthCheckerProbeMessage message) {
    if (message.isPing()) {
      HealthCheckerProbeMessage pingReplyMessage = this.messageFactory.createPingReply(transport.getConnectionId(),
                                                                                       transport.getConnection());
      this.transport.send(pingReplyMessage);
      return true;
    }
    throw new AssertionError("Echo HealthChecker");
  }

  public boolean sendProbe() {
    throw new AssertionError("Echo HealthChecker");
  }

  public void refresh() {
    throw new AssertionError("Echo HealthChecker");
  }

}