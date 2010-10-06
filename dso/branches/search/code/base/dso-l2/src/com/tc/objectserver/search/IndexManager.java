/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.metadata.ValueType;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public interface IndexManager {
  Index getIndex(String name);

  boolean createIndex(String name, Map<String, ValueType> schema) throws IndexException;

  boolean deleteIndex(String name) throws IndexException;
  
  public Set<String> searchIndex(String name, String query) throws IOException;

  void shutdown();
}

