/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.modules;

import org.osgi.framework.Bundle;

import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Module;

import java.io.File;
import java.util.Dictionary;

public class ModuleInfo {
  private final Module   fModule;
  private File           fLocation;
  private Bundle         fBundle;
  private Exception      fError;
  private DsoApplication fApplication;
  private String         fDescription;

  public ModuleInfo(Module module) {
    fModule = module;
  }

  public Module getModule() {
    return fModule;
  }

  public void setLocation(File location) {
    fLocation = location;
  }

  public File getLocation() {
    return fLocation;
  }

  public void setBundle(Bundle bundle) {
    fBundle = bundle;
    if (fBundle != null) {
      Dictionary headers = bundle.getHeaders();
      fDescription = (String) headers.get("Bundle-Description");
    }
  }

  public void setError(Exception error) {
    fError = error;
  }

  public Exception getError() {
    return fError;
  }

  public void setApplication(DsoApplication application) {
    fApplication = application;
  }

  public DsoApplication getApplication() {
    return fApplication;
  }

  public String getDescription() {
    return fDescription;
  }
}
