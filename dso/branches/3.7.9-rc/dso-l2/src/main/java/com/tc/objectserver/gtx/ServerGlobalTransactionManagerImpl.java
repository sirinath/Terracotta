/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.gtx;

import com.tc.async.api.Sink;
import com.tc.net.NodeID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.persistence.api.TransactionStore;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.context.LowWaterMarkCallbackContext;
import com.tc.util.SequenceValidator;
import com.tc.util.sequence.Sequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class ServerGlobalTransactionManagerImpl implements ServerGlobalTransactionManager {

  private final TransactionStore                    transactionStore;
  private final PersistenceTransactionProvider      persistenceTransactionProvider;
  private final SequenceValidator                   sequenceValidator;
  private final GlobalTransactionIDSequenceProvider gidSequenceProvider;
  private final Sequence                            globalTransactionIDSequence;
  private final SortedMap<GlobalTransactionID, List<Runnable>> lwmCallbacks = new TreeMap<GlobalTransactionID, List<Runnable>>();
  private final Sink                                callbackSink;

  public ServerGlobalTransactionManagerImpl(SequenceValidator sequenceValidator, TransactionStore transactionStore,
                                            PersistenceTransactionProvider ptxp,
                                            GlobalTransactionIDSequenceProvider gidSequenceProvider,
                                            Sequence globalTransactionIDSequence, Sink callbackSink) {
    this.sequenceValidator = sequenceValidator;
    this.transactionStore = transactionStore;
    this.persistenceTransactionProvider = ptxp;
    this.gidSequenceProvider = gidSequenceProvider;
    this.globalTransactionIDSequence = globalTransactionIDSequence;
    this.callbackSink = callbackSink;
  }

  public void shutdownNode(NodeID nodeID) {
    this.sequenceValidator.remove(nodeID);
    PersistenceTransaction tx = this.persistenceTransactionProvider.newTransaction();
    transactionStore.shutdownNode(tx, nodeID);
    tx.commit();
    processCallbacks();
  }

  public void shutdownAllClientsExcept(Set cids) {
    PersistenceTransaction tx = this.persistenceTransactionProvider.newTransaction();
    transactionStore.shutdownAllClientsExcept(tx, cids);
    tx.commit();
    processCallbacks();
  }

  public boolean initiateApply(ServerTransactionID stxID) {
    GlobalTransactionDescriptor gtx = this.transactionStore.getTransactionDescriptor(stxID);
    return gtx.initiateApply();
  }

  public void clearCommitedTransactionsBelowLowWaterMark(ServerTransactionID sid) {
    PersistenceTransaction tx = this.persistenceTransactionProvider.newTransaction();
    transactionStore.clearCommitedTransactionsBelowLowWaterMark(tx, sid);
    tx.commit();
    processCallbacks();
  }

  public void clearCommitedTransactionsBelowLowWaterMark(GlobalTransactionID lowGlobalTransactionIDWatermark) {
    PersistenceTransaction tx = this.persistenceTransactionProvider.newTransaction();
    transactionStore.clearCommitedTransactionsBelowLowWaterMark(tx, lowGlobalTransactionIDWatermark);
    tx.commit();
  }

  public void commit(PersistenceTransaction persistenceTransaction, ServerTransactionID stxID) {
    transactionStore.commitTransactionDescriptor(persistenceTransaction, stxID);
  }

  public void commitAll(PersistenceTransaction persistenceTransaction, Collection stxIDs) {
    transactionStore.commitAllTransactionDescriptor(persistenceTransaction, stxIDs);
  }

  public GlobalTransactionID getLowGlobalTransactionIDWatermark() {
    return transactionStore.getLeastGlobalTransactionID();
  }

  public GlobalTransactionID getOrCreateGlobalTransactionID(ServerTransactionID serverTransactionID) {
    GlobalTransactionDescriptor gdesc = transactionStore.getOrCreateTransactionDescriptor(serverTransactionID);
    return gdesc.getGlobalTransactionID();
  }

  public GlobalTransactionID getGlobalTransactionID(ServerTransactionID serverTransactionID) {
    GlobalTransactionDescriptor gdesc = transactionStore.getTransactionDescriptor(serverTransactionID);
    return (gdesc != null ? gdesc.getGlobalTransactionID() : GlobalTransactionID.NULL_ID);

  }

  public void createGlobalTransactionDescIfNeeded(ServerTransactionID stxnID, GlobalTransactionID globalTransactionID) {
    transactionStore.createGlobalTransactionDescIfNeeded(stxnID, globalTransactionID);
  }

  public GlobalTransactionIDSequenceProvider getGlobalTransactionIDSequenceProvider() {
    return gidSequenceProvider;
  }

  public Sequence getGlobalTransactionIDSequence() {
    return globalTransactionIDSequence;
  }

  private void processCallbacks() {
    GlobalTransactionID gid = getLowGlobalTransactionIDWatermark();
    List<Runnable> callbacks = new ArrayList<Runnable>();
    synchronized (lwmCallbacks) {
      if (lwmCallbacks.isEmpty()) { return; }

      Iterator<List<Runnable>> i;
      if (gid.isNull()) {
        // No global transactions left, just clear out all the callbacks right away
        i = lwmCallbacks.values().iterator();
      } else {
        // clear out only the callbacks < gid
        i = lwmCallbacks.headMap(gid).values().iterator();
      }
      while (i.hasNext()) {
        callbacks.addAll(i.next());
        i.remove();
      }
    }
    // We can allow the callbacks to finish asynchronously here since from this point on they no longer have anything to
    // do with the current state of the global transaction system.
    for (Runnable r : callbacks) {
      callbackSink.add(new LowWaterMarkCallbackContext(r));
    }
  }

  @Override
  public void registerCallbackOnLowWaterMarkReached(final Runnable callback) {
    if (getLowGlobalTransactionIDWatermark().isNull()) {
      // Just execute the callback right away if there are no live transactions in the system.
      callbackSink.add(new LowWaterMarkCallbackContext(callback));
      return;
    }
    GlobalTransactionID gid = new GlobalTransactionID(getGlobalTransactionIDSequence().current());
    synchronized (lwmCallbacks) {
      if (!lwmCallbacks.containsKey(gid)) {
        lwmCallbacks.put(gid, new ArrayList<Runnable>());
      }
      lwmCallbacks.get(gid).add(callback);
    }
  }
}
