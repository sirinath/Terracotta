/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.SearchRequestID;

/**
 * The class represents a query request from the client. the cachename is to identify the index and the query string is
 * our client side query in string form.
 * 
 * @author Nabib El-Rahman
 */
public interface SearchQueryRequestMessage extends TCMessage, MultiThreadedEventContext {

  /**
   * Search Identifier. return SearchRequestID requestID
   */
  public SearchRequestID getRequestID();

  /**
   * Initialize message.
   * 
   * @param SearchRequestID searchRequestID
   * @param String cacheName
   * @param String queryString
   */
  public void initialSearchRequestMessage(final SearchRequestID searchRequestID, final String cacheName,
                                          final String queryString);

  /**
   * Name of cache to query against.
   * 
   * @return String string.
   */
  public String getCachename();

  /**
   * Query string to search
   * 
   * @return String string
   */
  public String getQuery();

}
