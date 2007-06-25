/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.DebugUtil;

/**
 * 
 */
public class ReceiveStateMachine extends AbstractStateMachine {
  private final State                      MESSAGE_WAIT_STATE = new MessageWaitState();

  private final SynchronizedLong           received           = new SynchronizedLong(-1);
  private final SynchronizedLong           acked              = new SynchronizedLong(0);
  private final int                        maxDelayedAcks;
  private final OOOProtocolMessageDelivery delivery;

  private String                           debugId            = "UNKNOWN";
  private static final boolean             debug              = true;

  public ReceiveStateMachine(OOOProtocolMessageDelivery delivery) {
    maxDelayedAcks = TCPropertiesImpl.getProperties().getInt("l2.nha.ooo.maxDelayedAcks", 16);
    this.delivery = delivery;
  }

  public void execute(OOOProtocolMessage msg) {
    getCurrentState().execute(msg);
  }

  protected State initialState() {
    return MESSAGE_WAIT_STATE;
  }

  private class MessageWaitState extends AbstractState {

    public void execute(OOOProtocolMessage msg) {
      if (msg.isSend()) {
        handleSendMessage(msg);
      } else if (msg.isHandshake()) {
        sendHandshakeReply(received.get());
      } else {
        // these message should be handled at higher level
        Assert.inv(msg.isAck() || msg.isGoodbye());
        Assert.inv(false);
      }
    }

    private void handleSendMessage(OOOProtocolMessage msg) {
      final long r = msg.getSent();
      final long curRecv = received.get();
      if (r <= curRecv) {
        // we already got message
        sendAck(curRecv);
      } else if (r > (curRecv + 1)) {
        // message missed, resend ack, receive to resend message.
        sendAck(curRecv);
        return;
      } else {
        Assert.inv(r == (curRecv + 1));
        putMessage(msg);
        ackIfNeeded(received.increment());
      }
    }
  }

  private void putMessage(OOOProtocolMessage msg) {
    this.delivery.receiveMessage(msg);
  }

  private void ackIfNeeded(long next) {
    Assert.inv(next >= acked.get());
    final long delta = next - acked.get();
    if (delta >= maxDelayedAcks) {
      sendAck(next);
    }
  }

  private void sendAck(long seq) {
    OOOProtocolMessage opm = delivery.createAckMessage(seq);
    Assert.inv(opm.getSessionId() > -1);
    acked.set(seq);
    delivery.sendMessage(opm);
  }

  private void sendHandshakeReply(long l) {
    OOOProtocolMessage msg = delivery.createHandshakeReplyMessage(l);
    delivery.sendMessage(msg);
  }

  public void reset() {
    received.set(-1);
  }

  private void debugLog(String msg) {
    if (debug) {
      DebugUtil.trace("Receiver-" + debugId + "-" + delivery.getConnectionId() + " -> " + msg);
    }
  }

  public void setDebugId(String debugId) {
    this.debugId = debugId;
  }

}
