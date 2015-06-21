/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.l1.impl;

import com.tc.invalidation.Invalidations;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.objectserver.l1.api.ClientState;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.l1.api.ObjectReferenceAddListener;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Client State Manager maintains the list of objects that are faulted into each client.
 */
public class ClientStateManagerImpl implements ClientStateManager, PrettyPrintable {
  private static final TCLogger logger = TCLogging.getLogger(ClientStateManagerImpl.class);

  private final ConcurrentHashMap<NodeID, ClientStateImpl>      clientStates;
  private final CopyOnWriteArraySet<ObjectReferenceAddListener> objectRefsAddListener;

  public ClientStateManagerImpl() {
    this.clientStates = new ConcurrentHashMap<NodeID, ClientStateImpl>();
    this.objectRefsAddListener = new CopyOnWriteArraySet<ObjectReferenceAddListener>();
  }

  @Override
  public List<DNA> createPrunedChangesAndAddObjectIDTo(final Collection<DNA> changes,
                                                       final ApplyTransactionInfo applyInfo, final NodeID id,
                                                       final Set<ObjectID> lookupObjectIDs,
                                                       final Invalidations invalidationsForClient) {
    final ClientStateImpl clientState = getClientState(id);
    if (clientState == null) {
      logger.warn(": createPrunedChangesAndAddObjectIDTo : Client state is NULL (probably due to disconnect) : "
                       + id);
      return Collections.emptyList();
    }

    boolean isEcho = id.equals(applyInfo.getServerTransactionID().getSourceID());

    clientState.lock();
    try {
      final List<DNA> prunedChanges = new LinkedList<DNA>();

      for (final DNA dna : changes) {
        final ObjectID oid = dna.getObjectID();
        if (isEcho && !applyInfo.getObjectsToEchoChangesFor().contains(oid)) {
          continue;
        }
        if (clientState.containsReference(oid)) {
          if (dna.isDelta() && !applyInfo.isBroadcastIgnoredFor(oid)) {
            prunedChanges.add(dna);
          } else {
            // This new Object must have already been sent as a part of a different lookup. So ignoring this change.
          }
          // else if (clientState.containsParent(dna.getObjectID(), includeIDs)) {
          // these objects needs to be looked up from the client during apply
          // objectIDs.add(dna.getObjectID());
          // }
        }
      }
      clientState.addReferencedChildrenTo(lookupObjectIDs, applyInfo);
      clientState.removeReferencedObjectIDsFrom(lookupObjectIDs);

      if (!isEcho) {
        // Don't need to echo invalidations
        addInvalidateObjectIDsTo(clientState, invalidationsForClient, applyInfo.getObjectIDsToInvalidate());
      }

      return prunedChanges;
    } finally {
      clientState.unlock();
    }
  }

  private void addInvalidateObjectIDsTo(ClientStateImpl clientState, Invalidations invalidationsForClient,
                                        Invalidations allInvalidations) {
    if (allInvalidations == null || allInvalidations.isEmpty()) return;
    Set<ObjectID> mapIDs = allInvalidations.getMapIds();
    for (ObjectID mapID : mapIDs) {
      ObjectIDSet invalidatedOids = allInvalidations.getObjectIDSetForMapId(mapID);
      for (ObjectID objectID : invalidatedOids) {
        if (clientState.containsReference(objectID)) {
          invalidationsForClient.add(mapID, objectID);
        }
      }
    }
  }

  @Override
  public boolean addReference(final NodeID id, final ObjectID objectID) {
    final ClientStateImpl c = getClientState(id);
    if (c != null) {
      c.lock();
      try {
        if (!c.addReference(objectID) ) {
          return false;
        }
        
      } finally {
        c.unlock();
      }
      for (ObjectReferenceAddListener listener : this.objectRefsAddListener) {
        listener.objectReferenceAdded(objectID);
      }
    } else {
      logger.warn(": addReference : Client state is NULL (probably due to disconnect) : " + id);
      return false;
    }
    return true;
  }

  @Override
  public void registerObjectReferenceAddListener(ObjectReferenceAddListener listener) {
    boolean added = this.objectRefsAddListener.add(listener);
    if (!added) {
      logger.warn("Object Reference Add Listener " + listener + " already registered.");
    }
  }

  @Override
  public void unregisterObjectReferenceAddListener(ObjectReferenceAddListener listener) {
    boolean removed = this.objectRefsAddListener.remove(listener);
    if (!removed) {
      logger.warn("Object Reference Add Listener " + listener + " not in registered set.");
    }

  }
  /**
   * From the local state of the l1 named nodeID remove all the objectIDs that are references and also remove from the
   * requested list any refrence already present
   * 
   * @param id nodeID of the client requesting the objects
   * @param removed set of objects removed from the client
   * @param requested set of Objects requested, this set is mutated to remove any object that is already present in the
   *        client.
   */
  @Override
  public void removeReferences(final NodeID id, final Set<ObjectID> removed, final Set<ObjectID> requested) {
    final ClientStateImpl c = getClientState(id);
    if (c != null) {
      c.lock();
      try {
        c.removeReferences(removed);
        c.removeReferencedObjectIDsFrom(requested);
      } finally {
        c.unlock();
      }
    } else {
      logger.warn(": removeReferences : Client state is NULL (probably due to disconnect) : " + id);
    }
  }

