/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.metadata.NVPair;

import java.util.List;

public interface Index {

  void remove(Object key) throws IndexException;

  void upsert(Object key, List<NVPair> attributes) throws IndexException;

  void close();

}
