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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class L2ObjectStateManager {

  private static final TCLogger logger = TCLogging.getLogger(L2ObjectStateManager.class);

  CopyOnWriteArraySet nodes = new CopyOnWriteArraySet();
  
  public void removeL2(NodeID nodeID) {
    for (Iterator i = nodes.iterator(); i.hasNext();) {
      L2ObjectState l2State = (L2ObjectState) i.next();
      if(nodeID.equals(l2State.nodeID)) {
        i.remove();
        return;
      }
    }
    logger.warn("L2State Not found for " + nodeID);
  }

  public int setExistingObjectsList(NodeID nodeID, Set oids, ObjectManager objectManager) {
    L2ObjectState l2State = new L2ObjectState(nodeID);
    int missing = l2State.initialize(oids, objectManager);
    nodes.add(l2State);
    return missing;
  }

  private static final class L2ObjectState {

    private final NodeID       nodeID;
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
      return missingOids.size();
    }
  }
}
