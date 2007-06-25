/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.net.protocol.TCNetworkMessage;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.DebugUtil;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * 
 */
public class SendStateMachine extends AbstractStateMachine {
  private static final int                 MAX_SEND_QUEUE_SIZE  = 1000;
  private final State                      HANDSHAKE_STATE      = new HandshakeState();
  private final State                      ACK_WAIT_STATE       = new AckWaitState();
  private final State                      HANDSHAKE_WAIT_STATE = new HandshakeWaitState();
  private final State                      MESSAGE_WAIT_STATE   = new MessageWaitState();
  private final SynchronizedLong           sent                 = new SynchronizedLong(-1);
  private final SynchronizedLong           acked                = new SynchronizedLong(-1);
  private final OOOProtocolMessageDelivery delivery;
  private BoundedLinkedQueue               sendQueue;
  private final LinkedList                 outstandingMsgs      = new LinkedList();
  private final SynchronizedInt            outstandingCnt       = new SynchronizedInt(0);
  private final int                        sendWindow;
  private final boolean                    isClient;
  private final String                     debugId;
  private static final boolean             debug                = true;

  // changed by tc.properties

  public SendStateMachine(OOOProtocolMessageDelivery delivery, boolean isClient) {
    super();

    // set sendWindow from tc.properties if exist. 0 to disable window send.
    sendWindow = TCPropertiesImpl.getProperties().getInt("l2.nha.ooo.sendWindow", 32);
    this.delivery = delivery;
    this.sendQueue = new BoundedLinkedQueue(MAX_SEND_QUEUE_SIZE);
    this.isClient = isClient;
    this.debugId = (isClient) ? "CLIENT" : "SERVER";
  }

  protected void basicResume() {
    if (isClient) switchToState(HANDSHAKE_STATE);
    else switchToState(MESSAGE_WAIT_STATE);
  }

  protected State initialState() {
    Assert.eval(MESSAGE_WAIT_STATE != null);
    return MESSAGE_WAIT_STATE;
  }

  public void execute(OOOProtocolMessage msg) {
    Assert.eval(isStarted());
    getCurrentState().execute(msg);
  }

  private class MessageWaitState extends AbstractState {

    public void enter() {
      execute(null);
    }

    public void execute(OOOProtocolMessage protocolMessage) {
      if (!sendQueue.isEmpty()) {
        Assert.eval(protocolMessage == null);
        if ((sendWindow == 0) || (outstandingCnt.get() < sendWindow)) {
          delivery.sendMessage(createProtocolMessage(sent.increment()));
        }
        switchToState(ACK_WAIT_STATE);
      }
    }
  }

  private class HandshakeState extends AbstractState {
    public void enter() {
      debugLog("Sending Handshake");
      sendHandshake();
      switchToState(HANDSHAKE_WAIT_STATE);
    }
  }

  private class HandshakeWaitState extends AbstractState {

    public void execute(OOOProtocolMessage msg) {
      // expecting an ack to do hand shake
      if (msg == null || !msg.isHandshakeReply()) {
        debugLog("NOT HandshakeReply message - dropping!");
        return;
      }

      long ackedSeq = msg.getAckSequence();

      if (ackedSeq == -1) {
        debugLog("The other side restarted [switching to MSG_WAIT_STATE]");
        reset();
        switchToState(MESSAGE_WAIT_STATE);
        return;
      }
      if (ackedSeq < acked.get()) {
        // this shall not, old ack
      } else {
        while (ackedSeq > acked.get()) {
          acked.increment();
          removeMessage();
        }
        // resend outstanding which is not acked
        if (outstandingCnt.get() > 0) {
          // resend those not acked
          resendOutstandings();
          switchToState(ACK_WAIT_STATE);
        } else {
          // all acked, we're good here
          switchToState(MESSAGE_WAIT_STATE);
        }
      }
    }
  }

  private class AckWaitState extends AbstractState {

    public void enter() {
      sendMoreIfAvailable();
    }

    public void execute(OOOProtocolMessage protocolMessage) {
      if (protocolMessage == null || protocolMessage.isSend()) return;

      long ackedSeq = protocolMessage.getAckSequence();
      Assert.eval(ackedSeq >= acked.get());

      while (ackedSeq > acked.get()) {
        acked.increment();
        removeMessage();
      }

      // try pump more
      sendMoreIfAvailable();

      if (outstandingCnt.get() == 0) {
        switchToState(MESSAGE_WAIT_STATE);
      }

      // ???: is this check properly synchronized?
      Assert.eval(acked.get() <= sent.get());
    }

    public void sendMoreIfAvailable() {
      while ((outstandingCnt.get() < sendWindow) && !sendQueue.isEmpty()) {
        delivery.sendMessage(createProtocolMessage(sent.increment()));
      }
    }
  }

  private void sendHandshake() {
    OOOProtocolMessage opm = delivery.createHandshakeMessage();
    delivery.sendMessage(opm);
  }

  private OOOProtocolMessage createProtocolMessage(long count) {
    final OOOProtocolMessage opm = delivery.createProtocolMessage(count, dequeue(sendQueue));
    Assert.eval(opm != null);
    outstandingCnt.increment();
    outstandingMsgs.add(opm);
    return (opm);
  }

  private void resendOutstandings() {
    ListIterator it = outstandingMsgs.listIterator(0);
    while (it.hasNext()) {
      OOOProtocolMessage msg = (OOOProtocolMessage) it.next();
      delivery.sendMessage(msg);
    }
  }

  private void removeMessage() {
    OOOProtocolMessage msg = (OOOProtocolMessage) outstandingMsgs.removeFirst();
    msg.reallyDoRecycleOnWrite();
    outstandingCnt.decrement();
  }

  public void reset() {

    sent.set(-1);
    acked.set(-1);

    // purge out outstanding sends
    outstandingCnt.set(0);
    outstandingMsgs.clear();

    BoundedLinkedQueue tmpQ = sendQueue;
    sendQueue = new BoundedLinkedQueue(MAX_SEND_QUEUE_SIZE);
    synchronized (tmpQ) {
      while (!tmpQ.isEmpty()) {
        dequeue(tmpQ);
      }
    }
  }

  private static TCNetworkMessage dequeue(BoundedLinkedQueue q) {
    try {
      return (TCNetworkMessage) q.take();
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
  }

  public void put(TCNetworkMessage message) throws InterruptedException {
    sendQueue.put(message);
  }

  private void debugLog(String msg) {
    if (debug) {
      DebugUtil.trace("SENDER-" + debugId + "-" + delivery.getConnectionId() + " -> " + msg);
    }
  }

}
