/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may
 * otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.loaders;

/**
 * Manage loading relationship between named classloaders and classes
 */
public interface ClassProvider {
  
  /**
   * @return the StandardClassLoaderRegistry that was set with {@link #setRegistry(StandardClassLoaderRegistry)}.
   */
  ClassLoaderRegistry getRegistry();

  /**
   * Set the class provider's registry. Depending on the class provider
   * implementation, different facets of the registry may be used to
   * choose which classloader to load a class with, but in general the
   * class provider will not be useful until it has a working registry.
   * Class provider implementations may throw IllegalStateException if
   * asked to provide a class before the registry is set.
   */
  void setRegistry(ClassLoaderRegistry context);
  
  /**
   * Given a class name and a classloader name, load the class
   * @param className Class name
   * @param loaderDesc Classloader name.
   * @return Class
   * @throws ClassNotFoundException If class not found through loader
   * @throws IllegalStateException if called before setProviderContext.
   */
  Class getClassFor(String className, String loaderDesc) throws ClassNotFoundException;

}
