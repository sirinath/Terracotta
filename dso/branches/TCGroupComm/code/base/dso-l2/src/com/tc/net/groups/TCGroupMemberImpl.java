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
  private TCGroupManager       manager;
  private final MessageChannel channel;
  private final NodeID         srcNodeID;                           // who setup channel
  private final NodeID         dstNodeID;
  private final NodeID         peerNodeID;
  private AtomicBoolean        connected = new AtomicBoolean(false);

  /*
   * Member channel established from src to dst.
   * Called by channel initiator, openChannel, peer is dstNodeID.
   */
  public TCGroupMemberImpl(NodeID srcNodeID, NodeID dstNodeID, MessageChannel channel) {
    this.channel = channel;
    this.srcNodeID = srcNodeID;
    this.dstNodeID = dstNodeID;
    this.peerNodeID = this.dstNodeID;
    this.channel.addListener(this);
  }

  /*
   * Member channel established from src to dst.
   * Called by channel receiver, channelCreated, peer is srcNodeID.
   */
  public TCGroupMemberImpl(MessageChannel channel, NodeID srcNodeID, NodeID dstNodeID) {
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
    if (!connected.get() || !channel.isOpen()) { throw new GroupException("Channel is not open: " + toString()); }
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
        connected.set(true);
      } else if (event.getType() == ChannelEventType.TRANSPORT_DISCONNECTED_EVENT) {
        connected.set(false);
        if (manager != null) manager.memberDisappeared(this);
      } else if (event.getType() == ChannelEventType.CHANNEL_CLOSED_EVENT) {
        connected.set(false);
        if (manager != null) manager.memberDisappeared(this);
      }
    }
  }

  public NodeID getSrcNodeID() {
    return srcNodeID;
  }

  public NodeID getDstNodeID() {
    return dstNodeID;
  }

  public NodeID getPeerNodeID() {
    return peerNodeID;
  }

  public void setTCGroupManager(TCGroupManager manager) {
    this.manager = manager;
    connected.set(true);
  }

  public TCGroupManager getTCGroupManager() {
    return manager;
  }

  public boolean isConnected() {
    return (connected.get());
  }

  public void close() {
    connected.set(false);
    getChannel().close();
  }
  
  public boolean highPriorityLink() {
    return(srcNodeID.compareTo(dstNodeID) > 0);
  }

}