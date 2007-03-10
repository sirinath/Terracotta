/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.l2.api;

import com.tc.net.groups.NodeID;

public interface ReplicatedClusterStateManager {

  public void publishNextAvailableObjectID(long l);

  public void sync();

  public void publishClusterState(NodeID nodeID);

}
