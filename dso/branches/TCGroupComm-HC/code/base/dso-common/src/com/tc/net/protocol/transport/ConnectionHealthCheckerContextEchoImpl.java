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
    return true;
  }

  public boolean isDying() {
    return true;
  }

  public void pingReplyReceived(long time) {
    //
  }

  public void pingSent(long time) {
    //
  }

  public void refresh() {
    //
  }

  public void receiveDummyPing() {
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
    //
  }

  public void sendDummyPing() {
    //
  }

  public void sendPing() {
    //
  }

  public boolean extraCheck() {
    return false;
  }

  public int getExtraCheckSuccessCount() {
    return 99;
  }

}