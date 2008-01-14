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
import java.util.Set;

public interface TCGroupManager extends GroupManager {
  
  public String makeGroupNodeName(String hostname, int groupPort);
  
  public boolean isExist(TCGroupMember member);

  public TCGroupMember openChannel(ConnectionAddressProvider addrProvider) throws TCTimeoutException, UnknownHostException,
      MaxConnectionsExceededException, IOException;

  public TCGroupMember openChannel(String hostname, int groupPort) throws TCTimeoutException, UnknownHostException,
      MaxConnectionsExceededException, IOException;

  public void closeChannel(TCGroupMember member);

  public void closeChannel(MessageChannel channel);
  
  public void closeAllChannels();
  
  public void start(Set initialConnectionIDs) throws IOException ;
  
  public void stop(long timeout) throws TCTimeoutException;
  
  public NodeID getNodeID();
  
  public TCGroupMember getMember(MessageChannel channel);
  
  public TCGroupMember getMember(NodeID nodeID);
  
  public List<TCGroupMember> getMembers();
  
  public void setDiscover(TCGroupMemberDiscovery dicover);
  
  public TCGroupMemberDiscovery getDiscover();
  
  public void shutdown();
  
  public int size();
  
  public void memberAdded(TCGroupMember member);
  
  public void memberDisappeared(TCGroupMember member);
  
  public void messageReceived(GroupMessage message, MessageChannel channel);
  
  public void pingReceived(TCGroupPingMessage msg);

}