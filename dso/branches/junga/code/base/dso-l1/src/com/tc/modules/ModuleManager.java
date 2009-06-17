/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.modules;

import com.terracottatech.config.Modules;

public class ModuleManager {

  public static void initModules(Modules modules, ModulesConfiguration modulesConfig) {
    //
  }

  
  // XXX - should this return ModuleInfoGroup or RepositoryInfo???
  public RepositoryInfo getDefaultRepositoryInfo() {
    return null;
  }
}
