/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.net.ClientID;
import com.tc.object.SearchRequestID;

/**
 * Context holding search queury search information.
 * TODO: This is just a strawman, need to put actual query string /or classes.
 * 
 * @author Nabib El-Rahman
 */
public class SearchQueryContext implements MultiThreadedEventContext {
  
  private final ClientID clientID;
  private final SearchRequestID requestID;
  private final String cacheName;
  private final String query;
  
  public SearchQueryContext(ClientID clientID, SearchRequestID requestID, String cacheName, String query) {
    this.clientID = clientID;
    this.requestID = requestID;
    this.cacheName = cacheName;
    this.query = query;
  }

  /**
   * Query string.
   * 
   * @return String string
   */
  public String getQuery() {
    return this.query;
  }

  /**
   * Cachename/Index name
   * 
   * @return String string
   */
  public String getCacheName() {
    return this.cacheName;
  }
  
  /**
   * Return clientID
   * 
   * @return ClientID clientID
   */
  public ClientID getClientID() {
    return this.clientID;
  }
  
  /**
   * SearchRequestID requestID
   * 
   * @return SearchRequestID requestID
   */
  public SearchRequestID getRequestID() {
    return this.requestID;
  }
  
  /**
   * {@inheritDoc}
   */
  public Object getKey() {
    return clientID;
  }

}
