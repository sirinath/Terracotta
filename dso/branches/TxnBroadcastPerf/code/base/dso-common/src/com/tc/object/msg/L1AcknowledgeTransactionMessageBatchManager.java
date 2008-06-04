/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import java.util.Timer;
import java.util.TimerTask;

public class L1AcknowledgeTransactionMessageBatchManager extends AcknowledgeTransactionMessageBatchManager {
  private final static boolean                 BATCH_ENABLED   = TCPropertiesImpl
                                                                   .getProperties()
                                                                   .getBoolean(
                                                                               TCPropertiesConsts.L1_BATCH_TXN_ACK_ENABLED);
  private final static int                     BATCH_THRESHOLD = TCPropertiesImpl
                                                                   .getProperties()
                                                                   .getInt(
                                                                           TCPropertiesConsts.L1_BATCH_TXN_ACK_THRESHOLD);
  private static final TCLogger                logger          = TCLogging
                                                                   .getLogger(L1AcknowledgeTransactionMessageBatchManager.class);
  private AcknowledgeTransactionMessageFactory atmFactory;

  private int                                  ackCounter;
  private int                                  batchCounter;
  private int                                  noPendingCount;
  private int                                  maxSendQueuePosition;

  public L1AcknowledgeTransactionMessageBatchManager(AcknowledgeTransactionMessageFactory atmFactory) {
    super(logger, BATCH_THRESHOLD);
    this.atmFactory = atmFactory;
    logStatistics();
  }

  public AcknowledgeTransactionMessage createMessage() {
    AcknowledgeTransactionMessage msg = atmFactory.newAcknowledgeTransactionMessage();
    return msg;
  }

  public void sendBatch(DSOMessageBase msg) {
    if (!BATCH_ENABLED) {
      msg.send();
    }

    // AcknowledgeTransactionMessage acks = (AcknowledgeTransactionMessage) msg;
    // for (int i = 0; i < acks.size(); ++i) {
    // logger.info("XXX L1 Send to " + msg.getChannelID() + " " + acks.getRequestID(i));
    // }
    super.sendBatch(msg);
  }

  private void logStatistics() {
    TimerTask task = new TimerTask() {
      public void run() {
        logger.info("XXX L1toL2 batches=" + batchCounter + " acks=" + ackCounter + " noPending=" + noPendingCount + " maxQueue="
                    + maxSendQueuePosition);
      }
    };
    new Timer().schedule(task, 1000, 60000);
  }

  public void sendStatisticsRecord(DSOMessageBase msg) {
    ++batchCounter;
    ackCounter += ((AcknowledgeTransactionMessage) msg).size();
  }

  public void sendStatisticsNoPending() {
    ++noPendingCount;
  }

  public void sendStatisticsQueuePosition(int sendQueuePosition) {
    if (sendQueuePosition > maxSendQueuePosition) {
      maxSendQueuePosition = sendQueuePosition;
    }
  }
}
