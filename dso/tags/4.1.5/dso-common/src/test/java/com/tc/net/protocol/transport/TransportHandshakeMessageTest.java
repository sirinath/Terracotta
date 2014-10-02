/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.bytes.TCByteBuffer;

import junit.framework.TestCase;

public class TransportHandshakeMessageTest extends TestCase {

  private TransportHandshakeMessage   message;
  private TransportMessageFactoryImpl factory;

  @Override
  public void setUp() throws Exception {
    factory = new TransportMessageFactoryImpl();
  }

  public void testSendAndReceive() throws Exception {
    boolean isMaxConnectionsExceeded = true;
    int maxConnections = 13;
    ConnectionID connectionId = new ConnectionID("abc", 1L);
    message = factory.createSynAck(connectionId, null, isMaxConnectionsExceeded, maxConnections, 43);
    TCByteBuffer payload[] = message.getPayload();

    WireProtocolHeader header = new WireProtocolHeader();
    header.setProtocol(WireProtocolHeader.PROTOCOL_TRANSPORT_HANDSHAKE);
    message = new TransportMessageImpl(null, header, payload);
    assertEquals(isMaxConnectionsExceeded, message.isMaxConnectionsExceeded());
    assertEquals(maxConnections, message.getMaxConnections());
  }
}
