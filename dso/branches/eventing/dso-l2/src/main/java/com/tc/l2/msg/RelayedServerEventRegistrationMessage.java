/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.async.api.EventContext;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.NodeID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.NodeIDSerializer;
import com.tc.server.ServerEventType;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class RelayedServerEventRegistrationMessage extends AbstractGroupMessage implements EventContext {

  public static final int      REGISTER   = 0;
  public static final int      UNREGISTER = 1;

  private NodeID               nodeId;
  private String                 destination;
  private Set<ServerEventType>   eventTypes;

  // To make serialization happy
  public RelayedServerEventRegistrationMessage() {
    super(-1);
  }

  public RelayedServerEventRegistrationMessage(final int type, final NodeID nodeId, final String destination,
                                         final Set<ServerEventType> eventTypes) {
    super(type);
    this.nodeId = nodeId;
    this.destination = destination;
    this.eventTypes = eventTypes;
  }

  public NodeID getNodeID() {
    return nodeId;
  }

  public String getDestination() {
    return destination;
  }

  public Set<ServerEventType> getEventTypes() {
    return eventTypes;
  }

  public boolean isRegisterationMessage() {
    return getType() == REGISTER;
  }


  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    NodeIDSerializer nodeIDSerializer = new NodeIDSerializer();
    nodeIDSerializer = (NodeIDSerializer) nodeIDSerializer.deserializeFrom(in);
    this.nodeId = nodeIDSerializer.getNodeID();
    this.destination = in.readString();
    int eventTypeCount = in.readInt();
    this.eventTypes = new HashSet<ServerEventType>(eventTypeCount);
    for (int i = 0; i < eventTypeCount; i++) {
      eventTypes.add(ServerEventType.values()[in.readInt()]);
    }
  }


  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    NodeIDSerializer nodeIDSerializer = new NodeIDSerializer(nodeId);
    nodeIDSerializer.serializeTo(out);
    out.writeString(destination);
    out.writeInt(eventTypes.size());
    for (ServerEventType eventType : eventTypes) {
      out.writeInt(eventType.ordinal());
    }
  }

}
