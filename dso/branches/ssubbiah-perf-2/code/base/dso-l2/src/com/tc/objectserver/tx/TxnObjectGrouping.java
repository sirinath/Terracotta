/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.object.tx.ServerTransactionID;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class TxnObjectGrouping implements PrettyPrintable {
  private static final int          MAX_OBJECTS_TO_COMMIT = TCPropertiesImpl.getProperties()
                                                              .getInt("l2.objectmanager.maxObjectsToCommit");
  private static final int          MAX_TXNS_TO_COMMIT    = TCPropertiesImpl.getProperties()
                                                              .getInt("l2.objectmanager.maxTxnsToCommit");

  private final ServerTransactionID txID;
  private Set                       txns;
  private Set                       pendingApplys;
  private Map                       objects;
  private final Map                 newRootsMap;

  public TxnObjectGrouping(ServerTransactionID sTxID, Map newRootsMap) {
    this.txID = sTxID;
    this.newRootsMap = newRootsMap;
    this.txns = new HashSet();
    this.pendingApplys = new HashSet();
    this.txns.add(sTxID);
    this.pendingApplys.add(sTxID);
    this.objects = Collections.EMPTY_MAP;
  }

  public TxnObjectGrouping(Map lookedupObjects) {
    this.txID = ServerTransactionID.NULL_ID;
    this.newRootsMap = Collections.EMPTY_MAP;
    this.txns = Collections.EMPTY_SET;
    this.pendingApplys = Collections.EMPTY_SET;
    objects = lookedupObjects;
  }

  public boolean applyComplete(ServerTransactionID txnId) {
    Assert.assertTrue(pendingApplys.remove(txnId));
    return pendingApplys.isEmpty();
  }

  public ServerTransactionID getServerTransactionID() {
    return txID;
  }

  public Map getObjects() {
    return objects;
  }

  public Map getNewRoots() {
    return newRootsMap;
  }

  /*
   * This method has a side effect of setting references in Old grouping to null. This is done to avoid adding huge
   * collections to smaller collections for performance reasons.
   */
  public void merge(TxnObjectGrouping oldGrouping) {
    Assert.assertTrue(this.txID != ServerTransactionID.NULL_ID);
    if (txns.size() >= oldGrouping.txns.size()) {
      txns.addAll(oldGrouping.txns);
    } else {
      Set temp = txns;
      txns = oldGrouping.txns;
      txns.addAll(temp);
    }
    if (objects.size() >= oldGrouping.objects.size()) {
      objects.putAll(oldGrouping.objects);
    } else {
      Map temp = objects;
      objects = oldGrouping.objects;
      objects.putAll(temp);
    }
    if (pendingApplys.size() >= oldGrouping.pendingApplys.size()) {
      pendingApplys.addAll(oldGrouping.pendingApplys);
    } else {
      Set temp = pendingApplys;
      pendingApplys = oldGrouping.pendingApplys;
      pendingApplys.addAll(temp);
    }
    newRootsMap.putAll(oldGrouping.newRootsMap);

    // Setting reference to null so that we disable access to these through old grouping
    oldGrouping.txns = null;
    oldGrouping.objects = null;
    oldGrouping.pendingApplys = null;
  }

  public boolean limitReached() {
    return txns.size() > MAX_TXNS_TO_COMMIT || objects.size() > MAX_OBJECTS_TO_COMMIT;
  }

  public int hashCode() {
    return txID.hashCode();
  }

  public boolean equals(Object o) {
    if (o instanceof TxnObjectGrouping) {
      TxnObjectGrouping other = (TxnObjectGrouping) o;
      return (txID.equals(other.txID));
    }
    return false;
  }

  public Collection getApplyPendingTxns() {
    return pendingApplys;
  }

  public Collection getTxnIDs() {
    return txns;
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.println("TransactionGrouping@" + System.identityHashCode(this));
    out.indent().println("txnID: ").visit(txID).println();
    out.indent().println("txns: ").visit(txns).println();
    out.indent().println("objects: ").visit(objects.keySet()).println();
    out.indent().println("pendingApplys: ").visit(pendingApplys).println();
    out.indent().println("newRootsMap: ").visit(newRootsMap).println();
    return out;
  }

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
    return "TxnGrouping[txns = " + txns.size() + ", objects = " + objects.size() + ", pendingApplys = "
           + pendingApplys.size() + ", roots = " + newRootsMap.size() + "]";
  }

}