/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.net.ClientID;
import com.tc.object.SearchRequestID;

import java.util.Set;

/**
 * Context holding search queury search information. TODO: This is just a strawman, need to put actual query string /or
 * classes.
 * 
 * @author Nabib El-Rahman
 */
public class SearchQueryContext implements MultiThreadedEventContext {

  private final ClientID        clientID;
  private final SearchRequestID requestID;
  private final String          cacheName;
  private final String          query;
  private final boolean         includeKeys;
  private final Set<String>     attributeSet;

  public SearchQueryContext(ClientID clientID, SearchRequestID requestID, String cacheName, String query,
                            boolean includeKeys, Set<String> attributeSet) {
    this.clientID = clientID;
    this.requestID = requestID;
    this.cacheName = cacheName;
    this.query = query;
    this.includeKeys = includeKeys;
    this.attributeSet = attributeSet;
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
   * Cachename/Index name.
   * 
   * @return String string
   */
  public String getCacheName() {
    return this.cacheName;
  }

  /**
   * Return clientID.
   * 
   * @return ClientID clientID
   */
  public ClientID getClientID() {
    return this.clientID;
  }

  /**
   * SearchRequestID requestID.
   * 
   * @return SearchRequestID requestID
   */
  public SearchRequestID getRequestID() {
    return this.requestID;
  }

  /**
   * Result set should include keys.
   * 
   * @return boolean true if should return keys.
   */
  public boolean includeKeys() {
    return includeKeys;
  }

  /**
   * Attribute keys, should return values with result set.
   * 
   * @return Set<String> attributes.
   */
  public Set<String> getAttributeSet() {
    return attributeSet;
  }

  /**
   * {@inheritDoc}
   */
  public Object getKey() {
    return clientID;
  }

}
