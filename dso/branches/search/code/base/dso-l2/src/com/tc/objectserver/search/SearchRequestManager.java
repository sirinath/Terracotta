/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.net.ClientID;
import com.tc.object.SearchRequestID;
import com.tc.search.SearchQueryResult;

import java.util.Set;

/**
 * Manager query request from the client.
 * 
 * @author Nabib El-Rahman
 */
public interface SearchRequestManager {

  /**
   * Query request. TODO: currently just requesting an attribute and value to match against, this will change
   * drastically when query is built out.
   * 
   * @param ClientID clientID
   * @param SearchRequestID requestID
   * @param String cachename
   * @param String query
   * @param boolean includeKeys
   * @param Set<String> attributeSet
   */
  public void queryRequest(ClientID clientID, SearchRequestID requestID, String cachename, String query,
                           boolean includeKeys, Set<String> attributeSet);

  /**
   * Query response.
   * 
   * @param SearchQueryContext queriedContext
   * @param Set<SearchQueryResult> results
   */
  public void queryResponse(SearchQueryContext queriedContext, Set<SearchQueryResult> results);

}