  @Override
  public boolean hasReference(final NodeID id, final ObjectID objectID) {
    final ClientStateImpl c = getClientState(id);
    if (c != null) {
      c.lock();
      try {
        return c.containsReference(objectID);
      } finally {
        c.unlock();
      }
    } else {
      logger.warn(": hasReference : Client state is NULL (probably due to disconnect) : " + id);
      return false;
    }
  }

  @Override
  public Set<ObjectID> addAllReferencedIdsTo(final Set<ObjectID> ids) {
    for (final ClientStateImpl c : this.clientStates.values()) {
      c.lock();
      try {
        c.addReferencedIdsTo(ids);
      } finally {
        c.unlock();
      }
    }
    return ids;
  }

  @Override
  public void removeReferencedFrom(final NodeID id, final Set<ObjectID> oids) {
    final ClientStateImpl c = getClientState(id);
    if (c == null) {
      this.logger.warn(": removeReferencedFrom : Client state is NULL (probably due to disconnect) : " + id);
      return;
    }
    c.lock();
    try {
      final Set<ObjectID> refs = c.getReferences();
      oids.removeAll(refs);
    } finally {
      c.unlock();
    }
  }

  /*
   * returns newly added references
   */
  @Override
  public Set<ObjectID> addReferences(final NodeID id, final Set<ObjectID> oids) {
    final ClientStateImpl c = getClientState(id);
    if (c == null) {
      this.logger.warn(": addReferences : Client state is NULL (probably due to disconnect) : " + id);
      return Collections.emptySet();
    }
    final Set<ObjectID> newReferences = new HashSet<ObjectID>();
    c.lock();
    try {
      final Set<ObjectID> refs = c.getReferences();
      if (refs.isEmpty()) {
        refs.addAll(oids);
        return oids;
      }

      for (final ObjectID oid : oids) {
        if (refs.add(oid)) {
          newReferences.add(oid);
        }
      }

    } finally {
      c.unlock();
    }
    for (ObjectReferenceAddListener listener : this.objectRefsAddListener) {
      listener.objectReferencesAdded(oids);
    }

    return newReferences;
  }

  @Override
  public void shutdownNode(final NodeID waitee) {
    this.clientStates.remove(waitee);
  }

  @Override
  public boolean startupNode(final NodeID nodeID) {
    return (this.clientStates.putIfAbsent(nodeID, new ClientStateImpl(nodeID)) == null);
  }

  private ClientStateImpl getClientState(final NodeID id) {
    return this.clientStates.get(id);
  }

  @Override
  public int getReferenceCount(final NodeID nodeID) {
    final ClientStateImpl c = getClientState(nodeID);
    if (c == null) { return 0; }
    c.lock();
    try {
      return c.getReferences().size();
    } finally {
      c.unlock();
    }
  }

  @Override
  public Set<NodeID> getConnectedClientIDs() {
    return Collections.unmodifiableSet(this.clientStates.keySet());
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    final PrettyPrinter rv = out;
    out.print(getClass().getName()).flush();
    out = out.duplicateAndIndent();
    out.indent().print("client states: ").flush();
    out = out.duplicateAndIndent();
    for (final ClientStateImpl c : this.clientStates.values()) {
      c.lock();
      try {
        out.indent().print(c.getNodeID() + "=").visit(c).flush();
      } finally {
        c.unlock();
      }
    }
    return rv;
  }

  private static class ClientStateImpl implements PrettyPrintable, ClientState {
    private final NodeID        nodeID;
    private final Set<ObjectID> managed = new BitSetObjectIDSet();
    private final ReentrantLock lock    = new ReentrantLock();

    public ClientStateImpl(final NodeID nodeID) {
      this.nodeID = nodeID;
    }

    public void lock() {
      this.lock.lock();
    }

    public void unlock() {
      this.lock.unlock();
    }

    public void removeReferencedObjectIDsFrom(final Set<ObjectID> lookupObjectIDs) {
      lookupObjectIDs.removeAll(this.managed);
    }

    public void addReferencedChildrenTo(final Set objectIDs, final ApplyTransactionInfo applyInfo) {
      final Set parents = applyInfo.getAllParents();
      parents.retainAll(this.managed);
      applyInfo.addReferencedChildrenTo(objectIDs, parents);
    }

    @Override
    public String toString() {
      return "ClientStateImpl[" + this.nodeID + ", " + this.managed + "]";
    }

    @Override
    public Set<ObjectID> getReferences() {
      return this.managed;
    }

    @Override
    public PrettyPrinter prettyPrint(final PrettyPrinter out) {
      out.print(getClass().getName()).flush();
      out.duplicateAndIndent().indent().print("managed: ").visit(this.managed);
      return out;
    }

    @Override
    public boolean addReference(final ObjectID id) {
      return this.managed.add(id);
    }

    @Override
    public boolean containsReference(final ObjectID id) {
      return this.managed.contains(id);
    }

    @Override
    public void removeReferences(final Set<ObjectID> references) {
      this.managed.removeAll(references);
    }

    @Override
    public void addReferencedIdsTo(final Set<ObjectID> ids) {
      ids.addAll(this.managed);
    }

    @Override
    public NodeID getNodeID() {
      return this.nodeID;
    }
  }

  // testing
  public ObjectReferenceAddListener[] getObjectReferenceAddRegisteredListeners() {
    return this.objectRefsAddListener.toArray(new ObjectReferenceAddListener[] {});
  }
}
