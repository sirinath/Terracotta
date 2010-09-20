/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.async.api.MultiThreadedEventContext;

/**
 * Context holding search queury search information.
 * TODO: This is just a strawman, need to put actual query string /or classes.
 * 
 * @author Nabib El-Rahman
 */
public interface SearchQueryContext extends MultiThreadedEventContext {

  /**
   * Query string
   * 
   * @return String string
   */
  public String getQueryString();
  
  /**
   * {@inheritDoc}
   */
  public Object getKey();

}
