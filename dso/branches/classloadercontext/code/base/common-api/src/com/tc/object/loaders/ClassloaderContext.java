/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.loaders;

/**
 * This class is immutable and has no public constructors or setters.
 * There are in general zillions of TCObjects, referencing at most a few dozen
 * ClassloaderContexts.  By making the class immutable it is possible to pool 
 * ClassloaderContexts, so that identical contexts can be reused, similar to
 * the way String interning works.
 */
public class ClassloaderContext {
  private final ClassLoader rootContextClassLoader;
  
  // TODO: define a static pool of classloader contexts
  
  /**
   * Factory
   */
  public static ClassloaderContext getClassloaderContext(ClassLoader rootContextClassLoader) {
    // TODO: if the classloader context already exists in the pool, use it.
    return new ClassloaderContext(rootContextClassLoader);
  }
  
  public ClassLoader getRootContextClassLoader() {
    return rootContextClassLoader;
  }

  /**
   * Constructor is private: use {@link #getClassloaderContext(ClassLoader)}
   */
  private ClassloaderContext() {
    this(null);
  }
  
  private ClassloaderContext(ClassLoader rootContextClassLoader) {
    this.rootContextClassLoader = rootContextClassLoader;
  }
}
