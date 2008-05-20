/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

public class L1AcknowledgeTransactionMessageBatchManager extends AcknowledgeTransactionMessageBatchManager {
  private static final TCLogger                logger = TCLogging
                                                          .getLogger(L1AcknowledgeTransactionMessageBatchManager.class);
  private AcknowledgeTransactionMessageFactory atmFactory;

  public L1AcknowledgeTransactionMessageBatchManager(AcknowledgeTransactionMessageFactory atmFactory) {
    super(logger);
    this.atmFactory = atmFactory;
  }

  public AcknowledgeTransactionMessage createMessage() {
    AcknowledgeTransactionMessage msg = atmFactory.newAcknowledgeTransactionMessage();
    return msg;
  }

}
