/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config;

/**
 * Provides default no-op method implementations for the ModuleSpec interface.
 */
public abstract class AbstractModuleSpec implements ModuleSpec {

  public ChangeApplicatorSpec getChangeApplicatorSpec() {
    return null;
  }

  public Class getPeerClass(Class clazz) {
    return null;
  }

  public String[] getTunneledMBeanDomains() {
    return null;
  }

  public boolean isPortableClass(Class clazz) {
    return false;
  }

  public boolean isUseNonDefaultConstructor(Class clazz) {
    return false;
  }

}
