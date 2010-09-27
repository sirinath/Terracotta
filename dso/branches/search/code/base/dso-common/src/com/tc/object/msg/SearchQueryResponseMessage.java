/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.SearchRequestID;

import java.util.Set;


/**
 * Response message for object requests.
 * 
 * @author Nabib El-Rahman
 */
public interface SearchQueryResponseMessage extends TCMessage {
  
  /**
   * Search Identifier.
   * 
   * return SearchRequestID requestID
   */
  public SearchRequestID getRequestID();
  
  /**
   * Initialize message.
   * 
   * @param SearchRequestID searchRequestID
   * @param Set<String> keys
   */
  public void initialSearchResponseMessage(final SearchRequestID searchRequestID, final Set<String> keys);
  
  
  /**
   * 
   * @return Set<String> keys.
   */
  public Set<String> getKeys();

}
