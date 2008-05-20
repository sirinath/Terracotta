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
  private static final TCLogger logger = TCLogging.getLogger(L2AcknowledgeTransactionMessageBatchManager.class);

  public L2AcknowledgeTransactionMessageBatchManager() {
    super(logger);
  }

  public AcknowledgeTransactionMessage createMessage(MessageChannel channel) {
    AcknowledgeTransactionMessage msg = (AcknowledgeTransactionMessage) channel
        .createMessage(TCMessageType.ACKNOWLEDGE_TRANSACTION_MESSAGE);
    return msg;
  }

}
