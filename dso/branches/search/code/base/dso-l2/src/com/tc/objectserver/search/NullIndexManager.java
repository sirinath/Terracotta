/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.metadata.NVPair;
import com.tc.object.metadata.ValueType;
import com.tc.search.SearchQueryResult;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class NullIndexManager implements IndexManager {

  public boolean createIndex(String name, Map<String, ValueType> schema) {
    return false;
  }

  public boolean deleteIndex(String name) {
    return false;
  }

  public Index getIndex(String name) {
    return new NullIndex();
  }

  public Set<SearchQueryResult> searchIndex(String name, String query, boolean includeKeys, Set<String> attributeSet) {
    return null;
  }

  public void shutdown() {
    // no nothing
  }

  private static final class NullIndex implements Index {

    public void close() {//

    }

    public void remove(Object key) {//

    }

    public void upsert(Object key, List<NVPair> attributes) {//

    }

    public Set<SearchQueryResult> search(String query, boolean includeKeys, Set<String> attributeSet) {
      return null;
    }

  }

}
