/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.exception.ImplementMe;
import com.tc.net.protocol.tcm.MessageChannel;

import java.util.ArrayList;
import java.util.List;


public class TCGroupMemberDiscoveryStatic implements TCGroupMemberDiscovery {
  private ArrayList<TCGroupMember> members = new ArrayList<TCGroupMember>();
  
  public TCGroupMemberDiscoveryStatic(L2TVSConfigurationSetupManager configSetupManager) {
    //
  }
  
  public void setupMembers(Node thisNode, Node[] allNodes) {
    throw new ImplementMe();
    
  }
  
  public List<TCGroupMember> getAllMembers() {
    throw new ImplementMe();
  }

  public List<TCGroupMember> getInactiveMembers() {
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
    TCGroupMember member = null;
    for(int i = 0; i < members.size(); ++i) {
      TCGroupMember m = members.get(i);
      if (m.getChannel() == channel ) {
        member = m;
        break;
      }
    }
    return (member);
  }

  public void memberAdded(TCGroupMember member) {
    // Keep only one connection between two nodes. Close the redundant one.
    for (int i = 0; i < members.size(); ++i) {
      TCGroupMember m = members.get(i);
      if (member.getSrcNodeID().equals(m.getDstNodeID()) && member.getDstNodeID().equals(m.getSrcNodeID())) {
        // already one connection established, choose one to keep
        int order = member.getSrcNodeID().compareTo(member.getDstNodeID());
        if (order > 0) {
          // choose new connection
          m.close();
          members.remove(m);
        } else if (order < 0) {
          // keep original one
          member.close();
          return;
        } else {
          throw new RuntimeException("SrcNodeID equals DstNodeID");
        }
      }
    }
    members.add(member);
  }

  public void memberDisappeared(TCGroupMember member) {
    members.remove(member);
  }

  public List<TCGroupMember> getCurrentMembers() {
    throw new ImplementMe();
  }
  
  public int size() {
    return members.size();
  }

}