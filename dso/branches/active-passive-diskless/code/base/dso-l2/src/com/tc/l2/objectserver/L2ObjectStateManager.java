/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArraySet;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.NodeID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.util.State;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class L2ObjectStateManager {

  private static final TCLogger logger = TCLogging.getLogger(L2ObjectStateManager.class);

  CopyOnWriteArraySet           nodes  = new CopyOnWriteArraySet();

  public void removeL2(NodeID nodeID) {
    for (Iterator i = nodes.iterator(); i.hasNext();) {
      L2ObjectState l2State = (L2ObjectState) i.next();
      if (nodeID.equals(l2State.nodeID)) {
        i.remove();
        return;
      }
    }
    logger.warn("L2State Not found for " + nodeID);
  }

  public int setExistingObjectsList(NodeID nodeID, Set oids, ObjectManager objectManager) {
    L2ObjectState l2State = new L2ObjectState(nodeID);
    nodes.add(l2State);
    int missing = l2State.initialize(oids, objectManager);
    return missing;
  }

  public boolean addAndRemoveSomeMissingOIDsTo(NodeID nodeID, Set oids, int count) {
    L2ObjectState l2State = getState(nodeID);
    if (l2State != null) {
      return l2State.addAndRemoveSomeMissingOIDsTo(oids, count);
    } else {
      logger.warn("L2 State Object Not found for " + nodeID);
      return false;
    }
  }

  private L2ObjectState getState(NodeID nodeID) {
    for (Iterator i = nodes.iterator(); i.hasNext();) {
      L2ObjectState l2State = (L2ObjectState) i.next();
      if (nodeID.equals(l2State.nodeID)) { return l2State; }
    }
    return null;
  }

  private static final class L2ObjectState {

    private final NodeID       nodeID;
    // XXX:: Tracking just the missing Oids is better in terms of memory overhead, but this might lead to difficult race
    // conditions. Rethink !!
    private Set                missingOids;

    private State              state         = UNINITIALIZED;

    private static final State UNINITIALIZED = new State("UNINITALIZED");
    private static final State INITIALIZED   = new State("INITALIZED");

    public L2ObjectState(NodeID nodeID) {
      this.nodeID = nodeID;
    }

    public synchronized boolean addAndRemoveSomeMissingOIDsTo(Set oids, int count) {
      for (Iterator i = missingOids.iterator(); i.hasNext();) {
        oids.add(i.next());
        i.remove();
        if (--count == 0) {
          break;
        }
      }
      return !oids.isEmpty();
    }

    public synchronized int initialize(Set oidsFromL2, ObjectManager objectManager) {
      this.missingOids = objectManager.getAllObjectIDs();
      Set missingHere = new HashSet();
      for (Iterator i = oidsFromL2.iterator(); i.hasNext();) {
        Object o = i.next();
        if (!missingOids.remove(o)) {
          missingHere.add(o);
        }
      }
      logger.info(nodeID + " : is missing " + missingOids.size() + " Objects");
      if (!missingHere.isEmpty()) {
        // XXX:: This is possible because some message (Transaction message with new object creation or object delete
        // message from GC) from previous active reached the other node and not this node and the active crashed
        logger.warn("Object IDs MISSING HERE : " + missingHere.size() + " : " + missingHere);
      }
      state = INITIALIZED;
      notifyAll();
      return missingOids.size();
    }
  }
}
