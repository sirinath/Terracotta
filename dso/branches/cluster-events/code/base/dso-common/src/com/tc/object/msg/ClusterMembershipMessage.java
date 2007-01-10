/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.io.TCByteBufferOutput;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageType;

import java.io.IOException;

public class ClusterMembershipMessage extends DSOMessageBase {

  public static class EventType {
    public static final int NODE_CONNECTED    = 0;
    public static final int NODE_DISCONNECTED = 1;

    public static boolean isValidType(final int t) {
      return t >= 0 && t <= 1;
    }

    public static boolean isNodeConnected(final int t) {
      return t == NODE_CONNECTED;
    }

    public static boolean isNodeDisconnected(final int t) {
      return t == NODE_DISCONNECTED;
    }
  }

  private static final byte EVENT_TYPE = 0;
  private static final byte NODE_ID    = 1;

  public ClusterMembershipMessage(MessageMonitor monitor, TCByteBufferOutput out, MessageChannel channel,
                                  TCMessageType type) {
    super(monitor, out, channel, type);
  }

  private int    eventType;
  private String nodeId;

  public void initialize(int et, String ni) {
    eventType = et;
    nodeId = ni;
  }

  protected void dehydrateValues() {
    putNVPair(EVENT_TYPE, eventType);
    putNVPair(NODE_ID, nodeId);
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case EVENT_TYPE:
        eventType = getIntValue();
        return true;
      case NODE_ID:
        nodeId = getStringValue();
        return true;
      default:
        return false;
    }
  }

  public int getEventType() {
    return eventType;
  }

  public String getNodeId() {
    return nodeId;
  }
}
