/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.async.api.Sink;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.context.ApplyTransactionContext;
import com.tc.objectserver.context.CommitTransactionContext;
import com.tc.objectserver.context.LookupEventContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class keeps track of locally checked out objects for applys and maintain the objects to txnid mapping in the
 * server. It wraps calls going to object manager from lookup, apply, commit stages
 */
public class TransactionObjectManagerImpl implements TransactionObjectManager, PrettyPrintable {

  // TODO:: Move to TCProperties
  private static final int                     MAX_OBJECTS_TO_COMMIT = 500;

  private final ObjectManager                  objectManager;
  private final TransactionSequencer           sequencer;
  private final ServerGlobalTransactionManager gtxm;
  private final Sink                           lookupSink;

  private final Object                         completedTxnIdsLock   = new Object();
  private Set                                  completedTxnIDs       = new HashSet();

  /*
   * This map contains ObjectIDs to TxnObjectGrouping that contains these objects
   */
  private final Map                            checkedOutObjects     = new HashMap();
  private final Map                            applyPendingTxns      = new HashMap();
  private final LinkedHashMap                  commitPendingTxns     = new LinkedHashMap();

  private final Set                            pendingObjectRequest  = new HashSet();
  private LinkedList                           pendingTxnList        = new LinkedList();

  public TransactionObjectManagerImpl(ObjectManager objectManager, TransactionSequencer sequencer,
                                      ServerGlobalTransactionManager gtxm, Sink lookupSink) {
    this.objectManager = objectManager;
    this.sequencer = sequencer;
    this.gtxm = gtxm;
    this.lookupSink = lookupSink;
    // Thread t = new Thread("SAro Dumper") {
    // public void run() {
    // ThreadUtil.reallySleep(60000);
    // dump();
    // }
    // };
    // t.start();
  }

  // ProcessTransactionHandler Method
  public void addTransactions(ChannelID channelID, List txns, Collection completedTxnIds) {
    sequencer.addTransactions(txns);
    addCompletedTxnIds(completedTxnIds);
  }

  private void addCompletedTxnIds(Collection txnIds) {
    synchronized (completedTxnIdsLock) {
      completedTxnIDs.addAll(txnIds);
    }
  }

  private Set getCompletedTxnIds() {
    synchronized (completedTxnIdsLock) {
      Set toRet = completedTxnIDs;
      completedTxnIDs = new HashSet();
      return toRet;
    }
  }

  // BatchedLookupHandler Method
  public void lookupObjectsForTransactions(Sink applyChangesSink) {
    ServerTransaction txn;
    while ((txn = sequencer.getNextTxnToProcess()) != null) {
      ServerTransactionID stxID = txn.getServerTransactionID();
      if (gtxm.needsApply(stxID)) {
        lookupObjectsForApplyAndAddToSink(new TxnLookupContext(txn, applyChangesSink));
      } else {
        // These txns are already applied, hence just sending it to the next stage.
        applyChangesSink.add(new ApplyTransactionContext(txn, Collections.EMPTY_MAP));
      }
    }
  }

  public synchronized void lookupObjectsForApplyAndAddToSink(TxnLookupContext lookupContext) {
    ServerTransaction txn = lookupContext.getTransaction();
    Collection oids = txn.getObjectIDs();
    Set newRequests = new HashSet();
    boolean makePending = false;
    for (Iterator i = oids.iterator(); i.hasNext();) {
      ObjectID oid = (ObjectID) i.next();
      if (checkedOutObjects.containsKey(oid)) {
        // Object is already checked out
      } else if (pendingObjectRequest.contains(oid)) {
        makePending = true;
      } else {
        newRequests.add(oid);
      }
    }
    // TODO:: make cache and stats right
    if (!newRequests.isEmpty()
        && !(objectManager.lookupObjectsForCreateIfNecessary(txn.getChannelID(), newRequests, lookupContext))) {
      // New request went pending in object manager
      makePending = true;
      pendingObjectRequest.addAll(newRequests);
    }
    if (makePending) {
      lookupContext.makePending(txn.getChannelID(), oids);
      pendingTxnList.add(lookupContext);
    } else {
      ServerTransactionID txnID = txn.getServerTransactionID();
      TxnObjectGrouping newGrouping = new TxnObjectGrouping(txnID, txn.getNewRoots());
      mergeTransactionGroupings(oids, newGrouping);
      applyPendingTxns.put(txnID, newGrouping);
      lookupContext.getSink().add(new ApplyTransactionContext(txn, newGrouping.getObjects()));
      makeUnpending(lookupContext);
    }
  }

  private void mergeTransactionGroupings(Collection oids, TxnObjectGrouping newGrouping) {
    for (Iterator i = oids.iterator(); i.hasNext();) {
      ObjectID oid = (ObjectID) i.next();
      TxnObjectGrouping oldGrouping = (TxnObjectGrouping) checkedOutObjects.get(oid);
      if (oldGrouping == null) {
        throw new AssertionError("Transaction Grouping for lookedup objects is Null !! " + oid);
      } else if (oldGrouping != newGrouping) {
        newGrouping.merge(oldGrouping);
        for (Iterator j = oldGrouping.getObjects().keySet().iterator(); j.hasNext();) {
          Object old = checkedOutObjects.put(j.next(), newGrouping);
          Assert.assertTrue(old == oldGrouping);
        }
        ServerTransactionID oldTxnId = oldGrouping.getServerTransactionID();
        if (commitPendingTxns.remove(oldTxnId) == null) {
          // This grouping is not in commitPending so it could be in apply pending
          for (Iterator j = oldGrouping.getApplyPendingTxns().iterator(); j.hasNext();) {
            oldTxnId = (ServerTransactionID) j.next();
            if (applyPendingTxns.containsKey(oldTxnId)) {
              applyPendingTxns.put(oldTxnId, newGrouping);
            }
          }
        }
      }
    }
  }

