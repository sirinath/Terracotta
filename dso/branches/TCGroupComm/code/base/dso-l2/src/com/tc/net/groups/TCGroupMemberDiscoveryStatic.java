/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.exception.ImplementMe;
import com.tc.net.protocol.tcm.MessageChannel;

import java.util.ArrayList;


public class TCGroupMemberDiscoveryStatic implements TCGroupMemberDiscovery {

  public TCGroupMemberDiscoveryStatic(L2TVSConfigurationSetupManager configSetupManager) {
    //
  }
  
  public void setupMembers(Node thisNode, Node[] allNodes) {
    throw new ImplementMe();
    
  }
  
  public ArrayList<TCGroupMember> getAllMembers() {
    throw new ImplementMe();
  }

  public ArrayList<TCGroupMember> getInactiveMembers() {
    throw new ImplementMe();
  }

  public void memberActivated(TCGroupMember member) {
    throw new ImplementMe();
  }

  public void memberDeactivated(TCGroupMember member) {
    throw new ImplementMe();
  }

  public boolean isMemberActivated(TCGroupMember member) {
    throw new ImplementMe();
  }

  
  public TCGroupMember getMember(NodeID nodeID) {
    throw new ImplementMe();
  }

  public boolean isMemberExist(TCGroupMember member) {
    throw new ImplementMe();
  }

  public TCGroupMember getMember(MessageChannel channel) {
    throw new ImplementMe();
  }

  public void memberAdded(NodeID nodeID) {
    throw new ImplementMe();
    
  }

  public void memberDisappeared(NodeID nodeID) {
    throw new ImplementMe();
    
  }

  public ArrayList<TCGroupMember> getCurrentMembers() {
    throw new ImplementMe();
  }

}