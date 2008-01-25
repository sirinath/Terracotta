/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelEventType;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;

import java.util.concurrent.atomic.AtomicBoolean;

/*
 * Each TCGroupMember sits on top of a channel.
 */
public class TCGroupMemberImpl implements TCGroupMember, ChannelEventListener {
  private TCGroupManagerImpl     manager;
  private final MessageChannel   channel;
  private final NodeIdComparable srcNodeID;
  private final NodeIdComparable dstNodeID;
  private final NodeIdComparable peerNodeID;
  // set member ready only when both ends are in group
  private AtomicBoolean          ready               = new AtomicBoolean(false);
  private boolean                closedEventNotified = false;

  /*
   * Member channel established from src to dst. Called by channel initiator, openChannel, peer is dstNodeID.
   */
  public TCGroupMemberImpl(NodeIdComparable srcNodeID, NodeIdComparable dstNodeID, MessageChannel channel) {
    this.channel = channel;
    this.srcNodeID = srcNodeID;
    this.dstNodeID = dstNodeID;
    this.peerNodeID = this.dstNodeID;
    this.channel.addListener(this);
  }

  /*
   * Member channel established from src to dst. Called by channel receiver, channelCreated, peer is srcNodeID.
   */
  public TCGroupMemberImpl(MessageChannel channel, NodeIdComparable srcNodeID, NodeIdComparable dstNodeID) {
    this.channel = channel;
    this.srcNodeID = srcNodeID;
    this.dstNodeID = dstNodeID;
    this.peerNodeID = this.srcNodeID;
    this.channel.addListener(this);
  }

  public MessageChannel getChannel() {
    return channel;
  }

  /*
   * Use a wrapper to send old tribes GroupMessage out through channel's TCMessage
   */
  public void send(GroupMessage msg) throws GroupException {
    if (!channel.isOpen()) { throw new GroupException("Channel is not ready: " + toString()); }
    TCGroupMessageWrapper wrapper = (TCGroupMessageWrapper) channel.createMessage(TCMessageType.GROUP_WRAPPER_MESSAGE);
    wrapper.setGroupMessage(msg);
    wrapper.send();
  }

  public String toString() {
    return ("Group Member: " + ((NodeIDImpl) peerNodeID).getName() + " " + srcNodeID + " <-> " + dstNodeID);
  }

  public void notifyChannelEvent(ChannelEvent event) {
    if (event.getChannel() == channel) {
      if (event.getType() == ChannelEventType.TRANSPORT_CONNECTED_EVENT) {
        ready.set(true);
      } else if ((event.getType() == ChannelEventType.TRANSPORT_DISCONNECTED_EVENT)
                 || (event.getType() == ChannelEventType.CHANNEL_CLOSED_EVENT)) {
        ready.set(false);
        closedEventNotified = true;
        if (manager != null) manager.memberDisappeared(this);
      }
    }
  }

  public NodeIdComparable getSrcNodeID() {
    return srcNodeID;
  }

  public NodeIdComparable getDstNodeID() {
    return dstNodeID;
  }

  public NodeIdComparable getPeerNodeID() {
    return peerNodeID;
  }

  public void setTCGroupManager(TCGroupManagerImpl manager) {
    this.manager = manager;
  }

  public TCGroupManagerImpl getTCGroupManager() {
    return manager;
  }

  public boolean isReady() {
    return (ready.get());
  }

  public void setReady(boolean isReady) {
    ready.set(isReady);
  }

  public void close() {
    ready.set(false);
    // if closed event notified, it is closing, don't do close
    // some incoming messages may be still under processing
    if (!closedEventNotified) {
      getChannel().close();
    }
    closedEventNotified = true;
  }

  public boolean highPriorityLink() {
    return ((srcNodeID).compareTo(dstNodeID) > 0);
  }

}