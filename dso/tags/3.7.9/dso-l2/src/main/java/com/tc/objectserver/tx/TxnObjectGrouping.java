/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.object.ObjectID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;
import com.tc.util.State;
import com.tc.util.TCCollections;

import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.SortedSet;

public final class TxnObjectGrouping implements PrettyPrintable {
  private static final int                MAX_OBJECTS    = TCPropertiesImpl
                                                             .getProperties()
                                                             .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_MAXOBJECTS_INTXNOBJ_GROUPING);
  private static final int                MAX_TXNS       = TCPropertiesImpl
                                                             .getProperties()
                                                             .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_MAXTXNS_INTXNOBJECT_GROUPING);

  private static final State              APPLY_PENDING  = new State("APPLY_PENDING");
  private static final State              COMMIT_PENDING = new State("COMMIT_PENDING");

  private final ServerTransactionID       txID;
  private Map<ServerTransactionID, State> txns;
  private Map<ObjectID, ManagedObject>    objects;
  private Map<String, ObjectID>           newRootsMap;
  private SortedSet<ObjectID>             deletedObjects;
  private int                             pendingApplys;
  private boolean                         isActive       = true;

  public TxnObjectGrouping(ServerTransactionID sTxID, Map<String, ObjectID> newRootsMap) {
    this.txID = sTxID;
    if (newRootsMap.isEmpty()) {
      this.newRootsMap = Collections.EMPTY_MAP;
    } else {
      this.newRootsMap = new HashMap<String, ObjectID>(newRootsMap);
    }
    this.txns = new HashMap<ServerTransactionID, State>();
    this.txns.put(sTxID, APPLY_PENDING);
    this.pendingApplys = 1;
    this.objects = Collections.EMPTY_MAP;
    this.deletedObjects = new ObjectIDSet();
  }

  public TxnObjectGrouping(Map<ObjectID, ManagedObject> lookedupObjects) {
    this.txID = ServerTransactionID.NULL_ID;
    this.newRootsMap = Collections.EMPTY_MAP;
    this.txns = Collections.EMPTY_MAP;
    objects = lookedupObjects;
    this.pendingApplys = 0;
    this.deletedObjects = TCCollections.EMPTY_OBJECT_ID_SET;
  }

  public boolean applyComplete(ApplyTransactionInfo applyTxnInfo) {
    State s = txns.put(applyTxnInfo.getServerTransactionID(), COMMIT_PENDING);
    Assert.assertTrue(s == APPLY_PENDING);
    --pendingApplys;
    this.deletedObjects.addAll(applyTxnInfo.getObjectIDsToDelete());
    return (pendingApplys == 0);
  }

  public ServerTransactionID getServerTransactionID() {
    return txID;
  }

  public Map<ObjectID, ManagedObject> getObjects() {
    return objects;
  }

  public Map<String, ObjectID> getNewRoots() {
    return newRootsMap;
  }

  public SortedSet<ObjectID> getDeletedObjects() {
    return deletedObjects;
  }

  /*
   * This method has a side effect of setting references in Old grouping to null. This is done to avoid adding huge
   * collections to smaller collections for performance reasons.
   */
  public void merge(TxnObjectGrouping oldGrouping) {
    Assert.assertTrue(this.txID != ServerTransactionID.NULL_ID && oldGrouping.isActive());
    if (txns.size() >= oldGrouping.txns.size()) {
      txns.putAll(oldGrouping.txns);
    } else {
      Map<ServerTransactionID, State> temp = txns;
      txns = oldGrouping.txns;
      txns.putAll(temp);
    }
    if (objects.size() >= oldGrouping.objects.size()) {
      objects.putAll(oldGrouping.objects);
    } else {
      Map<ObjectID, ManagedObject> temp = objects;
      objects = oldGrouping.objects;
      objects.putAll(temp);
    }
    if (!oldGrouping.newRootsMap.isEmpty()) {
      if (newRootsMap == Collections.EMPTY_MAP) {
        newRootsMap = oldGrouping.newRootsMap;
      } else {
        newRootsMap.putAll(oldGrouping.newRootsMap);
      }
    }
    deletedObjects.addAll(oldGrouping.getDeletedObjects());
    pendingApplys += oldGrouping.pendingApplys;

    // Setting these references to null so that we disable any further access to these through old grouping
    oldGrouping.txns = null;
    oldGrouping.objects = null;
    oldGrouping.newRootsMap = null;
    oldGrouping.deletedObjects = null;
    oldGrouping.isActive = false;
  }

  public boolean isActive() {
    return isActive;
  }

  public boolean limitReached() {
    return txns.size() > MAX_TXNS || (txns.size() > 1 && objects.size() > MAX_OBJECTS);
  }

  @Override
  public int hashCode() {
    return txID.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof TxnObjectGrouping) {
      TxnObjectGrouping other = (TxnObjectGrouping) o;
      return (txID.equals(other.txID));
    }
    return false;
  }

  public Iterator<ServerTransactionID> getApplyPendingTxnsIterator() {
    return new ApplyPendingTxnsIterator();
  }

  public Collection<ServerTransactionID> getTxnIDs() {
    return txns.keySet();
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.println("TransactionGrouping@" + System.identityHashCode(this));
    out.indent().println("txnID: ").visit(txID).println();
    out.indent().println("txns: ").visit(txns).println();
    out.indent().println("objects: ").visit(objects.keySet()).println();
    out.indent().println("pendingApplys: " + pendingApplys).println();
    out.indent().println("newRootsMap: ").visit(newRootsMap).println();
    return out;
  }

  @Override
  public String toString() {
    StringBuffer out = new StringBuffer();
    out.append("TransactionGrouping@" + System.identityHashCode(this)).append("\n");
    out.append("\t").append("txnID: ").append(txID).append("\n");
    out.append("\t").append("txns: ").append(txns).append("\n");
    out.append("\t").append("objects: ").append(objects.keySet()).append("\n");
    out.append("\t").append("pendingApplys: ").append(pendingApplys).append("\n");
    out.append("\t").append("newRootsMap: ").append(newRootsMap).append("\n");
    return out.toString();
  }

  public String shortDescription() {
    return "TxnGrouping[txns = " + txns.size() + ", objects = " + objects.size() + ", pendingApplys = " + pendingApplys
           + ", roots = " + newRootsMap.size() + "]";
  }

  private final class ApplyPendingTxnsIterator implements Iterator<ServerTransactionID> {

    int                                         remaining = pendingApplys;
    int                                         save      = pendingApplys;
    Iterator<Entry<ServerTransactionID, State>> pointer   = txns.entrySet().iterator();

    public boolean hasNext() {
      return (remaining > 0);
    }

    public ServerTransactionID next() {
      if (remaining <= 0) { throw new NoSuchElementException(); }
      if (save != pendingApplys) { throw new ConcurrentModificationException(); }
      remaining--;
      while (pointer.hasNext()) {
        Entry<ServerTransactionID, State> e = pointer.next();
        if (e.getValue() == APPLY_PENDING) { return e.getKey(); }
      }
      throw new AssertionError("Shouldnt reach here");
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

  }
}