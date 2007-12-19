/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.exception.ImplementMe;
import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.util.UUID;

/*
 * Each TCGroupMember sit on top of a channel. 
 */
public class TCGroupMemberImpl implements TCGroupMember, ChannelEventListener {
  private MessageChannel channel;
  private NodeID nodeID;
  private UUID uuid;
  private boolean alive;
  
  /*
   * Member can be created in two ways
   * By making a connection from this node.
   * By listen port of this node.
   */
  public TCGroupMemberImpl(MessageChannel channel) {
    this.channel = channel;
    this.nodeID = channel.getChannelID().getNodeID();
  }

  public void openChannel() {
    throw new ImplementMe();
  }

  public void closeChannel() {
    throw new ImplementMe();
  }
  
  public MessageChannel getChannel() {
    return channel;
  }

  public NodeID getNodeID() {
    throw new ImplementMe();
  }

  /*
   * Use a wrapper to send old tribe GroupMessage out through channel's TCMessage
   */
  public void send(GroupMessage msg) {
    TCGroupMessageWrapper wrapper = (TCGroupMessageWrapper) channel.createMessage(TCMessageType.GROUP_WRAPPER_MESSAGE);
    wrapper.setGroupMessage(msg);
    wrapper.send();
  }

  public void notifyChannelEvent(ChannelEvent event) {
    throw new ImplementMe();
  }

  public boolean isActive() {
    return alive;
  }
  
}