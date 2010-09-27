/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.net.ClientID;
import com.tc.object.SearchRequestID;

import java.util.Set;

public class NullSearchRequestManager implements SearchRequestManager {

  public void queryRequest(ClientID clientID, SearchRequestID requestID, String cachename, String attributeName,
                           String attributeValue) {
   //Do nothing
  }

  public void queryResponse(SearchQueryContext queriedContext, Set<String> keys) {
   //Do nothing
  }

}
