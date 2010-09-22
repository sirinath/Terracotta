/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.metadata;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.tx.ServerTransactionManager;

/**
 * 
 */
public abstract class AbstractMetaDataHandler extends AbstractEventHandler {

  private volatile MetaDataManager          manager;
  private volatile ServerTransactionManager txnManager;

  @Override
  public void handleEvent(EventContext context) throws EventHandlerException {

    handleMetaDataEvent(context);

    if (context instanceof AbstractMetaDataContext) {
      AbstractMetaDataContext metaDataContext = (AbstractMetaDataContext) context;
      if (this.manager.metaDataProcessingCompleted(metaDataContext.getTransactionID())) {
          this.txnManager.processingMetaDataCompleted(metaDataContext.getSourceID(), metaDataContext.getTransactionID());
      }
    }
  }

  public abstract void handleMetaDataEvent(EventContext context) throws EventHandlerException;

  @Override
  protected void initialize(ConfigurationContext context) {
    ServerConfigurationContext serverContext = (ServerConfigurationContext) context;
    this.manager = serverContext.getMetaDataManager();
    this.txnManager = serverContext.getTransactionManager();
  }

}
