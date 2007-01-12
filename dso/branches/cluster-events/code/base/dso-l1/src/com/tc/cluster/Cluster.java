/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cluster;

import java.util.HashMap;
import java.util.Map;

public class Cluster {

  private final Map nodes;

  private Node      thisNode;

  public Cluster() {
    nodes = new HashMap();
  }

  public Node getThisNode() {
    return thisNode;
  }

  public Map getNodes() {
    return nodes;
  }

//  public void thisNodeConnected(final String thisNodeId) {
//    thisNode = new Node(thisNodeId);
//    nodes.put(thisNode.getNodeId(), thisNode);
//    // FIXME: raise event
//    log("thisNodeConnected", thisNodeId);
//  }
  
  public void thisNodeConnected(final String thisNodeId, String[] nodesCurrentlyInCluster) {
    thisNode = new Node(thisNodeId);
    nodes.put(thisNode.getNodeId(), thisNode);
    for (int i = 0; i < nodesCurrentlyInCluster.length; i++) {
      Node n = new Node(nodesCurrentlyInCluster[i]);
      nodes.put(n.getNodeId(), n);
    }
    // FIXME: raise event
    log("thisNodeConnected", thisNodeId);
  }

  public void thisNodeDisconnected() {
    nodes.clear();
    // FIXME: raise event
    log("thisNodeDisconnected", thisNode.getNodeId());
  }

  public void nodeConnected(String nodeId) {
    Node n = new Node(nodeId);
    nodes.put(n.getNodeId(), n);
    // FIXME: fire event
    log("nodeConnected", nodeId);
  }

  public void nodeDisconnected(String nodeId) {
    nodes.remove(nodeId);
    // FIXME: fire event
    log("nodeDisconnected", nodeId);
  }

  private void log(String event, String nodeId) {
    System.err.println("\n\n###################################\n" + event + ": nodeId = " + nodeId + " cluster -> " + this + "\n"
                       + "###################################\n\n");
    
  }
  
  public String toString() {
    return "Cluster{ thisNode=" + thisNode + ", nodesInCluster=" + nodes.keySet() + "}";
  }
}
