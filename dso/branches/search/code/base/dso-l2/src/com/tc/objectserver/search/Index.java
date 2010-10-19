/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.metadata.NVPair;
import com.tc.search.SearchQueryResult;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface Index {

  void remove(Object key) throws IndexException;

  void upsert(Object key, List<NVPair> attributes) throws IndexException;

  Set<SearchQueryResult> search(String query, boolean includeKeys, Set<String> attributeSet) throws IOException;

  void close();

}
