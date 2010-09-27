/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.net.ClientID;
import com.tc.object.SearchRequestID;

import java.util.Set;

/**
 * Manager query request from the client.
 * 
 * 
 * @author Nabib El-Rahman
 */
public interface SearchRequestManager {
  
  /**
   * Query request.
   * TODO: currently just requesting an attribute and value to match against, this will change
   *       drastically when query is built out.
   *       
   * @param ClientID clientID     
   * @param SearchRequestID requestID 
   * @param String cachename
   * @param String attributeName
   * @param String attributeValue
   */
  public void queryRequest(ClientID clientID, SearchRequestID requestID, String cachename, String attributeName,
                           String attributeValue);
  
  /**
   * Query response.
   * 
   * @param SearchQueryContext queriedContext     
   * @param Set<String> keys
   */
  public void queryResponse(SearchQueryContext queriedContext, Set<String> keys);

}
