/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.MultiThreadedEventContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.metadata.AbstractMetaDataHandler;

/**
 * All search request are processed through this handler. Every context should implement
 * {@link MultiThreadedEventContext} so that order can be maintained per client.
 * 
 * @author Nabib El-Rahman
 */
public class SearchEventHandler extends AbstractMetaDataHandler {

  private volatile IndexManager indexManager;

  /**
   * {@inheritDoc}
   */
  @Override
  public void handleMetaDataEvent(EventContext context) throws EventHandlerException {
    if (context instanceof SearchUpsertContext) {
      SearchUpsertContext sicc = (SearchUpsertContext) context;

      try {
        Index index = this.indexManager.getIndex(sicc.getName());
        if (index == null) {
          boolean created = this.indexManager.createIndex(sicc.getName(), sicc.getSchema());
          if (!created) {
            index = this.indexManager.getIndex(sicc.getName());
          } else {
            // TODO: Return for now, Figure out what do to.
            return;
          }
        }
        index.upsert(sicc.getCacheKey(), sicc.getAttributes());
      } catch (IndexException e) {
        // TODO: figure out what to do with IndexException, rethrow for now.
        throw new EventHandlerException(e);
      }
    } else if (context instanceof SearchDeleteContext) {
      SearchDeleteContext sidc = (SearchDeleteContext) context;
      try {
        Index index = this.indexManager.getIndex(sidc.getName());
        index.remove(sidc.getCacheKey());
      } catch (IndexException e) {
        // TODO: figure out what to do with IndexException, rethrow for now.
        throw new EventHandlerException(e);
      }
    } else if (context instanceof SearchQueryContext) {
      // TODO: search lucene index.
    } else {
      throw new AssertionError("Unknown context: " + context);
    }
  }

  @Override
  protected void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext serverContext = (ServerConfigurationContext) context;
    this.indexManager = serverContext.getIndexManager();
  }

}
