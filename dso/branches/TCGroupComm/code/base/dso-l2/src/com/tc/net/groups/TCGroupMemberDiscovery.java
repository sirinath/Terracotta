/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.protocol.tcm.MessageChannel;

import java.util.List;


public interface TCGroupMemberDiscovery {
  
  /*
   * Import members, allow redundant calls
   */
  public void setupMembers(Node thisNode, Node[] allNodes);
  
  public List<TCGroupMember> getAllMembers();
  
  public List<TCGroupMember> getInactiveMembers();
  
  public void memberActivated(TCGroupMember member);
  
  public void memberDeactivated(TCGroupMember member);
  
  public boolean isMemberActivated(TCGroupMember member);
  
  public boolean isMemberExist(TCGroupMember member);
  
  public TCGroupMember getMember(NodeID nodeID);
  
  /*
   * Find member by MessageChannel
   */
  public TCGroupMember getMember(MessageChannel channel);
  
  public void memberAdded(TCGroupMember member);
  
  public void memberDisappeared(TCGroupMember member);
  
  public List<TCGroupMember> getCurrentMembers();
  
  public int size();

}