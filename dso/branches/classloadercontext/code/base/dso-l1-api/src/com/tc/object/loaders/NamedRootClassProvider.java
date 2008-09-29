/*
 * All content copyright (c) 2008 Terracotta, Inc., except as may otherwise be noted 
 * in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.loaders;

import com.tc.object.TCObject;
import com.tc.object.bytecode.ManagerUtil;

/**
 * A {@link ClassProvider} that first attempts to find a classloader based
 * on the named classloader, and if one is not available, then attempts to use
 * the classloader from a root specified in the TC configuration.
 */
public class NamedRootClassProvider extends StandardClassProvider {
  
  private final String rootName;
  
  public NamedRootClassProvider(String rootName) {
    this.rootName = rootName;
  }
  
  public Class getClassFor(final String className, String desc, ClassloaderContext requestorContext) throws ClassNotFoundException {
    try {
      // First try to use the standard class provider
      return super.getClassFor(className, desc, requestorContext);
    } catch (ClassNotFoundException e) {
      // if that didn't work, try using the classloader of the named root
      ClassLoader cl = getClassLoaderFromNamedRoot();
      if (null == cl) {
        String msg = "Class " + className + 
        " could not be loaded because a classloader could not be obtained from root " + rootName;
        throw new ClassNotFoundException(msg);
      }
      return cl.loadClass(className);
    }
  }
  
  private ClassLoader getClassLoaderFromNamedRoot() {
    Object root = ManagerUtil.lookupRoot(rootName);
    TCObject rootTCO = ManagerUtil.lookupExistingOrNull(root);
    if (rootTCO != null) {
      ClassloaderContext context = rootTCO.getClassloaderContext();
      if (context != null) {
        return context.getRootContextClassLoader();
      }
    }
    return null;
  }
  

}
