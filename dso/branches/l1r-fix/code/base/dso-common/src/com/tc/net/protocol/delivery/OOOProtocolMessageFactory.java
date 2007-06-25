/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.protocol.TCNetworkMessage;

interface OOOProtocolMessageFactory {
  public OOOProtocolMessage createNewHandshakeMessage(short sessionId, long ack);

  public OOOProtocolMessage createNewHandshakeReplyMessage(short sessionId, long ack);

  public OOOProtocolMessage createNewAckMessage(short sessionId, long ack);

  public OOOProtocolMessage createNewSendMessage(short sessionId, long sequence, TCNetworkMessage payload);

  public OOOProtocolMessage createNewMessage(OOOProtocolMessageHeader header, TCByteBuffer[] data);

  public OOOProtocolMessage createNewGoodbyeMessage(short sessionId);
}
