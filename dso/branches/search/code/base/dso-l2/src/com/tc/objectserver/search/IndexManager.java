/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.metadata.ValueType;
import com.tc.search.SearchQueryResult;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IndexManager {
  Index getIndex(String name);

  boolean createIndex(String name, Map<String, ValueType> schema) throws IndexException;

  boolean deleteIndex(String name) throws IndexException;

  public List<SearchQueryResult> searchIndex(String name, LinkedList queryStack, boolean includeKeys,
                                             Set<String> attributeSet, Map<String, Boolean> sortAttributes,
                                             Set<String> aggregatorAttributes) throws IOException;

  void shutdown();
}
