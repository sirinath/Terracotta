/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.tool;

import java.util.List;
import java.util.Set;

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

  /**
   * Returns a set of {@link ModuleId}s for each distinct family of modules.
   * A family is the set of modules that have the same groupId and artifactId.
   * Note that the version attribute of the returned ModuleId objects will be
   * null.
   */
  public Set<ModuleId> getModuleFamilies();

  /**
   * Returns the {@link Module} in the given family that has the highest version
   * number.
   */
  public Module getLatestInFamily(ModuleId family);

  /**
   * All the ModuleId that are in the given family.
   */
  public List<ModuleId> getModuleIdsInFamily(ModuleId familyId);

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
}
