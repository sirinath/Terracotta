/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.search;

import com.tc.io.TCSerializable;

import java.util.Map;

public interface SearchQueryResult extends TCSerializable {

  public String getKey();

  public Map<String, String> getAttributes();

}
