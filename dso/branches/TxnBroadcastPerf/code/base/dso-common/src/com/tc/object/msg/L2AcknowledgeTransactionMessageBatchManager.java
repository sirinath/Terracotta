/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;

public class L2AcknowledgeTransactionMessageBatchManager extends AcknowledgeTransactionMessageBatchManager {
  private final static int PRE_BATCH_MESSAGES = 1;  // XXX tunable, read from config
  private static final TCLogger logger = TCLogging.getLogger(L2AcknowledgeTransactionMessageBatchManager.class);

  public L2AcknowledgeTransactionMessageBatchManager() {
    super(logger, PRE_BATCH_MESSAGES);
  }

  public AcknowledgeTransactionMessage createMessage(MessageChannel channel) {
    AcknowledgeTransactionMessage msg = (AcknowledgeTransactionMessage) channel
        .createMessage(TCMessageType.ACKNOWLEDGE_TRANSACTION_MESSAGE);
    return msg;
  }
  
  // for testing
//  public void sendBatch(DSOMessageBase msg) {
//    AcknowledgeTransactionMessage acks = (AcknowledgeTransactionMessage)msg;
//    for(int i = 0; i < acks.size(); ++i) {
//      logger.info("XXX L2 Send to " + msg.getChannelID() + " " + acks.getRequestID(i));
//    }
//    super.sendBatch(msg);
//  }

}
