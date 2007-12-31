/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.protocol.tcm.MessageChannel;


public interface TCGroupMember {
  public NodeID getNodeID();
  
  public MessageChannel getChannel();
  
  public void send(GroupMessage msg);
  
  public boolean isActive();
  
  public void activate();
  
  public void deactivate();
}