/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;

import java.io.IOException;

/**
 * @author EY
 */
public class TCGroupHandshakeMessage extends DSOMessageBase {
  private final static byte NODE_ID = 1;
  private NodeIdComparable    nodeID;

  public TCGroupHandshakeMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutput out,
                                 MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public TCGroupHandshakeMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                 TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public NodeIdComparable getNodeID() {
    return this.nodeID;
  }
  
  public void initialize(NodeIdComparable nodeID) {
    this.nodeID = nodeID;
  }

  protected void dehydrateValues() {
    putNVPair(NODE_ID, nodeID);
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case NODE_ID:
        nodeID = new NodeIdComparable();
        getObject(nodeID);
        return true;
      default:
        return false;
    }
  }
}