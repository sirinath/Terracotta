/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.cluster;

import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tcclient.cluster.DsoNode;
import com.tcclient.cluster.DsoNodeImpl;
import com.tcclient.cluster.DsoNodeInternal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DsoClusterTopologyImpl implements DsoClusterTopology {
  private final Map<NodeID, DsoNodeInternal>     nodes          = new HashMap<NodeID, DsoNodeInternal>();

  private final ReentrantReadWriteLock           nodesLock      = new ReentrantReadWriteLock();
  private final ReentrantReadWriteLock.ReadLock  nodesReadLock  = nodesLock.readLock();
  private final ReentrantReadWriteLock.WriteLock nodesWriteLock = nodesLock.writeLock();

  Collection<DsoNodeInternal> getInternalNodes() {
    nodesReadLock.lock();
    try {
      return Collections.unmodifiableCollection(new ArrayList<DsoNodeInternal>(nodes.values()));
    } finally {
      nodesReadLock.unlock();
    }
  }

  DsoNodeInternal getInternalNode(NodeID nodeId) {
    nodesReadLock.lock();
    try {
      return nodes.get(nodeId);
    } finally {
      nodesReadLock.unlock();
    }
  }

  @Override
  public Collection<DsoNode> getNodes() {
    nodesReadLock.lock();
    try {
      return Collections.unmodifiableCollection(new ArrayList<DsoNode>(nodes.values()));
    } finally {
      nodesReadLock.unlock();
    }
  }

  boolean containsDsoNode(final NodeID nodeId) {
    nodesReadLock.lock();
    try {
      return nodes.containsKey(nodeId);
    } finally {
      nodesReadLock.unlock();
    }
  }

  DsoNodeInternal getAndRegisterDsoNode(final ClientID nodeId) {
    nodesReadLock.lock();
    try {
      DsoNodeInternal node = nodes.get(nodeId);
      if (node != null) { return node; }
    } finally {
      nodesReadLock.unlock();
    }

    return registerDsoNode(nodeId);
  }

  DsoNodeInternal getAndRemoveDsoNode(final NodeID nodeId) {
    nodesWriteLock.lock();
    try {
      DsoNodeInternal node = nodes.remove(nodeId);
      return node;
    } finally {
      nodesWriteLock.unlock();
    }
  }

  DsoNodeInternal registerDsoNode(final ClientID nodeId) {
    return registerDsoNodeBase(nodeId, false);
  }

  DsoNodeInternal registerThisDsoNode(final ClientID nodeId) {
    return registerDsoNodeBase(nodeId, true);
  }

  DsoNodeInternal updateOnRejoin(final ClientID thisNodeId, final NodeID[] clusterMembers) {
    nodesWriteLock.lock();
    try {
      nodes.clear();
      for (NodeID otherNode : clusterMembers) {
        if (!thisNodeId.equals(otherNode)) {
          registerDsoNodeBase((ClientID) otherNode, false);
        }
      }
      return registerDsoNodeBase(thisNodeId, true);
    } finally {
      nodesWriteLock.unlock();
    }
  }

  private DsoNodeInternal registerDsoNodeBase(final ClientID clientId, boolean isLocalNode) {
    final DsoNodeInternal node = new DsoNodeImpl(clientId.toString(), clientId.toLong(), isLocalNode);

    nodesWriteLock.lock();
    try {
      nodes.put(clientId, node);
      return node;
    } finally {
      nodesWriteLock.unlock();
    }
  }

}