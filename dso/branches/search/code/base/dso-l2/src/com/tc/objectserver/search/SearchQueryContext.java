/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.net.NodeID;

/**
 * Context holding search queury search information.
 * TODO: This is just a strawman, need to put actual query string /or classes.
 * 
 * @author Nabib El-Rahman
 */
public class SearchQueryContext implements MultiThreadedEventContext {
  
  private final NodeID id;
  private final String cacheName;
  private final String attributeName;
  private final String attributeValue;
  
  public SearchQueryContext(NodeID id, String cacheName, String attributeName, String attributeValue) {
    this.id = id;
    this.cacheName = cacheName;
    this.attributeName = attributeName;
    this.attributeValue = attributeValue;
  }

  /**
   * Attribute name.
   * 
   * @return String string
   */
  public String getAttributeName() {
    return attributeName;
  }


  /**
   * 
   */
  public String getAttributeValue() {
    return attributeValue;
  }


  /**
   * 
   */
  public Object getCacheName() {
    return cacheName;
  }

  /**
   * {@inheritDoc}
   */
  public Object getKey() {
    return id;
  }

}
