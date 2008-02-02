/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

public class ConnectionHealthCheckerContextEchoImpl implements ConnectionHealthCheckerContext {
  private final MessageTransportBase             transport;
  private final HealthCheckerProbeMessageFactory messageFactory;
  private final TCLogger                         logger = TCLogging.getLogger(this.getClass());

  public ConnectionHealthCheckerContextEchoImpl(MessageTransportBase mtb) {
    this.transport = mtb;
    this.messageFactory = new TransportMessageFactoryImpl();
  }

  public boolean isDead(long maxIdleTime) {
    throw new AssertionError("HealthChecker ECHO impl");
  }

  public boolean isDying() {
    throw new AssertionError("HealthChecker ECHO impl");
  }

  public void refresh() {
    throw new AssertionError("HealthChecker ECHO impl");
  }

  public void receiveDummyPing() {
    throw new AssertionError("HealthChecker ECHO impl");
  }

  public void receivePing() {
    sendPingReply();
    // XXX may note down the time in future
  }

  public void sendPingReply() {
    HealthCheckerProbeMessage pingReplyMessage = this.messageFactory.createPingReply(transport.getConnectionId(),
                                                                                     transport.getConnection());
    this.transport.send(pingReplyMessage);
    // XXX may note down the time in future
  }

  public void receivePingReply() {
    throw new AssertionError("HealthChecker ECHO impl");
    //
  }

  public void sendDummyPing() {
    throw new AssertionError("HealthChecker ECHO impl");
    //
  }

  public void sendPing() {
    throw new AssertionError("HealthChecker ECHO impl");
    //
  }

  public boolean extraCheck() {
    throw new AssertionError("HealthChecker ECHO impl");
  }

  public int getExtraCheckSuccessCount() {
    throw new AssertionError("HealthChecker ECHO impl");
  }

}