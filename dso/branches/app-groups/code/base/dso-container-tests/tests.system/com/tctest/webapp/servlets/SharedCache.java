/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.webapp.servlets;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SharedCache {
  // @Root
  private Map objects = new ConcurrentHashMap();
  
  public Object get(Object key) {
    return objects.get(key);
  }
  
  public void put(Object key, Object value) {
    objects.put(key, value);
  }
  
  public int size() {
    return objects.size();
  }
  
  public Iterator keys() {
    return objects.keySet().iterator();
  }
}
