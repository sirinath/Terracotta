/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.l2.ha.ClusterState;
import com.tc.net.GroupID;
import com.tc.net.StripeID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupToStripeMapSerializer;
import com.tc.net.groups.MessageID;
import com.tc.net.protocol.transport.ConnectionID;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.tc.net.groups.GroupMessage;

public class ClusterStateMessage extends AbstractGroupMessage {

  public static final int        OBJECT_ID                    = 0x00;
  public static final int        NEW_CONNECTION_CREATED       = 0x01;
  public static final int        CONNECTION_DESTROYED         = 0x02;
  public static final int        GLOBAL_TRANSACTION_ID        = 0x03;
  public static final int        DGC_ID                       = 0x04;
  public static final int        COMPLETE_STATE               = 0xF0;
  public static final int        OPERATION_FAILED_SPLIT_BRAIN = 0xFE;
  public static final int        OPERATION_SUCCESS            = 0xFF;

  private long                   nextAvailableObjectID;
  private long                   nextAvailableGID;
  private long                   nextAvailableDGCId;
  private String                 clusterID;
  private ConnectionID           connectionID;
  private long                   nextAvailableChannelID;
  private Set<ConnectionID>      connectionIDs;
  private Map<GroupID, StripeID> stripeIDMap;

  // To make serialization happy
  public ClusterStateMessage() {
    super(-1);
  }

  public ClusterStateMessage(int type) {
    super(type);
  }

  public ClusterStateMessage(int type, MessageID requestID) {
    super(type, requestID);
  }

  public ClusterStateMessage(int type, ConnectionID connID) {
    super(type);
    this.connectionID = connID;
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    switch (getType()) {
      case OBJECT_ID:
        nextAvailableObjectID = in.readLong();
        break;
      case GLOBAL_TRANSACTION_ID:
        nextAvailableGID = in.readLong();
        break;
      case DGC_ID:
        nextAvailableDGCId = in.readLong();
        break;
      case NEW_CONNECTION_CREATED:
      case CONNECTION_DESTROYED:
        connectionID = ConnectionID.readFrom(in);
        break;
      case COMPLETE_STATE:
        nextAvailableObjectID = in.readLong();
        nextAvailableGID = in.readLong();
        nextAvailableChannelID = in.readLong();
        nextAvailableDGCId = in.readLong();
        clusterID = in.readString();
        int size = in.readInt();
        connectionIDs = new HashSet<ConnectionID>(size);
        for (int i = 0; i < size; i++) {
          connectionIDs.add(ConnectionID.readFrom(in));
        }
        GroupToStripeMapSerializer serializer = new GroupToStripeMapSerializer();
        serializer.deserializeFrom(in);
        stripeIDMap = serializer.getMap();
        break;
      case OPERATION_FAILED_SPLIT_BRAIN:
      case OPERATION_SUCCESS:
        break;
      default:
        throw new AssertionError("Unknown type : " + getType());
    }
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    switch (getType()) {
      case OBJECT_ID:
        out.writeLong(nextAvailableObjectID);
        break;
      case GLOBAL_TRANSACTION_ID:
        out.writeLong(nextAvailableGID);
        break;
      case DGC_ID:
        out.writeLong(nextAvailableDGCId);
        break;
      case NEW_CONNECTION_CREATED:
      case CONNECTION_DESTROYED:
        connectionID.writeTo(out);
        break;
      case COMPLETE_STATE:
        out.writeLong(nextAvailableObjectID);
        out.writeLong(nextAvailableGID);
        out.writeLong(nextAvailableChannelID);
        out.writeLong(nextAvailableDGCId);
        out.writeString(clusterID);
        out.writeInt(connectionIDs.size());
        for (ConnectionID id : connectionIDs) {
          id.writeTo(out);
        }
        new GroupToStripeMapSerializer(stripeIDMap).serializeTo(out);
        break;
      case OPERATION_FAILED_SPLIT_BRAIN:
      case OPERATION_SUCCESS:
        break;
      default:
        throw new AssertionError("Unknown type : " + getType());
    }
  }

  public long getNextAvailableObjectID() {
    return nextAvailableObjectID;
  }

  public long getNextAvailableGlobalTxnID() {
    return nextAvailableGID;
  }

  public String getClusterID() {
    return clusterID;
  }

  public ConnectionID getConnectionID() {
    return connectionID;
  }

