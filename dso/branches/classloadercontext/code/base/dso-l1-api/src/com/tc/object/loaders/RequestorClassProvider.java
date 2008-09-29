/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.loaders;


/**
 * A ClassProvider that uses the root classloader context stored in the class
 * that is referencing the object being loaded.
 */
public class RequestorClassProvider extends StandardClassProvider {
  public Class getClassFor(final String className, String desc, ClassloaderContext requestorContext) throws ClassNotFoundException {
    try {
      // First try to use the standard class provider
      return super.getClassFor(className, desc, requestorContext);
    } catch (ClassNotFoundException e) {
      // if that didn't work, try using the classloader context from the object requesting this class
      if (null == requestorContext) {
        String msg = "Class " + className + 
        " could not be loaded because the requestor's classloader context was null";
        throw new ClassNotFoundException(msg);
      }
      ClassLoader cl = requestorContext.getRootContextClassLoader();
      if (null == cl) {
        String msg = "Class " + className + 
        " could not be loaded because the requestor's classloader context provided a null classloader";
        throw new ClassNotFoundException(msg);
      }
      return cl.loadClass(className);
    }
  }
  
}
