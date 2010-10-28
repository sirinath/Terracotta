/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.net.ClientID;
import com.tc.object.SearchRequestID;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * Context holding search queury search information. TODO: This is just a strawman, need to put actual query string /or
 * classes.
 * 
 * @author Nabib El-Rahman
 */
public class SearchQueryContext implements MultiThreadedEventContext {

  private final ClientID             clientID;
  private final SearchRequestID      requestID;
  private final String               cacheName;
  private final LinkedList           queryStack;
  private final boolean              includeKeys;
  private final Set<String>          attributeSet;
  private final Map<String, Boolean> sortAttributes;
  private final Map<String, String>  attributeAggregators;

  public SearchQueryContext(ClientID clientID, SearchRequestID requestID, String cacheName, LinkedList queryStack,
                            boolean includeKeys, Set<String> attributeSet, Map<String, Boolean> sortAttributes,
                            Map<String, String> attributeAggregators) {
    this.clientID = clientID;
    this.requestID = requestID;
    this.cacheName = cacheName;
    this.queryStack = queryStack;
    this.includeKeys = includeKeys;
    this.attributeSet = attributeSet;
    this.sortAttributes = sortAttributes;
    this.attributeAggregators = attributeAggregators;
  }

  /**
   * Query stack.
   * 
   * @return LinkedList linkedList
   */
  public LinkedList getQueryStack() {
    return this.queryStack;
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
   * Sorted attributes, pair of attributes if ascending, true
   * 
   * @return Map<String,Boolean> sortAttributes.
   */
  public Map<String, Boolean> getSortAttributes() {
    return sortAttributes;
  }

  /**
   * Attribute aggregators, returns a attribute->aggregator type pairs.
   * 
   * @return Map<String,String>
   */
  public Map<String, String> getAttributeAggregators() {
    return attributeAggregators;
  }

  /**
   * {@inheritDoc}
   */
  public Object getKey() {
    return clientID;
  }

}