  public void initMessage(ClusterState state) {
    switch (getType()) {
      case OBJECT_ID:
        nextAvailableObjectID = state.getNextAvailableObjectID();
        break;
      case GLOBAL_TRANSACTION_ID:
        nextAvailableGID = state.getNextAvailableGlobalTxnID();
        break;
      case DGC_ID:
        nextAvailableDGCId = state.getNextAvailableDGCID();
        break;
      case COMPLETE_STATE:
        nextAvailableObjectID = state.getNextAvailableObjectID();
        nextAvailableGID = state.getNextAvailableGlobalTxnID();
        nextAvailableChannelID = state.getNextAvailableChannelID();
        nextAvailableDGCId = state.getNextAvailableDGCID();
        clusterID = state.getStripeID().getName();
        connectionIDs = state.getAllConnections();
        stripeIDMap = state.getStripeIDMap();
        break;
      default:
        throw new AssertionError("Wrong Type : " + getType());
    }
  }

  public void initState(ClusterState state) {
    switch (getType()) {
      case OBJECT_ID:
        state.setNextAvailableObjectID(nextAvailableObjectID);
        break;
      case GLOBAL_TRANSACTION_ID:
        state.setNextAvailableGlobalTransactionID(nextAvailableGID);
        break;
      case DGC_ID:
        state.setNextAvailableDGCId(nextAvailableDGCId);
        break;
      case COMPLETE_STATE:
        state.setNextAvailableObjectID(nextAvailableObjectID);
        state.setNextAvailableGlobalTransactionID(nextAvailableGID);
        state.setNextAvailableChannelID(nextAvailableChannelID);
        state.setNextAvailableDGCId(nextAvailableDGCId);
        for (ConnectionID id : connectionIDs) {
          state.addNewConnection(id);
        }
        for (GroupID gid : stripeIDMap.keySet()) {
          state.addToStripeIDMap(gid, stripeIDMap.get(gid));
        }
        // trigger local stripeID ready event after StripeIDMap loaded.
        state.setStripeID(clusterID);
        break;
      case NEW_CONNECTION_CREATED:
        state.addNewConnection(connectionID);
        break;
      case CONNECTION_DESTROYED:
        state.removeConnection(connectionID);
        break;
      default:
        throw new AssertionError("Wrong Type : " + getType());
    }
  }

  public boolean isSplitBrainMessage() {
    return getType() == OPERATION_FAILED_SPLIT_BRAIN;
  }

  public static ClusterStateMessage createNextAvailableObjectIDMessage(ClusterState state) {
    ClusterStateMessage msg = new ClusterStateMessage(ClusterStateMessage.OBJECT_ID);
    msg.initMessage(state);
    return msg;
  }

  public static ClusterStateMessage createOKResponse(ClusterStateMessage msg) {
    ClusterStateMessage response = new ClusterStateMessage(ClusterStateMessage.OPERATION_SUCCESS, msg.getMessageID());
    return response;
  }

  public static ClusterStateMessage createNGSplitBrainResponse(ClusterStateMessage msg) {
    ClusterStateMessage response = new ClusterStateMessage(ClusterStateMessage.OPERATION_FAILED_SPLIT_BRAIN, msg
        .getMessageID());
    return response;
  }

  public static ClusterStateMessage createClusterStateMessage(ClusterState state) {
    ClusterStateMessage msg = new ClusterStateMessage(ClusterStateMessage.COMPLETE_STATE);
    msg.initMessage(state);
    return msg;
  }

  public static ClusterStateMessage createNewConnectionCreatedMessage(ConnectionID connID) {
    ClusterStateMessage msg = new ClusterStateMessage(ClusterStateMessage.NEW_CONNECTION_CREATED, connID);
    return msg;
  }

  public static ClusterStateMessage createConnectionDestroyedMessage(ConnectionID connID) {
    ClusterStateMessage msg = new ClusterStateMessage(ClusterStateMessage.CONNECTION_DESTROYED, connID);
    return msg;
  }

  public static ClusterStateMessage createNextAvailableGlobalTransactionIDMessage(ClusterState state) {
    ClusterStateMessage msg = new ClusterStateMessage(ClusterStateMessage.GLOBAL_TRANSACTION_ID);
    msg.initMessage(state);
    return msg;
  }

  public static ClusterStateMessage createNextAvailableDGCIterationMessage(ClusterState state) {
    ClusterStateMessage msg = new ClusterStateMessage(ClusterStateMessage.DGC_ID);
    msg.initMessage(state);
    return msg;
  }
}
