/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.metadata.NVPair;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface Index {

  void remove(Object key) throws IndexException;

  void upsert(Object key, List<NVPair> attributes) throws IndexException;
  
  Set<String> search(String query) throws IOException;

  void close();

}
