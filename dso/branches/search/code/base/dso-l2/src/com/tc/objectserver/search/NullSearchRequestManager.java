/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.net.ClientID;
import com.tc.object.SearchRequestID;
import com.tc.search.SearchQueryResult;

import java.util.Set;

public class NullSearchRequestManager implements SearchRequestManager {

  public void queryRequest(ClientID clientID, SearchRequestID requestID, String cachename, String query,
                           boolean includeKeys, Set<String> attributeSet) {
    // Do nothing
  }

  public void queryResponse(SearchQueryContext queriedContext, Set<SearchQueryResult> results) {
    // Do nothing
  }

}
