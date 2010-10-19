/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.SearchRequestID;
import com.tc.search.SearchQueryResult;

import java.util.Set;

/**
 * Response message for object requests.
 * 
 * @author Nabib El-Rahman
 */
public interface SearchQueryResponseMessage extends TCMessage {

  /**
   * Search Identifier. return SearchRequestID requestID
   */
  public SearchRequestID getRequestID();

  /**
   * Initialize message.
   * 
   * @param SearchRequestID searchRequestID
   * @param Set<SearchQueryResult> results
   */
  public void initialSearchResponseMessage(SearchRequestID searchRequestID, Set<SearchQueryResult> results);

  /**
   * @return Set<SearchQueryResult> results.
   */
  public Set<SearchQueryResult> getResults();

}