  private synchronized void addLookedupObjects(Map lookedupObjects) {
    TxnObjectGrouping tg = new TxnObjectGrouping(lookedupObjects);
    for (Iterator i = lookedupObjects.keySet().iterator(); i.hasNext();) {
      Object oid = i.next();
      pendingObjectRequest.remove(oid);
      checkedOutObjects.put(oid, tg);
    }
  }

  private void makePending(TxnLookupContext context) {
    sequencer.makePending(context.getTransaction());
  }

  private void makeUnpending(TxnLookupContext context) {
    if (context.isPendingRequest()) {
      sequencer.processedPendingTxn(context.getTransaction());
      lookupSink.add(new LookupEventContext()); // TODO:: Optimize unnecessary adds to the sink
    }
  }

  private synchronized void processPendingTransactions() {
    this.pendingTxnList = new LinkedList();
    for (Iterator i = pendingTxnList.iterator(); i.hasNext();) {
      TxnLookupContext tlc = (TxnLookupContext) i.next();
      lookupObjectsForApplyAndAddToSink(tlc);
    }
  }

  // ApplyTransaction stage method
  public synchronized boolean applyTransactionComplete(ServerTransactionID stxnID) {
    TxnObjectGrouping grouping = (TxnObjectGrouping) applyPendingTxns.remove(stxnID);
    Assert.assertNotNull(grouping);
    if (grouping.applyComplete(stxnID)) {
      // Since verifying against all txns is costly, only the prime one (the one that created this grouping) is verfied
      // against
      ServerTransactionID pTxnID = grouping.getServerTransactionID();
      Assert.assertNull(applyPendingTxns.get(pTxnID));
      Object old = commitPendingTxns.put(pTxnID, grouping);
      Assert.assertNull(old);
      return true;
    }
    return false;
  }

  // Commit Transaction stage method
  public synchronized void addTransactionsToCommit(CommitTransactionContext ctc) {
    int count = 0;
    for (Iterator i = commitPendingTxns.values().iterator(); i.hasNext();) {
      TxnObjectGrouping grouping = (TxnObjectGrouping) i.next();
      i.remove();
      Assert.assertTrue(grouping.getApplyPendingTxns().isEmpty());
      Map objects = grouping.getObjects();
      count += objects.size();
      ctc.addObjectsAndAppliedTxns(grouping.getTxnIDs(), objects.values(), grouping.getNewRoots());
      for (Iterator j = objects.keySet().iterator(); j.hasNext();) {
        Object old = checkedOutObjects.remove(j.next());
        Assert.assertTrue(old == grouping);
      }
      if (count >= MAX_OBJECTS_TO_COMMIT) {
        break;
      }
    }
    if (count > 0) {
      ctc.setCompletedTransactionIds(getCompletedTxnIds());
    }
  }

  public void dump() {
    PrintWriter pw = new PrintWriter(System.err);
    new PrettyPrinter(pw).visit(this);
    pw.flush();
  }

  public synchronized PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.println(getClass().getName());
    out.indent().print("checkedOutObjects: ").println(checkedOutObjects);
    out.indent().print("applyPendingTxns: ").visit(applyPendingTxns).println();
    out.indent().print("commitPendingTxns: ").visit(commitPendingTxns).println();
    out.indent().println("pendingTxnList: " + pendingTxnList);
    out.indent().print("pendingObjectRequest: ").visit(pendingObjectRequest).println();
    return out;
  }

  private class TxnLookupContext implements ObjectManagerResultsContext {

    private final ServerTransaction txn;
    private volatile boolean        pending = false;
    private final Sink              applyChangesSink;

    public TxnLookupContext(ServerTransaction txn, Sink applyChangesSink) {
      this.txn = txn;
      this.applyChangesSink = applyChangesSink;
    }

    public Sink getSink() {
      return applyChangesSink;
    }

    public ServerTransaction getTransaction() {
      return txn;
    }

    // TODO:: Remove this
    public Set getCheckedOutObjectIDs() {
      return Collections.EMPTY_SET;
    }

    public boolean isPendingRequest() {
      return pending;
    }

    // Make pending could be called more than once !
    public synchronized void makePending(ChannelID channelID, Collection ids) {
      if (!pending) {
        pending = true;
        TransactionObjectManagerImpl.this.makePending(this);
      }
    }

    public void setResults(ChannelID chID, Collection ids, ObjectManagerLookupResults results) {
      synchronized (TransactionObjectManagerImpl.this) {
        TransactionObjectManagerImpl.this.addLookedupObjects(results.getObjects());
        if (pending) {
          TransactionObjectManagerImpl.this.processPendingTransactions();
        }
      }
    }
  }
}
