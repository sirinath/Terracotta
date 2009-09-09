/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.l1.impl;

import com.tc.logging.TCLogger;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.objectserver.l1.api.ClientState;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.ObjectIDSet;
import com.tc.util.State;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author steve
 */
public class ClientStateManagerImpl implements ClientStateManager {

  private static final State             STARTED = new State("STARTED");
  private static final State             STOPPED = new State("STOPPED");

  private State                          state   = STARTED;

  private final Map<NodeID, ClientState> clientStates;
  private final TCLogger                 logger;

  // for testing
  public ClientStateManagerImpl(final TCLogger logger, final Map<NodeID, ClientState> states) {
    this.logger = logger;
    this.clientStates = states;
  }

  public ClientStateManagerImpl(final TCLogger logger) {
    this(logger, new HashMap<NodeID, ClientState>());
  }

  public synchronized List<DNA> createPrunedChangesAndAddObjectIDTo(final Collection<DNA> changes,
                                                                    final BackReferences includeIDs, final NodeID id,
                                                                    final Set<ObjectID> lookupObjectIDs) {
    assertStarted();
    ClientStateImpl clientState = getClientState(id);
    if (clientState == null) {
      this.logger.warn(": createPrunedChangesAndAddObjectIDTo : Client state is NULL (probably due to disconnect) : "
                       + id);
      return Collections.emptyList();
    }

    List<DNA> prunedChanges = new LinkedList<DNA>();

    Set parents = includeIDs.getAllParents();
    for (final DNA dna : changes) {
      if (clientState.containsReference(dna.getObjectID())) {
        ObjectID oid = dna.getObjectID();
        if (dna.isDelta() && includeIDs.shouldBroadcast(clientState.getReferences(), oid)) {
          prunedChanges.add(dna);
        } else {
          // Ignore this parent as we are not interested in this one.
          parents.remove(oid);
        }
        // else if (clientState.containsParent(dna.getObjectID(), includeIDs)) {
        // these objects needs to be looked up from the client during apply
        // objectIDs.add(dna.getObjectID());
        // }
      }
    }
    parents.retainAll(clientState.getReferences());
    includeIDs.addReferencedChildrenTo(lookupObjectIDs, parents);
    clientState.removeReferencedObjectIDsFrom(lookupObjectIDs);

    return prunedChanges;
  }

  public synchronized void addReference(final NodeID id, final ObjectID objectID) {
    assertStarted();
    ClientStateImpl c = getClientState(id);
    if (c != null) {
      c.addReference(objectID);
    } else {
      this.logger.warn(": addReference : Client state is NULL (probably due to disconnect) : " + id);
    }
  }

  public synchronized void removeReferences(final NodeID id, final Set<ObjectID> removed) {
    assertStarted();
    ClientStateImpl c = getClientState(id);
    if (c != null) {
      c.removeReferences(removed);
    } else {
      this.logger.warn(": removeReferences : Client state is NULL (probably due to disconnect) : " + id);
    }
  }

  public synchronized boolean hasReference(final NodeID id, final ObjectID objectID) {
    ClientStateImpl c = getClientState(id);
    if (c != null) {
      return c.containsReference(objectID);
    } else {
      this.logger.warn(": hasReference : Client state is NULL (probably due to disconnect) : " + id);
      return false;
    }
  }

  public synchronized void addAllReferencedIdsTo(final Set<ObjectID> ids) {
    assertStarted();
    for (final ClientState s : this.clientStates.values()) {
      s.addReferencedIdsTo(ids);
    }
  }

  public synchronized void removeReferencedFrom(final NodeID id, final Set<ObjectID> oids) {
    ClientState cs = getClientState(id);
    if (cs == null) {
      this.logger.warn(": removeReferencedFrom : Client state is NULL (probably due to disconnect) : " + id);
      return;
    }
    Set<ObjectID> refs = cs.getReferences();

    oids.removeAll(refs);
  }

  /*
   * returns newly added references
   */
  public synchronized Set<ObjectID> addReferences(final NodeID id, final Set<ObjectID> oids) {
    ClientState cs = getClientState(id);
    if (cs == null) {
      this.logger.warn(": addReferences : Client state is NULL (probably due to disconnect) : " + id);
      return Collections.emptySet();
    }
    Set<ObjectID> refs = cs.getReferences();
    if (refs.isEmpty()) {
      refs.addAll(oids);
      return oids;
    }

    Set<ObjectID> newReferences = new HashSet<ObjectID>();
    for (ObjectID oid : oids) {
      if (refs.add(oid)) {
        newReferences.add(oid);
      }
    }
    return newReferences;
  }

  public synchronized void shutdownNode(final NodeID waitee) {
    if (!isStarted()) {
      // it's too late to remove the client from the database. On startup, this guy will fail to reconnect
      // within the timeout period and be slain.
      return;
    }
    this.clientStates.remove(waitee);
  }

  public synchronized void startupNode(final NodeID nodeID) {
    if (!isStarted()) { return; }
    Object old = this.clientStates.put(nodeID, new ClientStateImpl(nodeID));
    if (old != null) { throw new AssertionError("Client connected before disconnecting : old Client state = " + old); }
  }

  public synchronized void stop() {
    assertStarted();
    this.state = STOPPED;
    this.logger.info("ClientStateManager stopped.");
  }

  private boolean isStarted() {
    return this.state == STARTED;
  }

  private void assertStarted() {
    if (this.state != STARTED) { throw new AssertionError("Not started."); }
  }

  private ClientStateImpl getClientState(final NodeID id) {
    return (ClientStateImpl) this.clientStates.get(id);
  }

  public int getReferenceCount(final NodeID nodeID) {
    ClientState clientState = getClientState(nodeID);
    return clientState != null ? clientState.getReferences().size() : 0;
  }

  public synchronized Set<NodeID> getConnectedClientIDs() {
    return this.clientStates.keySet();
  }

  public synchronized PrettyPrinter prettyPrint(PrettyPrinter out) {
    PrettyPrinter rv = out;
    out.println(getClass().getName());
    out = out.duplicateAndIndent();
    out.indent().println("client states: ");
    out = out.duplicateAndIndent();
    for (NodeID key : this.clientStates.keySet()) {
      ClientState st = this.clientStates.get(key);
      out.indent().print(key + "=").visit(st).println();
    }
    return rv;
  }

  private static class ClientStateImpl implements PrettyPrintable, ClientState {
    private final NodeID        nodeID;
    private final Set<ObjectID> managed = new ObjectIDSet();

    public ClientStateImpl(final NodeID nodeID) {
      this.nodeID = nodeID;
    }

    public void removeReferencedObjectIDsFrom(final Set<ObjectID> lookupObjectIDs) {
      lookupObjectIDs.removeAll(this.managed);
    }

    @Override
    public String toString() {
      return "ClientStateImpl[" + this.nodeID + ", " + this.managed + "]";
    }

    public Set<ObjectID> getReferences() {
      return this.managed;
    }

    public PrettyPrinter prettyPrint(final PrettyPrinter out) {
      out.println(getClass().getName());
      out.duplicateAndIndent().indent().print("managed: ").visit(this.managed);
      return out;
    }

    public void addReference(final ObjectID id) {
      if (!id.isNull()) {
        this.managed.add(id);
      }
    }

    public boolean containsReference(final ObjectID id) {
      return this.managed.contains(id);
    }

    public void removeReferences(final Set<ObjectID> references) {
      this.managed.removeAll(references);
    }

    public void addReferencedIdsTo(final Set<ObjectID> ids) {
      ids.addAll(this.managed);
    }

    public NodeID getNodeID() {
      return this.nodeID;
    }
  }

}
