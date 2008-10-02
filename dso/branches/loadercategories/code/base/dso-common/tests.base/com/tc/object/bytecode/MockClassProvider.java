/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.object.loaders.ClassLoaderRegistry;
import com.tc.object.loaders.ClassProvider;


public class MockClassProvider implements ClassProvider {
  
  private ClassLoaderRegistry registry = new MockClassLoaderRegistry();
  
  public MockClassProvider() {
    super();
  }

  public Class getClassFor(String className, String loaderDesc) throws ClassNotFoundException {
    return getClass().getClassLoader().loadClass(className);
  }

  public void setRegistry(ClassLoaderRegistry context) {
    throw new AssertionError("Shouldn't set ClassLoaderRegistry for a MockClassProvider; it is already set");
  }

  public ClassLoaderRegistry getRegistry() {
    return registry;
  }

}
