/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.msg.BatchTransactionAcknowledgeMessage;
import com.tc.object.tx.ClientTransactionManager;

public class BatchTransactionAckHandler extends AbstractEventHandler {

  private ClientTransactionManager transactionManager;

  public void handleEvent(EventContext context) {
    BatchTransactionAcknowledgeMessage msg = (BatchTransactionAcknowledgeMessage) context;
    transactionManager.receivedBatchAcknowledgement(msg.getBatchID());
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ClientConfigurationContext cc = (ClientConfigurationContext) context;
    transactionManager = cc.getTransactionManager();
  }

}
