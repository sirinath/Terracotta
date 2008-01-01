/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.groups.NodeIDImpl;
import com.tc.util.UUID;

import junit.framework.TestCase;

public class TransportHandshakeMessageForTCGroupTest extends TestCase {

  private TransportHandshakeMessage            message;
  private TransportHandshakeMessageFactoryImpl factory;
  private NodeIDImpl                           nodeID;

  public void setUp() throws Exception {

    nodeID = new NodeIDImpl("node", UUID.getUUID().toString().getBytes());
    factory = new TransportHandshakeMessageFactoryImpl(nodeID);

  }

  public void testSendAndReceive() throws Exception {
    boolean isMaxConnectionsExceeded = true;
    int maxConnections = 13;
    DefaultConnectionIdFactory connectionIDProvider = new DefaultConnectionIdFactory();
    ConnectionID connectionId = connectionIDProvider.nextConnectionId();
    message = factory.createSynAck(connectionId, null, isMaxConnectionsExceeded, maxConnections);
    TCByteBuffer payload[] = message.getPayload();

    WireProtocolHeader header = new WireProtocolHeader();
    header.setProtocol(WireProtocolHeader.PROTOCOL_TRANSPORT_HANDSHAKE);
    message = new TransportHandshakeMessageImpl(null, header, payload);
    assertEquals(isMaxConnectionsExceeded, message.isMaxConnectionsExceeded());
    assertEquals(maxConnections, message.getMaxConnections());
    assertTrue(message.getNodeID().equals(nodeID));
  }
}
