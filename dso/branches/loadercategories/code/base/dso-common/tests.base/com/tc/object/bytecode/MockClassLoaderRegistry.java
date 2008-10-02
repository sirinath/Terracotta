/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may 
 * otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.object.loaders.ClassLoaderRegistry;
import com.tc.object.loaders.NamedClassLoader;

public class MockClassLoaderRegistry implements ClassLoaderRegistry {
  
  public String getLoaderDescriptionFor(ClassLoader loader) {
    return "";
  }

  public void registerNamedLoader(NamedClassLoader loader) {
    // do nothing
  }

  public boolean isStandardLoader(String desc) {
    return "".equals(desc);
  }

  public ClassLoader lookupLoader(String desc) {
    return getClass().getClassLoader();
  }

}