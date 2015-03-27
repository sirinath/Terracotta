/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.tx;

import com.tc.net.NodeID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;

import java.util.Set;

/**
 * This class keeps track of the various actions that the server transaction went through.
 * 
 * @author Saravanan Subbiah
 * @author Nabib El-Rahman
 */
public interface TransactionAccount {

  /**
   * Return the nodeID for associated with this {@link TransactionAccount} This can be both a ClientID or a ServerID
   * 
   * @return NodeID nodeID
   */
  public NodeID getNodeID();

  /**
   * Remove waitee associated with @{link TransactionID}
   * 
   * @param waitee Node still to give acknowledgment.
   * @param requestID TransactionID ID of transaction.
   * @returns boolean true if completed, false if not completed or if the client has sent a duplicate ACK.
   */
  public boolean removeWaitee(NodeID waitee, TransactionID txnID);

  /**
   * Add waitee associated with @{link TransactionID}
   * 
   * @param waitee Node still to give acknowledgment.
   * @param requestID TransactionID ID of transaction.
   */
  public void addWaitee(NodeID waitee, TransactionID requestID);

  /**
   * Acknowledge @{link TransactionID} has been committed but not applied. This usually indicates a transaction already
   * has being applied.
   * 
   * @param requestID TransactionID ID of transaction.
   * @return boolean true if completed, false if not completed.
   */
  public boolean skipApplyAndCommit(TransactionID requestID);

  /**
   * Acknowledge @{link TransactionID} has been committed.
   * 
   * @param requestID TransactionID ID of transaction.
   * @return boolean true if completed, false if not completed.
   */
  public boolean applyCommitted(TransactionID requestID);

  /**
   * Acknowledge @{link TransactionID} has been broadcasted.
   * 
   * @param requestID TransactionID ID of transaction.
   * @return boolean true if completed, false if not completed
   */
  public boolean broadcastCompleted(TransactionID requestID);

  /**
   * Acknowledge @{link TransactionID} has processed metadata.
   * 
   * @param requestID TransactionID id of transaction.
   * @return boolean true if completed, false if not completed.
   */
  public boolean processMetaDataCompleted(TransactionID requestID);

  /**
   * Indicates whether @{link TransactionID} has waitees.
   * 
   * @param requestID TransactionID id of transaction.
   * @return true if has waitees, false if no waitees.
   */
  public boolean hasWaitees(TransactionID requestID);

  /**
   * Return set of @{link TransactionID} that is waiting for an acknowledgement from waitee.
   * 
   * @param NodeID waitee nodeID for all the transaction waiting on it.
   * @return Set<TransactionID> returns set of transaction waiting for waitee.
   */
  public Set<TransactionID> requestersWaitingFor(NodeID waitee);

  /**
   * Acknowledge that transaction has been relayed. This is usually used to send transactions to be applied on the
   * passive.
   * 
   * @param requestID TransactionID ID of transaction.
   * @return boolean true if completed, false if not completed.
   */
  public boolean relayTransactionComplete(TransactionID txnID);

  /**
   * Acknowledge arrival of incoming transactions to the server.
   * 
   * @param Set<ServerTransactionID> serverTransactionIDs server transactions.
   */
  public void incomingTransactions(Set<ServerTransactionID> serverTransactionIDs);

  /**
   * Add all pending @{link ServerTransactionID} to set.
   * 
   * @param Set<ServerTransactionID> txnsInSystem set to add ServerTransactionIDs to.
   */
  public void addAllPendingServerTransactionIDsTo(Set<ServerTransactionID> txnsInSystem);

  /**
   * Notify TransactionAccount that node is dead. invoke callback if no pending transactions.
   * 
   * @param CallBackOnComplete callBack, callBack on completion.
   */
  public void nodeDead(CallBackOnComplete callBack);

  /**
   * 
   */
  public void addObjectsSyncedTo(NodeID to, TransactionID txnID);

  /**
   * Wait until a transaction is relayed to all passives
   */
  void waitUntilRelayed(TransactionID txnID);

  /**
   * Waits until the given transaction is committed to disk
   *
   * @param txnID id
   */
  void waitUntilCommitted(TransactionID txnID);

  /**
   * Call back interface.
   */
  public interface CallBackOnComplete {

    /**
     * Call back method
     * 
     * @param NodeID dead. Dead node.
     */
    public void onComplete(NodeID dead);
  }
}