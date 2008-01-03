/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

public interface TCGroupMembership {
  /*
   * Whenever a new member joins, need to resolve two way connections into one. A resolve policy shall be provided.
   */
  public void add(TCGroupMember member);

  public void remove(TCGroupMember member);

  public boolean isExist(TCGroupMember member);

  public void clean();

  public NodeID join(Node thisNode, Node[] allNodes) throws GroupException;

  public void sendTo(NodeID node, GroupMessage msg);

  public void sendAll(GroupMessage msg);

  /*
   * Proactively open a channel to other node
   */
  public TCGroupMember openChannel(ConnectionAddressProvider addrProvider) throws TCTimeoutException, UnknownHostException,
      MaxConnectionsExceededException, IOException;

  public TCGroupMember openChannel(String hostname, int groupPort) throws TCTimeoutException, UnknownHostException,
      MaxConnectionsExceededException, IOException;

  public void closeChannel(TCGroupMember member);

  public void closeChannel(MessageChannel channel);
  
  public void start() throws IOException ;
  
  public NodeID getNodeID();
  
  public TCGroupMember getMember(MessageChannel channel);
  
  public TCGroupMember getMember(NodeID nodeID);

  public List<TCGroupMember> getMembers();
  
  public void setDiscover(TCGroupMemberDiscovery dicover);
  
  public TCGroupMemberDiscovery getDiscover();
  
  public void shutdown();
  
  public int size();

}