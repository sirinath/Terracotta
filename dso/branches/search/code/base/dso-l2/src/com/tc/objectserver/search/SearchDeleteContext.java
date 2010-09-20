/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.async.api.MultiThreadedEventContext;


/**
 * Context holding search index deletion information.
 *  
 * @author Nabib El-Rahman
 */
public class SearchDeleteContext implements MultiThreadedEventContext {
  
  private final String name;
  private final Object key;
  
  public SearchDeleteContext(String name, Object key) {
    this.name = name;
    this.key = key;
  }
  
  /**
   * Name of index.
   */
  public String getName() {
    return name;
  }

  /**
   * key of cache entry.
   */
  public Object getKey() {
    return key;
  }
  
}