/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.groups.NodeID;


public interface TransportHandshakeMessage extends WireProtocolMessage {
  public ConnectionID getConnectionId();

  public boolean isMaxConnectionsExceeded();

  public int getMaxConnections();
  
  // currently used by TC-Grouop-Comm
  public NodeID getNodeID();
  
  // XXX: Yuck.
  public boolean isSyn();
  public boolean isSynAck();
  public boolean isAck();
}
