/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

public class AcknowledgeTransactionMessageBatchManager extends AbstractMessageBatchManager {
  private static final TCLogger logger = TCLogging.getLogger(AcknowledgeTransactionMessageBatchManager.class);

  public AcknowledgeTransactionMessageBatchManager() {
    super(logger);
  }

  protected void queueMessageToBatch(DSOMessageBase batchMsg, DSOMessageBase msg) {
    AcknowledgeTransactionMessage ackBatch = (AcknowledgeTransactionMessage) batchMsg;
    AcknowledgeTransactionMessage ack = (AcknowledgeTransactionMessage) msg;
    ackBatch.addAckMessage(ack.getRequestID());
//    logger.info("XXX batching to " + batchMsg.getChannelID() + " size=" + ackBatch.size() + " with " + ack.getRequestID());
  }

}
