/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;

import com.tc.exception.TCRuntimeException;
import com.tc.properties.TCProperties;
import com.tc.util.SequenceGenerator;
import com.tc.util.SequenceID;

public class TransactionSequencer {

  private static final int              MAX_BYTE_SIZE_FOR_BATCH = TCProperties
                                                                    .getProperties()
                                                                    .getInt(
                                                                            "l1.transactionmanager.maxBatchSizeInKiloBytes") * 1024;
  private static final int              MAX_PENDING_BATCHES     = TCProperties.getProperties()
                                                                    .getInt("l1.transactionmanager.maxPendingBatches");

  private final SequenceGenerator       sequence                = new SequenceGenerator(1);
  private ClientTransactionBatch        currentBatch;
  private final TransactionBatchFactory batchFactory;
  private final BoundedLinkedQueue      pendingBatches          = new BoundedLinkedQueue(MAX_PENDING_BATCHES);

  public TransactionSequencer(TransactionBatchFactory batchFactory) {
    this.batchFactory = batchFactory;
    currentBatch = createNewBatch();
  }

  private ClientTransactionBatch createNewBatch() {
    return batchFactory.nextBatch();
  }

  private void addTransactionToBatch(ClientTransaction txn, ClientTransactionBatch batch) {
    batch.addTransaction(txn);
  }

  public synchronized void addTransaction(ClientTransaction txn) {
    SequenceID sequenceID = new SequenceID(sequence.getNextSequence());
    txn.setSequenceID(sequenceID);
    addTransactionToBatch(txn, currentBatch);
    if (currentBatch.byteSize() > MAX_BYTE_SIZE_FOR_BATCH) {
      put(currentBatch);
      currentBatch = createNewBatch();
    }
  }

  private void put(ClientTransactionBatch batch) {
    try {
      pendingBatches.put(batch);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  private ClientTransactionBatch get() {
    try {
      return (ClientTransactionBatch) pendingBatches.poll(0);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  private ClientTransactionBatch peek() {
    return (ClientTransactionBatch) pendingBatches.peek();
  }

  public ClientTransactionBatch getNextBatch() {
    ClientTransactionBatch batch = get();
    if (batch != null) return batch;
    synchronized (this) {
      // Check again to avoid sending the txn in the wrong order
      batch = get();
      if (batch != null) return batch;
      if (!currentBatch.isEmpty()) {
        batch = currentBatch;
        currentBatch = createNewBatch();
        return batch;
      }
      return null;
    }
  }

  /**
   * Used only for testing
   */
  public synchronized void clear() {
    while (get() != null) {
      // remove all pending
    }
    currentBatch = createNewBatch();
  }

  public SequenceID getNextSequenceID() {
    ClientTransactionBatch batch = peek();
    if (batch != null) return batch.getMinTransactionSequence();
    synchronized (this) {
      batch = peek();
      if (batch != null) return batch.getMinTransactionSequence();
      if (!currentBatch.isEmpty()) return currentBatch.getMinTransactionSequence();
      SequenceID currentSequenceID = new SequenceID(sequence.getCurrentSequence());
      return currentSequenceID.next();
    }
  }

}
