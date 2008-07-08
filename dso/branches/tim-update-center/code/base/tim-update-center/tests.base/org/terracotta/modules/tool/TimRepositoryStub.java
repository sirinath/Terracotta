/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.tool;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stub implementation of the {@link TimRepository} interface used by tests.
 */
public class TimRepositoryStub implements TimRepository {

  public List<Tim> listAllCompatibleTims(String tcVersion) {
    List<Tim> result = new ArrayList<Tim>();
    return result;
  }

  public Set<Tim> search(Map<String, String> searchCriteria) {
    return new HashSet<Tim>();
  }

}
