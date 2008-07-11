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
public interface Modules {

  /**
   * Returns a {@link Module} object for the TIM with the given ID.
   */
  public Module getModuleById(ModuleId id);

  public List<Module> getSiblingModules(ModuleId id);

  /**
   * Returns a list of all TIMs available from this repository that are
   * compatible with the current Terracotta version.
   */
  public List<Module> list();

  /**
   * Returns a filtered list of all TIMs. The filter limits the list so
   * that only the latest versions are included in the list.  
   */
  public List<Module> listLatest();
  
  /**
   * Return a list of modules having the same groupId and artifactId
   */
  public List<Module> getModulesByName(String groupId, String artifactId);
}
