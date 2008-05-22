/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

public class L1AcknowledgeTransactionMessageBatchManager extends AcknowledgeTransactionMessageBatchManager {
  private final static int PRE_BATCH_MESSAGES = 1;  // XXX tunable, read from config

  private static final TCLogger                logger = TCLogging
                                                          .getLogger(L1AcknowledgeTransactionMessageBatchManager.class);
  private AcknowledgeTransactionMessageFactory atmFactory;

  public L1AcknowledgeTransactionMessageBatchManager(AcknowledgeTransactionMessageFactory atmFactory) {
    super(logger, PRE_BATCH_MESSAGES);
    this.atmFactory = atmFactory;
  }

  public AcknowledgeTransactionMessage createMessage() {
    AcknowledgeTransactionMessage msg = atmFactory.newAcknowledgeTransactionMessage();
    return msg;
  }
  
  // for testing
//  public void sendBatch(DSOMessageBase msg) {
//    AcknowledgeTransactionMessage acks = (AcknowledgeTransactionMessage)msg;
//    for(int i = 0; i < acks.size(); ++i) {
//      logger.info("XXX L1 Send to " + msg.getChannelID() + " " + acks.getRequestID(i));
//    }
//    super.sendBatch(msg);
//  }

}
