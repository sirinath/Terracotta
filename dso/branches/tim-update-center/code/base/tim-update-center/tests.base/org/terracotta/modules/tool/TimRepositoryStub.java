/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.tool;

import java.util.ArrayList;
import java.util.List;

/**
 * Stub implementation of the {@link TimRepository} interface used by tests.
 */
public class TimRepositoryStub implements TimRepository {

  public List<Tim> listAllCompatibleTims(String terracottaVersion) {
    return new ArrayList<Tim>();
  }
}
