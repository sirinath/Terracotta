/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.logging.TCLogger;

abstract public class AcknowledgeTransactionMessageBatchManager extends AbstractMessageBatchManager {

  public AcknowledgeTransactionMessageBatchManager(TCLogger logger, int preBatchMessages) {
    super(logger, preBatchMessages);
  }

  protected void queueMessageToBatch(DSOMessageBase batchMsg, DSOMessageBase msg) {
    AcknowledgeTransactionMessage ackBatch = (AcknowledgeTransactionMessage) batchMsg;
    AcknowledgeTransactionMessage ack = (AcknowledgeTransactionMessage) msg;
    ackBatch.addAckMessage(ack.getRequesterID(0), ack.getRequestID(0));
    // logger.info("XXX batching to " + batchMsg.getChannelID() + " size=" + ackBatch.size() + " with " +
    // ack.getRequestID());
  }

}
