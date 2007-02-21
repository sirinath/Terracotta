/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

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

  ConcurrentHashMap             nodes  = new ConcurrentHashMap();

  public void addL2(NodeID nodeID) {
    nodes.put(nodeID, new L2ObjectState(nodeID));
  }

  public void removeL2(NodeID nodeID) {
    nodes.remove(nodeID);
  }

  public int setExistingObjectsList(NodeID nodeID, Set objectIDs, ObjectManager objectManager) {
    L2ObjectState l2State = (L2ObjectState) nodes.get(nodeID);
    if (l2State != null) {
      return l2State.initialize(objectIDs, objectManager);
    } else {
      logger.warn("L2 State Object Not found for " + nodeID);
      return -1;
    }
  }

  private static final class L2ObjectState {

    private static final State UNINITIALIZED = new State("UNINITIALIZED");
    private static final State INITIALIZED   = new State("INITIALIZED");

    private final NodeID       nodeID;
    private State              state         = UNINITIALIZED;
    private Set                missingOids;

    public L2ObjectState(NodeID nodeID) {
      this.nodeID = nodeID;
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
      return missingOids.size();
    }
  }
}
