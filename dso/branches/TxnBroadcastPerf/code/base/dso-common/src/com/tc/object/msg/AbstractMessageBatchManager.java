/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.logging.TCLogger;
import com.tc.net.protocol.TCNetworkMessageEvent;
import com.tc.net.protocol.TCNetworkMessageEventType;
import com.tc.net.protocol.TCNetworkMessageListener;
import com.tc.net.protocol.tcm.ChannelManagerEventListener;
import com.tc.net.protocol.tcm.MessageChannel;

import java.util.HashMap;
import java.util.Map;

abstract public class AbstractMessageBatchManager implements MessageBatchManager, ChannelManagerEventListener {
  private final TCLogger logger;
  private final Map      batchAckStates; // Map<MessageChannel, BatchAckState>

  public AbstractMessageBatchManager(TCLogger logger) {
    this.logger = logger;
    batchAckStates = new HashMap();
  }

  public void sendBatch(DSOMessageBase msg) {
    BatchingState state = getOrCreateState(msg);
    if (state != null) state.sendOrQueue(msg);
  }

  /*
   * Overwritten by subclasses to queue new message into batch
   */
  abstract protected void queueMessageToBatch(DSOMessageBase batchMsg, DSOMessageBase msg);

  private BatchingState getOrCreateState(DSOMessageBase msg) {
    MessageChannel ch = msg.getChannel();
    synchronized (batchAckStates) {
      BatchingState state = (BatchingState) batchAckStates.get(ch);
      if (state == null) {
        if (ch.isClosed()) {
          logger.warn("Send message to closed channel " + ch.getChannelID());
          return null;
        }
        state = new BatchingState(ch);
        batchAckStates.put(ch, state);
      }
      return (state);
    }
  }

  public void channelCreated(MessageChannel channel) {
    //
  }

  public void channelRemoved(MessageChannel channel) {
    synchronized (batchAckStates) {
      BatchingState state = (BatchingState) batchAckStates.get(channel);
      if (state != null) {
        state.drop();
        batchAckStates.remove(channel);
      }
    }
  }

  private class BatchingState implements TCNetworkMessageListener {

    private volatile DSOMessageBase sending;
    private volatile DSOMessageBase posting;
    private volatile DSOMessageBase batching;
    private final MessageChannel    channel;

    private BatchingState(MessageChannel channel) {
      this.channel = channel;
    }

    private void sendOrQueue(DSOMessageBase msg) {
      synchronized (this) {
        if (sending == null) {
          sending = msg;
        } else if (posting == null) {
          posting = msg;
        } else {
          if (batching == null) {
            batching = msg;
          } else {
            queueMessageToBatch(batching, msg);
          }
//          logger.info("XXX batching message " + msg.getMessageType());
          return;
        }
      }
      send(msg);
    }

    private void send(DSOMessageBase msg) {
      msg.addListener(this);
      msg.send();
    }

    private void ackSent() {
      DSOMessageBase msg;
      synchronized (this) {
        if (batching != null) {
          sending = posting;
          msg = posting = batching;
          batching = null;
        } else {
          sending = posting;
          posting = null;
          return;
        }
      }
      send(msg);
    }

    private synchronized void drop() {
      sending = null;
      if (batching != null) {
        batching = null;
        logger.warn("Drop batch messages " + channel.getChannelID());
      }
    }

    public void notifyMessageEvent(TCNetworkMessageEvent event) {
      if (event.getType() == TCNetworkMessageEventType.SENT_EVENT) {
        ackSent();
      } else if (event.getType() == TCNetworkMessageEventType.SEND_ERROR_EVENT) {
        drop();
      }
    }
  }

}
