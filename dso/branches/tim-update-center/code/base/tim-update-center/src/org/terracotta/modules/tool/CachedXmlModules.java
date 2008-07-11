/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.tool;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link Modules} that uses a cached XML file as its
 * data source.
 */
public class CachedXmlModules implements Modules {

  public List<Module> listAllCompatibleTims(String terracottaVersion) {
    // TODO: implement me
    return new ArrayList<Module>();
  }

  public Module getModuleById(ModuleId id) {
    return null;
  }
}
