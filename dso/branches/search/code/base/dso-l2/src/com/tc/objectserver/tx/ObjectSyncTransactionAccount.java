/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.NodeID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Maintains accounting for object syncing to the passive server.
 * Primarily waitee accounting is maintained.
 * 
 * @author Saravanan Subbiah
 * @author Nabib El-Rahman
 */
public class ObjectSyncTransactionAccount implements TransactionAccount {

  private final NodeID                  sourceID;
  private final Map<TransactionID, Set> txn2Waitees = new HashMap();

  public ObjectSyncTransactionAccount(NodeID sourceID) {
    this.sourceID = sourceID;
  }

  /**
   * {@inheritDoc}
   */
  public NodeID getNodeID() {
    return sourceID;
  }

  /**
   * {@inheritDoc}
   */
  public synchronized void addAllPendingServerTransactionIDsTo(Set<ServerTransactionID> txnIDs) {
    for (Iterator<TransactionID> i = txn2Waitees.keySet().iterator(); i.hasNext();) {
      TransactionID txnID = i.next();
      txnIDs.add(new ServerTransactionID(sourceID, txnID));
    }
  }

  /**
   * {@inheritDoc}
   */
  public synchronized void addWaitee(NodeID waitee, TransactionID requestID) {
   // Even though the current logic is we only send 1 txn to 1 node during object sync,
   //it is managed in a set for safe measures.
    Assert.assertEquals(NodeID.SERVER_NODE_TYPE, waitee.getNodeType());
    Set waitees = getOrCreate(requestID);
    waitees.add(waitee);
  }
  
  /**
   * {@inheritDoc}
   */
  public synchronized boolean removeWaitee(NodeID waitee, TransactionID requestID) {
    Assert.assertEquals(NodeID.SERVER_NODE_TYPE, waitee.getNodeType());
    Set waiteesSet = txn2Waitees.get(requestID);
    if (waiteesSet == null) { return true; }
    waiteesSet.remove(waitee);
    if (waiteesSet.isEmpty()) {
      txn2Waitees.remove(requestID);
      return true;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public Set requestersWaitingFor(NodeID nodeID) {
    if (nodeID.getNodeType() == NodeID.CLIENT_NODE_TYPE) { return Collections.EMPTY_SET; }
    synchronized (this) {
      Set requesters = new HashSet();
      for (Iterator<Entry<TransactionID, Set>> i = txn2Waitees.entrySet().iterator(); i.hasNext();) {
        Entry<TransactionID, Set> e = i.next();
        Set waiteesSet = e.getValue();
        if (waiteesSet.contains(nodeID)) {
          TransactionID requester = e.getKey();
          requesters.add(requester);
        }
      }
      return requesters;
    }
  }

  /**
   * @param TransactionID requestID
   * 
   * @throws {@link UnsupportedOperationException} always
   */
  public boolean applyCommitted(TransactionID requestID) {
    throw new UnsupportedOperationException();
  }

  /**
   * @param TransactionID requestID
   * 
   * @throws {@link UnsupportedOperationException} always
   */
  public boolean broadcastCompleted(TransactionID requestID) {
    throw new UnsupportedOperationException();
  }

  /**
   * @param TransactionID requestID
   * 
   * @throws {@link UnsupportedOperationException} always
   */
  public synchronized boolean hasWaitees(TransactionID requestID) {
    return txn2Waitees.containsKey(requestID);
  }

  /**
   * @param Set<ServerTransactionID> serverTransactionIDs
   * 
   * @throws {@link UnsupportedOperationException} always
   */
  public void incomingTransactions(Set<ServerTransactionID> serverTransactionIDs) {
    throw new UnsupportedOperationException();
  }

  /**
   * @param CallBackOnComplete callBack
   * 
   * @throws {@link UnsupportedOperationException} always
   */
  public void nodeDead(CallBackOnComplete callBack) {
    // This should never be called as record is only created for local node
    throw new UnsupportedOperationException();
  }

  /**
   * @param TransactionID requestID
   * 
   * @throws {@link UnsupportedOperationException} always
   */
  public boolean relayTransactionComplete(TransactionID requestID) {
    throw new UnsupportedOperationException();
  }

  /**
   * @param TransactionID requestID
   * 
   * @throws {@link UnsupportedOperationException} always
   */
  public boolean skipApplyAndCommit(TransactionID requestID) {
    throw new UnsupportedOperationException();
  }
  
  /**
   * returns false always since MetaData completion does not
   * factor into object sync transaction accounting.
   * 
   * @param TransactionID requestID
   * 
   * @return boolean
   */
  public boolean processMetaDataCompleted(TransactionID requestID) {
    return false;
  }

  @Override
  public String toString() {
    StringBuilder strBuffer = new StringBuilder();
    strBuffer.append("ObjectSynchTransactionAccount [ " + sourceID + " ] : { txn2Waitees  : ");
    synchronized (this) {
      for (Iterator<Entry<TransactionID, Set>> iter = this.txn2Waitees.entrySet().iterator(); iter.hasNext();) {
        Entry entry = iter.next();
        strBuffer.append(entry.getKey()).append(": ").append(entry.getValue());
      }
    }
    strBuffer.append(" }\n");
    return strBuffer.toString();
  }

  private Set getOrCreate(TransactionID requestID) {
    Set waitees =  txn2Waitees.get(requestID);
    if (waitees == null) {
      txn2Waitees.put(requestID, (waitees = new HashSet(2)));
    }
    return waitees;
  }

}
