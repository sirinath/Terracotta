/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.logging.TCLogger;
import com.tc.net.protocol.TCNetworkMessageEvent;
import com.tc.net.protocol.TCNetworkMessageEventType;
import com.tc.net.protocol.TCNetworkMessageListener;
import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelEventType;
import com.tc.net.protocol.tcm.ChannelManagerEventListener;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.util.Assert;

import java.util.HashMap;
import java.util.Map;

abstract public class AbstractMessageBatchManager implements MessageBatchManager, ChannelManagerEventListener,
    ChannelEventListener {
  private final TCLogger logger;
  private final Map      batchAckStates; // Map<MessageChannel, BatchAckState>
  private final int      batchThreshold; // outstanding messages before start batching

  public AbstractMessageBatchManager(TCLogger logger, int batchThreshold) {
    this.logger = logger;
    this.batchThreshold = batchThreshold;
    batchAckStates = new HashMap();
  }

  public void sendBatch(DSOMessageBase msg) {
    BatchingState state = getOrCreateState(msg);
    if (state != null) state.sendOrQueue(msg);
  }

  public void flush(MessageChannel channel) {
    BatchingState state;
    synchronized (batchAckStates) {
      state = (BatchingState) batchAckStates.get(channel);
    }
    if (state != null) state.flush();
  }

  /*
   * Overwritten by subclasses to queue new message into batch
   */
  abstract protected void queueMessageToBatch(DSOMessageBase batchMsg, DSOMessageBase msg);

  abstract public void sendStatisticsRecord(DSOMessageBase msg);

  abstract public void sendStatisticsQueuePosition(int sendQueuePosition);

  abstract public void sendStatisticsNoPending();

  private BatchingState getOrCreateState(DSOMessageBase msg) {
    MessageChannel ch = msg.getChannel();
    synchronized (batchAckStates) {
      BatchingState state = (BatchingState) batchAckStates.get(ch);
      Assert.assertNotNull("Unavailabe state " + ch.getChannelID(), state);
      return (state);
    }
  }

  private void created(MessageChannel channel) {
    synchronized (batchAckStates) {
      Assert.assertNull("Stale state " + channel.getLocalAddress() + " -> " + channel.getRemoteAddress(),
                        batchAckStates.get(channel));
      batchAckStates.put(channel, new BatchingState(channel));
    }
    // logger.info("XXX created " + channel.getLocalAddress() + " -> " + channel.getRemoteAddress());
  }

  private void removed(MessageChannel channel) {
    synchronized (batchAckStates) {
      BatchingState state = (BatchingState) batchAckStates.get(channel);
      if (state != null) {
        state.drop();
        batchAckStates.remove(channel);
      }
    }
    // logger.info("XXX removed " + channel.getChannelID());
  }

  // listen to L2
  public void channelCreated(MessageChannel channel) {
    created(channel);
  }

  // listen to L2
  public void channelRemoved(MessageChannel channel) {
    removed(channel);
  }

  // listen to L1
  public void notifyChannelEvent(ChannelEvent event) {
    if (ChannelEventType.TRANSPORT_CONNECTED_EVENT.matches(event)) {
      created(event.getChannel());
    } else if (ChannelEventType.TRANSPORT_DISCONNECTED_EVENT.matches(event)) {
      removed(event.getChannel());
    }
  }

  private class BatchingState implements TCNetworkMessageListener {

    private volatile boolean        doBatching = false;
    private volatile DSOMessageBase batching;
    private final MessageChannel    channel;

    private BatchingState(MessageChannel channel) {
      this.channel = channel;
    }

    private void sendOrQueue(DSOMessageBase msg) {

      synchronized (this) {
        if (doBatching) {
          if (batching == null) {
            batching = msg;
          } else {
            queueMessageToBatch(batching, msg);
          }
          return;
        }
        doBatching = true;
      }
      send(msg);

      // logger.info("XXX batching message " + msg.getChannelID() + " " + msg.getMessageType());
    }

    private void send(DSOMessageBase msg) {
      sendStatisticsRecord(msg);
      msg.addListener(this);
      int pos = msg.send();
      synchronized (this) {
        if (pos <= batchThreshold) doBatching = false;
      }
      sendStatisticsQueuePosition(pos);
    }

    private void sendNoCallback(DSOMessageBase msg) {
      sendStatisticsRecord(msg);
      msg.send();
    }

    private void ackSent() {
      DSOMessageBase msg;
      synchronized (this) {
        if (batching == null) {
          sendStatisticsNoPending();
          doBatching = false;
          return;
        } else {
          msg = batching;
          batching = null;
          doBatching = true;
        }
      }
      send(msg);
    }

    private void drop() {
      synchronized (this) {
        doBatching = false;
        if (batching != null) {
          batching = null;
          logger.warn("Drop batch messages " + channel.getChannelID());
        }
      }
    }

    private void flush() {
      synchronized (this) {
        if (batching != null) {
          sendNoCallback(batching);
          batching = null;
          doBatching = false;
        }
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
