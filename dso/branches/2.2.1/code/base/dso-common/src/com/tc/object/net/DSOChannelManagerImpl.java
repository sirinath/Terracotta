/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.net;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;
import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;

import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.ChannelManager;
import com.tc.net.protocol.tcm.ChannelManagerEventListener;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.BatchTransactionAcknowledgeMessage;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Wraps the generic ChannelManager to hide it from the rest of the DSO world
 */
public class DSOChannelManagerImpl implements DSOChannelManager, DSOChannelManagerMBean, ChannelManagerEventListener {

  private static final MessageChannel[] EMPTY_CHANNEL_ARRAY = new MessageChannel[] {};

  private final Map                     publishedChannels   = new ConcurrentReaderHashMap();
  private final List                    eventListeners      = new CopyOnWriteArrayList();

  public DSOChannelManagerImpl(ChannelManager genericChannelManager) {
    genericChannelManager.addEventListener(this);
  }

  public MessageChannel getChannel(ChannelID id) throws NoSuchChannelException {
    MessageChannel rv = (MessageChannel) publishedChannels.get(id);
    if (rv == null) { throw new NoSuchChannelException("No such channel: " + id); }
    return rv;
  }

  public void closeAll(Collection channelIDs) {
    for (Iterator i = channelIDs.iterator(); i.hasNext();) {
      ChannelID id = (ChannelID) i.next();
      try {
        MessageChannel channel = getChannel(id);
        channel.close();
      } catch (NoSuchChannelException e) {
        //
      }
    }
  }

  public MessageChannel[] getChannels() {
    return (MessageChannel[]) publishedChannels.values().toArray(EMPTY_CHANNEL_ARRAY);
  }

  public boolean isValidID(ChannelID channelID) {
    return publishedChannels.containsKey(channelID);
  }

  public void addEventListener(ChannelManagerEventListener listener) {
    if (listener == null) { throw new IllegalArgumentException("listener must be non-null"); }
    this.eventListeners.add(listener);
  }

  public String getChannelAddress(ChannelID channelID) {
    try {
      MessageChannel channel = getChannel(channelID);
      TCSocketAddress addr = channel.getRemoteAddress();
      return addr.getStringForm();
    } catch (NoSuchChannelException e) {
      return "no longer connected";
    }
  }

  public Collection getAllChannelIDs() {
    return new HashSet(publishedChannels.keySet());
  }

  public void publishChannel(MessageChannel channel) {
    publishedChannels.put(channel.getChannelID(), channel);
    fireChannelCreatedEvent(channel);
  }

  public void channelCreated(MessageChannel channel) {
    // do nothing here -- handshake manager will "publish" the channel when it sees fit
  }

  public void channelRemoved(MessageChannel channel) {
    publishedChannels.remove(channel.getChannelID());
    fireChannelRemovedEvent(channel);
  }

  private void fireChannelCreatedEvent(MessageChannel channel) {
    for (Iterator iter = eventListeners.iterator(); iter.hasNext();) {
      ChannelManagerEventListener eventListener = (ChannelManagerEventListener) iter.next();
      eventListener.channelCreated(channel);
    }
  }

  private void fireChannelRemovedEvent(MessageChannel channel) {
    for (Iterator iter = eventListeners.iterator(); iter.hasNext();) {
      ChannelManagerEventListener eventListener = (ChannelManagerEventListener) iter.next();
      eventListener.channelRemoved(channel);
    }
  }

  public BatchTransactionAcknowledgeMessage newBatchTransactionAcknowledgeMessage(ChannelID channelID)
      throws NoSuchChannelException {
    return (BatchTransactionAcknowledgeMessage) getChannel(channelID)
        .createMessage(TCMessageType.BATCH_TRANSACTION_ACK_MESSAGE);
  }

}
