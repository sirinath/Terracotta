/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface ServerTransactionManager {

  /**
   * called when a Node (Client or Server) leaves.
   */
  public void shutdownNode(NodeID nodeID);

  /**
   * called with a Node is connected;
   */
  public void nodeConnected(NodeID nodeID);

  /**
   * Add "waiter/requestID" is waiting for clientID "waitee" to respond to my message send
   * 
   * @param waiter - ChannelID of the sender of the message that is waiting for a response
   * @param requestID - The id of the request sent by the channel ID that is waiting for a response
   * @param waitee - the channelID that waiter is waiting for a response from
   */
  public void addWaitingForAcknowledgement(NodeID waiter, TransactionID requestID, NodeID waitee);

  /**
   * Is the waiter done waiting or does it need to continue waiting?
   * 
   * @param waiter - ChannelID of the sender of the message that is waiting for a response
   * @param requestID - The id of the request sent by the channel ID that is waiting for a response
   * @return
   */
  public boolean isWaiting(NodeID waiter, TransactionID requestID);

  /**
   * received an acknowledgment from the client that the changes in the given transaction have been applied. This could
   * potentially trigger an acknowledgment to the originating client.
   * 
   * @param waiter - NodeID of the sender of the message that is waiting for a response
   * @param waitee - the channelID that waiter is waiting for a response from
   */
  public void acknowledgement(NodeID waiter, TransactionID requestID, NodeID waitee);

  /**
   * Apply the changes in the given transaction to the given set of checked out objects.
   * 
   * @param instanceMonitor
   */
  public void apply(ServerTransaction txn, Map objects, ApplyTransactionInfo includeIDs,
                    ObjectInstanceMonitor instanceMonitor);

  public void cleanup(Set<ObjectID> deletedObjects);
  /**
   * Commits all the changes in objects and releases the objects This could potentially trigger an acknowledgment to the
   * originating client.
   */
  public void commit(Collection<ManagedObject> objects,
                     Map<String, ObjectID> newRoots, Collection<ServerTransactionID> appliedServerTransactionIDs);

  /**
   * The broadcast stage is completed. This could potentially trigger an acknowledgment to the originating client.
   */
  public void broadcasted(NodeID waiter, TransactionID requestID);

  public void processingMetaDataCompleted(NodeID sourceID, TransactionID txnID);

  /**
   * Notifies the transaction managed that the given transaction is being skipped
   */
  public void skipApplyAndCommit(ServerTransaction txn);

  public void addTransactionListener(ServerTransactionListener listener);

  public void removeTransactionListener(ServerTransactionListener listener);

  /**
   * Returns when all transactions which were added to the transaction accounting system
   * after the listener was attached have finished processing.
   * @param l
   */
  public void callBackOnTxnsInSystemCompletion(TxnsInSystemCompletionListener l);

  public void callBackOnResentTxnsInSystemCompletion(TxnsInSystemCompletionListener l);

  public void incomingTransactions(NodeID nodeID, Map<ServerTransactionID, ServerTransaction> txns);

  public void transactionsRelayed(NodeID node, Set serverTxnIDs);

  public void objectsSynched(NodeID node, ServerTransactionID tid);

  public void setResentTransactionIDs(NodeID source, Collection transactionIDs);

  public void start(Set cids);

  public void goToActiveMode();


  /**Returns the number of transactions which have been added to the transaction accounting system
   * and are in various stages of being applied. This is somewhat similar to the number of transactions
   * "flowing" through the system.
   *
   * @return an int equal to the number of transactions which are in various stages of being applied.
   */
  public int getTotalPendingTransactionsCount();

  /**
   * Returns the number of transactions the server has processed since it started.
   * For passive servers this is always zero. Each active server keeps a track of
   * the total number of transactions it has processed since it was started using an
   * internal non-decreasing counter. This method returns the current value of that counter.
   * @return a long equal to the number transactions processed by the server since it started
   */
  public long getTotalNumOfActiveTransactions();

  public void processMetaData(ServerTransaction txn, ApplyTransactionInfo applyInfo);

  /**
   * Set the given callback to be executed when the Low Water Mark passes the current GID. (i.e. all transactions
   * currently in the system will no longer be resent).
   * 
   * @param r Callback to be executed
   */
  public void callbackOnLowWaterMarkInSystemCompletion(Runnable r);

  /**
   * Pauses addition of new transactions to the server. New transactions sent by the clients after this
   * method's call will not be added to the transaction accounting system and will get queued up.
   * Note that this method does not halt the processing for transactions which have already been added
   * to the transaction accounting system.
   */
  public void pauseTransactions();

  /**
   * Allows transactions to be added to the accounting system, reversing the effect of pauseTransactions() method.
   */
  public void unPauseTransactions();

  public void loadApplyChangeResults(ServerTransaction txn, ApplyTransactionInfo applyInfo);

  void waitForTransactionRelay(ServerTransactionID serverTransactionID);

  void waitForTransactionCommit(ServerTransactionID serverTransactionID);
}
