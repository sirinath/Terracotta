/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.metadata.AbstractNVPair;
import com.tc.search.SearchQueryResult;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Index {

  void remove(Object key) throws IndexException;

  void upsert(Object key, List<AbstractNVPair> attributes) throws IndexException;

  List<SearchQueryResult> search(LinkedList queryStack, boolean includeKeys, Set<String> attributeSet,
                                 Map<String, Boolean> sortAttributes, Set<String> aggregatorAttributes)
      throws IOException;

  void close();

}
