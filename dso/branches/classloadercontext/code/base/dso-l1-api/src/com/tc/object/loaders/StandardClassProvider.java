/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.loaders;

import com.tc.aspectwerkz.transform.inlining.AsmHelper;


/**
 * Standard ClassProvider, using named classloaders and aware of boot, extension, and system classloaders.
 * This class provider does not make use of the ClassloaderContext.
 */
public class StandardClassProvider implements ClassProvider {
  
  protected ClassLoaderRegistry registry;

  public Class getClassFor(final String className, String desc, ClassloaderContext context) throws ClassNotFoundException {
    if (registry == null) {
      throw new IllegalStateException("getClassFor() was called with a null provider registry");
    }
    ClassLoader loader = getClassLoader(desc);
    if (loader == null) { 
        throw new ClassNotFoundException("Could not find classloader for description: " + desc
                                                             + ", trying to load " + className); 
    }

    try {
      return Class.forName(className, false, loader);
    } catch (ClassNotFoundException e) {
      if (loader instanceof BytecodeProvider) {
        BytecodeProvider provider = (BytecodeProvider) loader;
        byte[] bytes = provider.__tc_getBytecodeForClass(className);
        if (bytes != null && bytes.length != 0) { 
          return AsmHelper.defineClass(loader, bytes, className); 
        }
      }
      throw e;
    }
  }

  private static class SystemLoaderHolder {
    final static ClassLoader loader = ClassLoader.getSystemClassLoader();
  }

  public void setRegistry(ClassLoaderRegistry registry) {
    this.registry = registry;
  }

  protected ClassLoader getClassLoader(String desc) {
    ClassLoader rv = null;
    if (registry.isStandardLoader(desc)) { 
      return SystemLoaderHolder.loader; 
    }
    rv = registry.lookupLoader(desc);
    return rv;
  }

  public ClassLoaderRegistry getRegistry() {
    return registry;
  }

}
