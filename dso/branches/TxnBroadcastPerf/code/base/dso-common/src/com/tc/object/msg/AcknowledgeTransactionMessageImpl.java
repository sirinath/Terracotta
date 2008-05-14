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
import com.tc.util.Assert;

import java.io.IOException;
import java.util.ArrayList;

/**
 * @author steve
 */
public class AcknowledgeTransactionMessageImpl extends DSOMessageBase implements AcknowledgeTransactionMessage {
  private final static byte REQUEST_ID   = 1;
  private final static byte REQUESTER_ID = 2;

  private NodeID            requesterID;
  private final ArrayList   acks         = new ArrayList(); // ArrayList<TransactionID>

  public AcknowledgeTransactionMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                           MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public AcknowledgeTransactionMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                           TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  protected void dehydrateValues() {
    putNVPair(REQUESTER_ID, new NodeIDSerializer(requesterID));
    for (int i = 0; i < acks.size(); ++i) {
      putNVPair(REQUEST_ID, getRequestID(i).toLong());
    }
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case REQUESTER_ID:
        requesterID = ((NodeIDSerializer) getObject(new NodeIDSerializer())).getNodeID();
        return true;
      case REQUEST_ID:
        acks.add(new TransactionID(getLongValue()));
        return true;
      default:
        return false;
    }
  }

  public void initialize(NodeID nid, TransactionID txID) {
    requesterID = nid;
    acks.add(txID);
    Assert.assertTrue(acks.size() == 1);
  }

  public void batchAck(TransactionID txID) {
    acks.add(txID);
  }

  public NodeID getRequesterID() {
    return requesterID;
  }

  public TransactionID getRequestID() {
    return getRequestID(0);
  }

  public TransactionID getRequestID(int index) {
    return ((TransactionID) acks.get(index));
  }

  public int acksBatchSize() {
    return acks.size();
  }

}