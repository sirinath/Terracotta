/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import EDU.oswego.cs.dl.util.concurrent.Latch;

import com.tc.exception.TCRuntimeException;
import com.tc.object.locks.LockID;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class LockAccounting {

  private final CopyOnWriteArrayList<TxnRemovedListener> listeners = new CopyOnWriteArrayList<TxnRemovedListener>();
  private final Map<TransactionID, Collection<LockID>>   tx2Locks  = new HashMap<TransactionID, Collection<LockID>>();
  private final Map<LockID, Collection<TransactionID>>   lock2Txs  = new HashMap<LockID, Collection<TransactionID>>();

  public synchronized Object dump() {
    return "LockAccounting:\ntx2Locks=" + tx2Locks + "\nlock2Txs=" + lock2Txs + "/LockAccounting";
  }

  public String toString() {
    return "LockAccounting[tx2Locks=" + tx2Locks + ", lock2Txs=" + lock2Txs + "]";
  }

  public synchronized void add(TransactionID txID, Collection lockIDs) {
    getOrCreateSetFor(txID, tx2Locks).addAll(lockIDs);
    for (Iterator i = lockIDs.iterator(); i.hasNext();) {
      LockID lid = (LockID) i.next();
      getOrCreateSetFor(lid, lock2Txs).add(txID);
    }
  }

  public synchronized Collection getTransactionsFor(LockID lockID) {
    Collection rv = new HashSet();
    Collection toAdd = lock2Txs.get(lockID);
    if (toAdd != null) {
      rv.addAll(toAdd);
    }
    return rv;
  }

  // This method returns a set of lockIds that has no more transactions to wait for
  public synchronized Set acknowledge(TransactionID txID) {
    Set completedLockIDs = null;
    Set lockIDs = getSetFor(txID, tx2Locks);
    if (lockIDs != null) {
      // this may be null if there are phantom acknowledgments caused by server restart.
      for (Iterator i = lockIDs.iterator(); i.hasNext();) {
        LockID lid = (LockID) i.next();
        Set txIDs = getOrCreateSetFor(lid, lock2Txs);
        if (!txIDs.remove(txID)) throw new AssertionError("No lock=>transaction found for " + lid + ", " + txID);
        if (txIDs.isEmpty()) {
          lock2Txs.remove(lid);
          if (completedLockIDs == null) {
            completedLockIDs = new HashSet();
          }
          completedLockIDs.add(lid);
        }
      }
    }
    removeTxn(txID);
    return (completedLockIDs == null ? Collections.EMPTY_SET : completedLockIDs);
  }

  public synchronized boolean isEmpty() {
    return tx2Locks.isEmpty() && lock2Txs.isEmpty();
  }

  private static Set getSetFor(Object key, Map m) {
    return (Set) m.get(key);
  }

  private static Set getOrCreateSetFor(Object key, Map m) {
    Set rv = getSetFor(key, m);
    if (rv == null) {
      rv = new HashSet();
      m.put(key, rv);
    }
    return rv;
  }

  private void removeTxn(TransactionID txnID) {
    tx2Locks.remove(txnID);
    notifyTxnRemoved(txnID);
  }

  private void notifyTxnRemoved(TransactionID txnID) {
    for (TxnRemovedListener l : listeners) {
      l.txnRemoved(txnID);
    }
  }

  public void waitAllCurrentTxnCompleted() {
    TxnRemovedListener listener;
    Latch latch = new Latch();
    synchronized (this) {
      Set currentTxnSet = new HashSet(tx2Locks.keySet());
      listener = new TxnRemovedListener(currentTxnSet, latch);
      listeners.add(listener);
    }
    try {
      latch.acquire();
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    } finally {
      listeners.remove(listener);
    }
  }

  private static class TxnRemovedListener {
    private final Set<TransactionID> txnSet;
    private final Latch              latch;

    TxnRemovedListener(Set<TransactionID> txnSet, Latch latch) {
      this.txnSet = txnSet;
      this.latch = latch;
      if (txnSet.size() == 0) allTxnCompleted();
    }

    void txnRemoved(TransactionID txnID) {
      this.txnSet.remove(txnID);
      if (txnSet.size() == 0) allTxnCompleted();
    }

    void allTxnCompleted() {
      this.latch.release();
    }

  }

}
