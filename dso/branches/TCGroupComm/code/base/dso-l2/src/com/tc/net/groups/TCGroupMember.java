/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.protocol.tcm.MessageChannel;

public interface TCGroupMember {

  public NodeIdUuidImpl getSrcNodeID();

  public NodeIdUuidImpl getDstNodeID();

  public NodeIdUuidImpl getPeerNodeID();

  public MessageChannel getChannel();

  public void send(GroupMessage msg) throws GroupException;

  public void setTCGroupManager(TCGroupManagerImpl manager);

  public TCGroupManagerImpl getTCGroupManager();

  public boolean isConnected();

  public void close();

  public boolean highPriorityLink();
}