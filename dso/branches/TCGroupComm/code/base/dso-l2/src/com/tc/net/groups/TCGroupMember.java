/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.protocol.tcm.MessageChannel;


public interface TCGroupMember {
  public NodeID getNodeID();
  
  /*
   * Establish connection with this node.
   */
  public void openChannel();
  
  public void closeChannel();
  
  public MessageChannel getChannel();
  
  public void send(GroupMessage msg);
  
  boolean isActive();
}