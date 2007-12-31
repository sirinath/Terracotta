/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.net.groups.NodeID;
import com.tc.util.AbstractIdentifier;

public class NetworkStackID extends AbstractIdentifier {
  private NodeID nodeID;

  private NetworkStackID() {
    super();
  }

  public NetworkStackID(long id) {
    super(id);
  }

  public String getIdentifierType() {
    return "NetworkStackID";
  }
  
  public void setNodeID(NodeID nodeID) {
    this.nodeID = nodeID;
  }
  
  public NodeID getNodeID() {
    return nodeID;
  }

}
