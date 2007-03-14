/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.transport.ConnectionIdFactory;
import com.tc.objectserver.persistence.api.PersistentMapStore;
import com.tc.util.State;
import com.tc.util.sequence.ObjectIDSequence;

public class ClusterState {

  private static final TCLogger     logger            = TCLogging.getLogger(ClusterState.class);

  private static final String       L2_STATE_KEY      = "L2_STATE_KEY";

  private final PersistentMapStore  clusterStateStore;
  private final ObjectIDSequence    oidSequence;
  private final ConnectionIdFactory connectionIdFactory;

  private long                      nextAvailObjectID = -1;
  private State                     currentState;

  public ClusterState(PersistentMapStore clusterStateStore, ObjectIDSequence oidSequence,
                      ConnectionIdFactory connectionIdFactory) {
    this.clusterStateStore = clusterStateStore;
    this.oidSequence = oidSequence;
    this.connectionIdFactory = connectionIdFactory;
  }

  public void setNextAvailableObjectID(long nextAvailOID) {
    if (nextAvailOID < nextAvailObjectID) {
      // Could happen when two actives fight it out. Dont want to assert, let the state manager fight it out.
      logger.error("Trying to set Next Available ObjectID to a lesser value : known = " + nextAvailObjectID
                   + " new value = " + nextAvailOID + " IGNORING");
      return;
    }
    this.nextAvailObjectID = nextAvailOID;
  }

  public long getNextAvailableObjectID() {
    return nextAvailObjectID;
  }

  public void syncInternal() {
    syncOIDSequence();
  }

  private void syncOIDSequence() {
    long nextOID = getNextAvailableObjectID();
    if (nextOID != -1) {
      logger.info("Setting the Next Available OID to " + nextOID);
      this.oidSequence.setNextAvailableObjectID(nextOID);
    }
  }

  public void setCurrentState(State state) {
    this.currentState = state;
    syncCurrentStateToDB();
  }

  private void syncCurrentStateToDB() {
    clusterStateStore.put(L2_STATE_KEY, currentState.getName());
  }
}
