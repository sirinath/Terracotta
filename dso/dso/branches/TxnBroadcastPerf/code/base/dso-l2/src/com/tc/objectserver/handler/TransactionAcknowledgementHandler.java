/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.object.msg.AcknowledgeTransactionMessage;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.util.Assert;

public class TransactionAcknowledgementHandler extends AbstractEventHandler {
//  private static final TCLogger logger = TCLogging.getLogger(TransactionAcknowledgementHandler.class);
  private ServerTransactionManager transactionManager;

  public void handleEvent(EventContext context) {
    AcknowledgeTransactionMessage atm = (AcknowledgeTransactionMessage) context;
    int acks = atm.size();
    Assert.assertTrue(acks > 0);
    for (int i = 0; i < acks; ++i) {
//      logger.info("XXX L2 receive ack " + ((DSOMessageBase)atm).getChannelID() + " ack[" + i + "]" + " " + atm.getRequestID(i));
      transactionManager.acknowledgement(atm.getRequesterID(i), atm.getRequestID(i), atm.getClientID());
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.transactionManager = scc.getTransactionManager();
  }

}