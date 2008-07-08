/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.tool;

import java.util.List;

/**
 * Repository from which information about TIMs can be queried.
 *
 * Implementations of this interface may query a remote service for each request
 * or cache information for a period of time.
 */
public interface TimRepository {

  /**
   * Returns a list of all TIMs available from this repository that are
   * compatible with the given Terracotta version.
   */
  public List<Tim> listAllCompatibleTims(String terracottaVersion);
}
