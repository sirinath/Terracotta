/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.core.TCConnection;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.protocol.NetworkStackID;
import com.tc.util.Assert;

public class ServerMessageTransport extends MessageTransportBase {

  private static final TCLogger logger  = TCLogging.getLogger(ServerMessageTransport.class);

  public ServerMessageTransport(ConnectionID connectionID, TransportHandshakeErrorHandler handshakeErrorHandler,
                                TransportHandshakeMessageFactory messageFactory) {
    super(MessageTransportState.STATE_RESTART, handshakeErrorHandler, messageFactory, true, logger);
    this.connectionId = connectionID;
  }

  /**
   * Constructor for when you want a transport that you can specify a connection
   * (e.g., in a server). This constructor will create an open MessageTransport
   * ready for use.
   */
  public ServerMessageTransport(ConnectionID connectionId, TCConnection conn,
                                TransportHandshakeErrorHandler handshakeErrorHandler,
                                TransportHandshakeMessageFactory messageFactory) {
    super(MessageTransportState.STATE_START, handshakeErrorHandler, messageFactory, true, logger);
    this.connectionId = connectionId;
    Assert.assertNotNull(conn);
    wireNewConnection(conn);
  }

  protected ConnectionAttacher getConnectionAttacher() {
    if (this.status.isRestart()) {
      return new RestartConnectionAttacher();
    } else return super.getConnectionAttacher();
  }

  public NetworkStackID open() {
    throw new UnsupportedOperationException("Server transport doesn't support open()");
  }

  protected void receiveTransportMessageImpl(WireProtocolMessage message) {
    synchronized (status) {
      if (status.isStart()) {
        verifyAndHandleAck(message);
        message.recycle();
        return;
      } else if (verifyHandshakeMessage(message)) {
        handleHandshakeError(new TransportHandshakeErrorContext("Unexpected handshake message in state: " + status),
                             (TransportHandshakeMessage) message);
        return;
      }
    }
    super.receiveToReceiveLayer(message);
  }

  private void verifyAndHandleAck(WireProtocolMessage message) {
    if (!verifyAck(message)) {
      handleHandshakeError(new TransportHandshakeErrorContext("Expected an ACK message but received: " + message));
    } else {
      handleAck((TransportHandshakeMessage) message);
    }
  }

  private void handleAck(TransportHandshakeMessage ack) {
    synchronized (status) {
      Assert.eval(status.isStart());
      Assert.eval("Wrong connection ID: [" + this.connectionId + "] != [" + ack.getConnectionId() + "]",
                  this.connectionId.equals(ack.getConnectionId()));
      status.established();
      fireTransportConnectedEvent();
    }
  }

  private boolean verifyAck(WireProtocolMessage message) {
    return message instanceof TransportHandshakeMessage && ((TransportHandshakeMessage)message).isAck();
  }

  private boolean verifyHandshakeMessage(WireProtocolMessage message) {
    return message instanceof TransportHandshakeMessage;
  }

  private final class RestartConnectionAttacher implements ConnectionAttacher {

    public void attachNewConnection(TCConnectionEvent closeEvent, TCConnection oldConnection, TCConnection newConnection) {
      Assert.assertNull(oldConnection);
      wireNewConnection(newConnection);
    }

  }

}