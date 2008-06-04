/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import java.util.Timer;
import java.util.TimerTask;

public class L2AcknowledgeTransactionMessageBatchManager extends AcknowledgeTransactionMessageBatchManager {
  private final static boolean  BATCH_ENABLED   = TCPropertiesImpl.getProperties()
                                                    .getBoolean(TCPropertiesConsts.L2_BATCH_TXN_ACK_ENABLED);
  private final static int      BATCH_THRESHOLD = TCPropertiesImpl.getProperties()
                                                    .getInt(TCPropertiesConsts.L2_BATCH_TXN_ACK_THRESHOLD);
  private static final TCLogger logger          = TCLogging
                                                    .getLogger(L2AcknowledgeTransactionMessageBatchManager.class);

  private int                   ackCounter;
  private int                   batchCounter;
  private int                   noPendingCount;
  private int                   maxSendQueuePosition;

  public L2AcknowledgeTransactionMessageBatchManager() {
    super(logger, BATCH_THRESHOLD);
    logStatistics();
  }

  public AcknowledgeTransactionMessage createMessage(MessageChannel channel) {
    AcknowledgeTransactionMessage msg = (AcknowledgeTransactionMessage) channel
        .createMessage(TCMessageType.ACKNOWLEDGE_TRANSACTION_MESSAGE);
    return msg;
  }

  public void sendBatch(DSOMessageBase msg) {
    if (!BATCH_ENABLED) {
      msg.send();
      return;
    }

    // AcknowledgeTransactionMessage acks = (AcknowledgeTransactionMessage) msg;
    // for (int i = 0; i < acks.size(); ++i) {
    // logger.info("XXX L2 Send to " + msg.getChannelID() + " " + acks.getRequestID(i));
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
