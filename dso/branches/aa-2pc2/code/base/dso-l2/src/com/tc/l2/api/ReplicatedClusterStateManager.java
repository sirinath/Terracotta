/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.api;

import com.tc.net.NodeID;
import com.tc.net.groups.GroupException;

public interface ReplicatedClusterStateManager {

  public void publishNextAvailableObjectID(long l);

  public void publishNextAvailableGlobalTransactionID(long l);

  public void goActiveAndSyncState();

  public void publishClusterState(NodeID nodeID) throws GroupException;

  public void fireNodeLeftEvent(NodeID nodeID);

}
