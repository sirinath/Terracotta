/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.protocol.tcm.MessageChannel;

import java.util.Set;

public interface ClientHandshakeAckMessage {
  public void send();

  public boolean getPersistentServer();

  public void initialize(boolean persistent, Set allNodes, String thisNodeID, String serverVersion,
                         ServerID serverNodeID);

  public MessageChannel getChannel();

  public String[] getAllNodes();

  public String getThisNodeId();

  public String getServerVersion();

  public NodeID getSourceNodeID();

  public ServerID getServerNodeID();
}
