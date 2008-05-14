/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.msg.AcknowledgeTransactionMessage;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.util.Assert;

/**
 * @author steve
 */
public class ReceiveTransactionCompleteHandler extends AbstractEventHandler {
  private ClientTransactionManager transactionManager;

  public void handleEvent(EventContext context) {
    AcknowledgeTransactionMessage atm = (AcknowledgeTransactionMessage) context;
    int acks = atm.acksBatchSize();
    Assert.assertTrue(acks > 0);
    for(int i = 0; i < acks; ++i) {
      transactionManager.receivedAcknowledgement(atm.getLocalSessionID(), atm.getRequestID(i));
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ClientConfigurationContext cc = (ClientConfigurationContext) context;
    transactionManager = cc.getTransactionManager();
  }
}