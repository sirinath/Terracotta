/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.groups.NodeID;

import java.util.HashMap;
import java.util.Map;

public class AcknowledgeTransactionBatchManagerImpl implements AcknowledgeTransactionBatchManager {

  private final Map batchAckStates; // Map<NodeID, BatchAckState>

  public AcknowledgeTransactionBatchManagerImpl() {
    batchAckStates = new HashMap();
  }

  public void batchAckSend(AcknowledgeTransactionMessage ack) {
    BatchAckState state = getOrCreateState(ack.getRequesterID());
    state.batchAck(ack);
  }

  private BatchAckState getOrCreateState(NodeID nid) {
    synchronized (batchAckStates) {
      BatchAckState state = (BatchAckState) batchAckStates.get(nid);
      if (state == null) {
        state = new BatchAckState();
        batchAckStates.put(nid, state);
      }
      return (state);
    }
  }

  private static class BatchAckState {

    private final Runnable                ackSentCallback;
    private Runnable                      originalSentCallBack;
    private AcknowledgeTransactionMessage ackSending;
    private AcknowledgeTransactionMessage ackWaiting;

    private BatchAckState() {
      ackSentCallback = sentCallback();
    }

    private synchronized void batchAck(AcknowledgeTransactionMessage ack) {
      if (ackSending == null) {
        ackSending = ack;
        ackSend(ackSending);
      } else {
        if (ackWaiting == null) {
          ackWaiting = ack;
        } else {
          ackWaiting.batchAck(ack.getRequestID());
        }
      }
    }

    private void ackSend(AcknowledgeTransactionMessage ack) {
      AcknowledgeTransactionMessageImpl impl = (AcknowledgeTransactionMessageImpl) ack;
      originalSentCallBack = impl.getSentCallback();
      impl.setSentCallback(ackSentCallback);
      impl.send();
    }

    private synchronized void ackSent() {
      ackSending = null;
      originalSentCallBack = null;
      if (ackWaiting != null) {
        ackSending = ackWaiting;
        ackWaiting = null;
        ackSend(ackSending);
      }
    }

    private Runnable sentCallback() {
      Runnable callback = new Runnable() {
        public void run() {
          if (originalSentCallBack != null) {
            originalSentCallBack.run();
          }
          ackSent();
        }
      };
      return (callback);
    }
  }
}
