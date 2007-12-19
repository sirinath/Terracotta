/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.protocol.tcm.MessageChannel;

public interface TCGroupMembership {
  /*
   * Whenever a new member joins, need to resolve two way connections into one.
   * A resolve policy shall be provided.
   */
  public void add(TCGroupMember member);
  
  public void remove(TCGroupMember member);
  
  public boolean isExist(TCGroupMember member);
  
  public void clean();
  
  public NodeID join(Node thisNode, Node[] allNodes);
  
  public void sendTo(NodeID node, GroupMessage msg);
  
  public void sendAll(GroupMessage msg);
  
  /*
   * Proactively open a channel to other node
   */
  public void openChannel(ConnectionAddressProvider addrProvider);
  
  public void closeChannel(TCGroupMember member);
  
  public void closeChannel(MessageChannel channel);

}