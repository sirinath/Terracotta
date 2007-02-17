/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.l2.msg.ClusterStateMessage;
import com.tc.net.groups.NodeID;

public interface ElectionManager {

  public NodeID runElection(NodeID myNodeId, boolean isNew);

  public void declareWinner(NodeID myNodeId);
  
  public void handleStartElectionRequest(ClusterStateMessage msg);

  public void handleElectionAbort(ClusterStateMessage msg);

  public void handleElectionResultMessage(ClusterStateMessage msg);

  public void reset();

}
