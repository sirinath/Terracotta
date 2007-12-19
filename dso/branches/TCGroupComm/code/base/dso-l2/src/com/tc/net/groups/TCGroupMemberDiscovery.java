/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.protocol.tcm.MessageChannel;

import java.util.ArrayList;


public interface TCGroupMemberDiscovery {
  
  /*
   * Import members, allow redundant calls
   */
  public void setupMembers(Node thisNode, Node[] allNodes);
  
  public ArrayList<TCGroupMember> getAllMembers();
  
  public ArrayList<TCGroupMember> getInactiveMembers();
  
  public void memberActivated(TCGroupMember member);
  
  public void memberDeactivated(TCGroupMember member);
  
  public boolean isMemberActivated(TCGroupMember member);
  
  public boolean isMemberExist(TCGroupMember member);
  
  public TCGroupMember getMember(NodeID nodeID);
  
  public TCGroupMember getMember(MessageChannel channel);
  
  public void memberAdded(NodeID nodeID);
  
  public void memberDisappeared(NodeID nodeID);
  
  public ArrayList<TCGroupMember> getCurrentMembers();

}