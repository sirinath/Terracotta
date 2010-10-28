/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.RemoteSearchRequestManager;
import com.tc.object.msg.SearchQueryResponseMessage;
import com.tc.object.msg.SearchQueryResultsImpl;
import com.tc.search.SearchQueryResults;

public class ReceiveSearchQueryResponseHandler extends AbstractEventHandler {

  private final RemoteSearchRequestManager remoteSearchRequestManager;

  public ReceiveSearchQueryResponseHandler(final RemoteSearchRequestManager remoteSearchRequestManager) {
    this.remoteSearchRequestManager = remoteSearchRequestManager;
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof SearchQueryResponseMessage) {
      final SearchQueryResponseMessage responseMsg = (SearchQueryResponseMessage) context;
      final SearchQueryResults results = new SearchQueryResultsImpl(responseMsg.getResults(), responseMsg
          .getAggregatorResults());
      this.remoteSearchRequestManager.addResponseForQuery(responseMsg.getLocalSessionID(), responseMsg.getRequestID(),
                                                          results, responseMsg.getSourceNodeID());
    } else {
      throw new AssertionError("Unknown message type received from server - " + context.getClass().getName());
    }
  }
}
