/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.metadata.ValueType;

import java.util.Map;

public interface IndexManager {
  Index getIndex(String name);

  boolean createIndex(String name, Map<String, ValueType> schema) throws IndexException;

  boolean deleteIndex(String name) throws IndexException;

  void shutdown();
}

