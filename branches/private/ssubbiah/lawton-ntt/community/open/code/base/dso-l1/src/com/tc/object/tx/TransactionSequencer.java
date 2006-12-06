/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.TCProperties;
import com.tc.util.SequenceGenerator;
import com.tc.util.SequenceID;

public class TransactionSequencer {

  private static final TCLogger         logger         = TCLogging.getLogger(TransactionSequencer.class);

  private static final boolean          LOGGING_ENABLED;
  private static final int              MAX_BYTE_SIZE_FOR_BATCH;
  private static final int              MAX_PENDING_BATCHES;
  private static final long             MAX_SLEEP_TIME_BEFORE_HALT;
  private static final long[]           SLEEP_TIMES;

  static {
    // Set the values from the properties here.
    LOGGING_ENABLED = TCProperties.getProperties().getBoolean("l1.transactionmanager.logging.enabled");
    MAX_BYTE_SIZE_FOR_BATCH = TCProperties.getProperties().getInt("l1.transactionmanager.maxBatchSizeInKiloBytes") * 1024;
    MAX_PENDING_BATCHES = TCProperties.getProperties().getInt("l1.transactionmanager.maxPendingBatches");
    MAX_SLEEP_TIME_BEFORE_HALT = TCProperties.getProperties().getLong("l1.transactionmanager.maxSleepTimeBeforeHalt");
    SLEEP_TIMES = new long[MAX_PENDING_BATCHES];
    initSleepTimes();
  }

  private final SequenceGenerator       sequence       = new SequenceGenerator(1);
  private final TransactionBatchFactory batchFactory;
  private final BoundedLinkedQueue      pendingBatches = new BoundedLinkedQueue(MAX_PENDING_BATCHES);

  private ClientTransactionBatch        currentBatch;
  private int                           pending_size   = 0;

  public TransactionSequencer(TransactionBatchFactory batchFactory) {
    this.batchFactory = batchFactory;
    currentBatch = createNewBatch();
  }

  private static void initSleepTimes() {
    int i = 0;
    int end = MAX_SLEEP_TIME_BEFORE_HALT <= 0 ? SLEEP_TIMES.length : SLEEP_TIMES.length / 2;
    for (; i < end; i++) {
      SLEEP_TIMES[i] = 0;
    }
    int log2 = logBase2(MAX_SLEEP_TIME_BEFORE_HALT);
    int stepSize = log2 > 0 ? (SLEEP_TIMES.length - i) / log2 : Integer.MAX_VALUE;
    int steps = 0;
    int st = 1;
    for (; i < SLEEP_TIMES.length; i++) {
      SLEEP_TIMES[i] = st;
      if (++steps >= stepSize) {
        steps = 0;
        st *= 2;
      }
    }
    if (LOGGING_ENABLED) {
      log_sleep_time();
    }
  }

  private static void log_sleep_time() {
    StringBuffer sb = new StringBuffer("Sleep times are initialized to :");
    for (int i = 0; i < SLEEP_TIMES.length; i++) {
      sb.append(" ").append(SLEEP_TIMES[i]);
    }
    logger.info(sb.toString());
  }

  private static int logBase2(long n) {
    return (int) (Math.log(n) / Math.log(2));
  }

  private ClientTransactionBatch createNewBatch() {
    return batchFactory.nextBatch();
  }

  private void addTransactionToBatch(ClientTransaction txn, ClientTransactionBatch batch) {
    batch.addTransaction(txn);
  }

  /**
   * XXX::Note : There is automatic throttling built in by adding to a BoundedLinkedQueue from within a synch block
   */
  public synchronized void addTransaction(ClientTransaction txn) {
    SequenceID sequenceID = new SequenceID(sequence.getNextSequence());
    txn.setSequenceID(sequenceID);
    addTransactionToBatch(txn, currentBatch);
    if (currentBatch.byteSize() > MAX_BYTE_SIZE_FOR_BATCH) {
      put(currentBatch);
      reconcilePendingSize();
      if (LOGGING_ENABLED) log_size();
      currentBatch = createNewBatch();
    }
    throttle();
  }

  private void throttle() {
    int idx = pending_size - 1;
    if (idx > -1 && idx < SLEEP_TIMES.length) {
      long sleepTime = SLEEP_TIMES[idx];
      if (sleepTime > 0) {
        try {
          wait(sleepTime);
        } catch (InterruptedException e) {
          throw new TCRuntimeException(e);
        }
      }
    }
  }

  private void reconcilePendingSize() {
    pending_size = pendingBatches.size();
  }

  private void put(ClientTransactionBatch batch) {
    try {
      pendingBatches.put(batch);
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  private void log_size() {
    int size = pending_size;
    if (size == MAX_PENDING_BATCHES) {
      logger.info("MAX pending size reached !!! : " + size);
    } else if (size % 5 == 0) {
      logger.info("Pending Batch Size : " + size);
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
      reconcilePendingSize();
      notifyAll();
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
