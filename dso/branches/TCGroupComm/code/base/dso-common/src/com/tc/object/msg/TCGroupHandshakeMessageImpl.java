/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.groups.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionID;

import java.io.IOException;

public class TCGroupHandshakeMessageImpl extends DSOMessageBase implements TCGroupHandshakeMessage {

  private static final byte NODE_ID = 1;
  private NodeID            nodeID;

  public TCGroupHandshakeMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutput out,
                                     MessageChannel channel, TCMessageType messageType) {
    super(sessionID, monitor, out, channel, messageType);
  }

  public TCGroupHandshakeMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                     TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public void setNodeID(NodeID nodeID) {
    this.nodeID = nodeID;
  }

  public NodeID getNodeID() {
    return nodeID;
  }

  protected void dehydrateValues() {
    putNVPair(NODE_ID, this.nodeID);
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case NODE_ID:
        getObject(nodeID);
        return true;
      default:
        return false;
    }
  }
}
