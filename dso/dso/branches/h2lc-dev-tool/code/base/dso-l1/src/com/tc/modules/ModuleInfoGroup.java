/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.modules;

import org.osgi.framework.Bundle;

import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Module;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ModuleInfoGroup {
  private final List<ModuleInfo> fModuleInfoList;

  public ModuleInfoGroup() {
    fModuleInfoList = new ArrayList();
  }

  public ModuleInfo add(Module module) {
    ModuleInfo moduleInfo = new ModuleInfo(module);
    fModuleInfoList.add(moduleInfo);
    return moduleInfo;
  }

  public ModuleInfo getOrAdd(Module module) {
    ModuleInfo moduleInfo = getModuleInfo(module);
    if (moduleInfo == null) {
      moduleInfo = add(module);
    }
    return moduleInfo;
  }

  public static boolean sameModule(Module m1, Module m2) {
    return m1 != null && m2 != null && m1.getName().equals(m2.getName()) && m1.getGroupId().equals(m2.getGroupId())
           && m1.getVersion().equals(m2.getVersion());
  }

  public ModuleInfo getModuleInfo(Module module) {
    for (ModuleInfo moduleInfo : fModuleInfoList) {
      if (sameModule(module, moduleInfo.getModule())) { return moduleInfo; }
    }
    return null;
  }

  public ModuleInfo[] getAllModuleInfos() {
    return fModuleInfoList.toArray(new ModuleInfo[0]);
  }

  public ModuleInfo associateBundle(Bundle bundle) {
    for (ModuleInfo moduleInfo : fModuleInfoList) {
      File location = moduleInfo.getLocation();
      String bundleLocation = bundle.getLocation();

      if (location != null) {
        try {
          File bundleFile = new File(new URL(bundleLocation).getPath());
          String bundleFileLocation = bundleFile.getAbsolutePath();
          if (bundleFileLocation.equals(location.getAbsolutePath())) {
            moduleInfo.setBundle(bundle);
            return moduleInfo;
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    return null;
  }

  public void setModuleApplication(ModuleInfo moduleInfo, DsoApplication application) {
    moduleInfo.setApplication(application);
  }
}
