/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.groups.NodeID;
import com.tc.net.protocol.TCNetworkMessageEvent;
import com.tc.net.protocol.TCNetworkMessageEventType;
import com.tc.net.protocol.TCNetworkMessageListener;
import com.tc.net.protocol.tcm.ChannelManagerEventListener;
import com.tc.net.protocol.tcm.MessageChannel;

import java.util.HashMap;
import java.util.Map;

public class AcknowledgeTransactionBatchManagerImpl implements AcknowledgeTransactionBatchManager, ChannelManagerEventListener {

  private final Map batchAckStates; // Map<NodeID, BatchAckState>

  public AcknowledgeTransactionBatchManagerImpl() {
    batchAckStates = new HashMap();
  }

  public void batchAckSend(AcknowledgeTransactionMessage ack) {
    AckBatchingState state = getOrCreateState(ack);
    state.batchAck(ack);
  }

  private AckBatchingState getOrCreateState(AcknowledgeTransactionMessage ack) {
    NodeID nid = ack.getRequesterID();
    synchronized (batchAckStates) {
      AckBatchingState state = (AckBatchingState) batchAckStates.get(nid);
      if (state == null) {
        state = new AckBatchingState(nid);
        batchAckStates.put(nid, state);
      }
      return (state);
    }
  }
  
  public void channelCreated(MessageChannel channel) {
  }

  public void channelRemoved(MessageChannel channel) {
  }

  private class AckBatchingState implements TCNetworkMessageListener {

    private volatile AcknowledgeTransactionMessage ackSending;
    private volatile AcknowledgeTransactionMessage ackWaiting;
    private final NodeID                           nid;

    private AckBatchingState(NodeID nid) {
      this.nid = nid;
    }

    private synchronized void batchAck(AcknowledgeTransactionMessage ack) {
      if (ackSending == null) {
        ackSending = ack;
        sendAck(ackSending);
      } else {
        if (ackWaiting == null) {
          ackWaiting = ack;
        } else {
          ackWaiting.addAckMessage(ack.getRequestID());
        }
      }
    }

    private void sendAck(AcknowledgeTransactionMessage ack) {
      AcknowledgeTransactionMessageImpl msg = (AcknowledgeTransactionMessageImpl) ack;
      msg.addListener(this);
      msg.send();
    }

    private synchronized void ackSent() {
      ackSending = null;
      if (ackWaiting != null) {
        ackSending = ackWaiting;
        ackWaiting = null;
        sendAck(ackSending);
      } else {
        // XXX a better way to remove state
        batchAckStates.remove(nid);
      }
    }

    public void notifyMessageEvent(TCNetworkMessageEvent event) {
      if (event.getType() == TCNetworkMessageEventType.SENT_EVENT) {
        ackSent();
      } else if (event.getType() == TCNetworkMessageEventType.SEND_ERROR_EVENT) {
        // XXX
      }
    }
  }

}
