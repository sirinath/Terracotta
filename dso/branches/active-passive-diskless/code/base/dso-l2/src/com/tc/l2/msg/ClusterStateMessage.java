/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.l2.ha.ClusterState;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.MessageID;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ClusterStateMessage extends AbstractGroupMessage {

  public static final int OBJECT_ID         = 0x00;
  public static final int COMPLETE_STATE    = 0xF0;
  public static final int OPERATION_SUCCESS = 0xFF;
  
  private transient ClusterState state;
  
  private long nextAvailableObjectID;

  // To make serialization happy
  public ClusterStateMessage() {
    super(-1);
  }

  public ClusterStateMessage(int type, ClusterState state) {
    super(type);
    this.state = state;
  }

  public ClusterStateMessage(int type, MessageID requestID) {
    super(type, requestID);
  }

  protected void basicReadExternal(int msgType, ObjectInput in) throws IOException {
    switch (msgType) {
      case OBJECT_ID:
        nextAvailableObjectID = in.readLong();
        break;
      case COMPLETE_STATE:
        nextAvailableObjectID = in.readLong();
        break;
      case OPERATION_SUCCESS:
        break;
      default:
        throw new AssertionError("Unknown type : " + msgType);
    }
  }

  protected void basicWriteExternal(int msgType, ObjectOutput out) throws IOException {
    switch (msgType) {
      case OBJECT_ID:
        out.writeLong(state.getNextAvailableObjectID());
        break;
      case COMPLETE_STATE:
        out.writeLong(state.getNextAvailableObjectID());
        break;
      case OPERATION_SUCCESS:
        break;
      default:
        throw new AssertionError("Unknown type : " + msgType);
    }
  }
  
  public long getNextAvailableObjectID() {
    return nextAvailableObjectID;
  }

}
