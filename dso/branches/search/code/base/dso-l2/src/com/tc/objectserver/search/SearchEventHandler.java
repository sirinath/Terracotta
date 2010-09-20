/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.MultiThreadedEventContext;

/**
 * All search request are processed through this handler. Every context
 * should implement {@link MultiThreadedEventContext} so that order can
 * be maintained per client.
 * 
 * @author Nabib El-Rahman
 */
public class SearchEventHandler extends AbstractEventHandler {
  
  private final IndexManager indexManager;
  
  public SearchEventHandler(IndexManager manager) {
    this.indexManager = manager;
  }
   
  /**
   * {@inheritDoc}
   */
  @Override
  public void handleEvent(EventContext context) throws EventHandlerException {
    if(context instanceof SearchUpsertContext) {
      SearchUpsertContext sicc = (SearchUpsertContext)context;
      
      try {
        Index index = this.indexManager.getIndex(sicc.getName());
        if(index == null) {
         boolean created = this.indexManager.createIndex(sicc.getName(), sicc.getSchema());
         if(!created) {
           index = this.indexManager.getIndex(sicc.getName());
         } else {
           //TODO: Return for now, Figure out what do to.
           return;
         }
        }
        index.upsert(sicc.getName(), sicc.getAttributes());
      } catch (IndexException e) {
        //TODO: figure out what to do with IndexException, rethrow for now.
        throw new EventHandlerException(e);
      }
    } else if(context instanceof SearchDeleteContext) {
      SearchDeleteContext sidc = (SearchDeleteContext)context;
      try {
        Index index = this.indexManager.getIndex(sidc.getName());
        index.remove(sidc.getName())
;      } catch (IndexException e) {
        //TODO: figure out what to do with IndexException, rethrow for now.
        throw new EventHandlerException(e);
      }
    } else if( context instanceof SearchQueryContext) {
      //TODO: search lucene index.
    } else {
      throw new AssertionError("Unknown context: " + context );
    }
  }

}

