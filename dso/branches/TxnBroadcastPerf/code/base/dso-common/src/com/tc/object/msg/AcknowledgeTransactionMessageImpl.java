/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.groups.NodeID;
import com.tc.net.groups.NodeIDSerializer;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;

import java.io.IOException;
import java.util.ArrayList;

/**
 * @author steve
 */
public class AcknowledgeTransactionMessageImpl extends DSOMessageBase implements AcknowledgeTransactionMessage {
  private final static byte REQUEST_ID   = 1;
  private final static byte REQUESTER_ID = 2;

  private NodeID            requesterID;
  private final ArrayList   acks         = new ArrayList(); // ArrayList<BaseAckTxnMessage>

  public AcknowledgeTransactionMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                           MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public AcknowledgeTransactionMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                           TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  protected void dehydrateValues() {
    for (int i = 0; i < acks.size(); ++i) {
      putNVPair(REQUESTER_ID, new NodeIDSerializer(getRequesterID(i)));
      putNVPair(REQUEST_ID, getRequestID(i).toLong());
    }
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case REQUESTER_ID:
        requesterID = ((NodeIDSerializer) getObject(new NodeIDSerializer())).getNodeID();
        return true;
      case REQUEST_ID:
        acks.add(new BaseAckTxnMessage(requesterID, new TransactionID(getLongValue())));
        return true;
      default:
        return false;
    }
  }

  public void initialize() {
    //
  }

  public void addAckMessage(NodeID nid, TransactionID txID) {
    acks.add(new BaseAckTxnMessage(nid, txID));
  }

  public NodeID getRequesterID(int index) {
    return ((BaseAckTxnMessage) acks.get(index)).getRequesterID();
  }

  public TransactionID getRequestID(int index) {
    return ((BaseAckTxnMessage) acks.get(index)).getRequestID();
  }

  public int size() {
    return acks.size();
  }

  private static class BaseAckTxnMessage {
    private final NodeID        requesterID;
    private final TransactionID requestID;

    private BaseAckTxnMessage(NodeID nid, TransactionID txID) {
      this.requesterID = nid;
      this.requestID = txID;
    }

    private NodeID getRequesterID() {
      return requesterID;
    }

    private TransactionID getRequestID() {
      return requestID;
    }

  }

}