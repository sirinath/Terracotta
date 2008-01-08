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

public interface TCGroupMembership {

  public boolean isExist(TCGroupMember member);

  public NodeID join(Node thisNode, Node[] allNodes) throws GroupException;

  public void sendTo(NodeID node, GroupMessage msg);

  public void sendAll(GroupMessage msg);

  public GroupResponse sendAllAndWaitForResponse (GroupMessage msg) throws GroupException;
  
  public GroupMessage sendToAndWaitForResponse(NodeID nodeID, GroupMessage msg) throws GroupException;
  
  /*
   * Proactively open a channel to other node
   */
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
  
  public void registerForGroupEvents(GroupEventsListener listener);
  
  public void memberAdded(TCGroupMember member);
  
  public void memberDisappeared(TCGroupMember member);
  
  public void registerForMessages(Class msgClass, GroupMessageListener listener);
  
  public void messageReceived(GroupMessage message, MessageChannel channel);

  public void setZapNodeRequestProcessor(ZapNodeRequestProcessor processor);
  
  public void zapNode(NodeID nodeID, int type, String reason);
}