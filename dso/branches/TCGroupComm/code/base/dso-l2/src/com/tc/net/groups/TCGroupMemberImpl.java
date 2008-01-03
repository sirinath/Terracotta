/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelEventType;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;

/*
 * Each TCGroupMember sits on top of a channel.
 */
public class TCGroupMemberImpl implements TCGroupMember, ChannelEventListener {
  private TCGroupMembership    membership;
  private final MessageChannel channel;
  private final NodeID         srcNodeID;
  private final NodeID         dstNodeID;
  private boolean              connected = true;

  /*
   * Member established by this node, srcNodeID.
   */
  public TCGroupMemberImpl(NodeID srcNodeID, MessageChannel channel) {
    this.channel = channel;
    this.srcNodeID = srcNodeID;
    this.dstNodeID = channel.getChannelID().getNodeID();
    this.channel.addListener(this);
  }

  /*
   * Member established by dstNodeID.
   */
  public TCGroupMemberImpl(MessageChannel channel, NodeID dstNodeID) {
    this.channel = channel;
    this.srcNodeID = channel.getChannelID().getNodeID();
    this.dstNodeID = dstNodeID;
    this.channel.addListener(this);
  }

  public MessageChannel getChannel() {
    return channel;
  }

  /*
   * Use a wrapper to send old tribes GroupMessage out through channel's TCMessage
   */
  public void send(GroupMessage msg) {
    TCGroupMessageWrapper wrapper = (TCGroupMessageWrapper) channel.createMessage(TCMessageType.GROUP_WRAPPER_MESSAGE);
    wrapper.setGroupMessage(msg);
    wrapper.send();
  }

  public void notifyChannelEvent(ChannelEvent event) {
    if (event.getChannel() == channel) {
      if (event.getType() == ChannelEventType.TRANSPORT_CONNECTED_EVENT) {
        connected = true;
      } else if (event.getType() == ChannelEventType.TRANSPORT_DISCONNECTED_EVENT) {
        connected = false;
      } else if (event.getType() == ChannelEventType.CHANNEL_CLOSED_EVENT) {
        connected = false;
        if (membership != null) membership.remove(this);
      }
    }
  }

  public NodeID getSrcNodeID() {
    return srcNodeID;
  }

  public NodeID getDstNodeID() {
    return dstNodeID;
  }

  public void setTCGroupMembership(TCGroupMembership membership) {
    this.membership = membership;
  }

  public TCGroupMembership getTCGroupMembership() {
    return membership;
  }

  public boolean isConnected() {
    return connected;
  }

  public void close() {
    getChannel().close();
  }

}