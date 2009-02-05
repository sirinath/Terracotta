/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.loaders;

/**
 * Manage loading relationship between named classloaders and classes
 */
public interface ClassProvider {

  /**
   * Given a class name and a classloader description, load the class
   * @param className Class name
   * @param desc Classloader description
   * @return Class
   * @throws ClassNotFoundException If class not found through loader
   */
  Class getClassFor(String className, LoaderDescription desc) throws ClassNotFoundException;

  /**
   * Convenience wrapper around {@link #getLoaderDescriptionFor(ClassLoader)}
   * @param clazz a Class loaded by a registered classloader
   */
  LoaderDescription getLoaderDescriptionFor(Class clazz);

  /**
   * Get classloader by name. App group substitution will not take place.
   * @param loaderName Classloader name
   * @return Classloader
   * @throws AssertionError if the specified classloader has not been registered
   */
  ClassLoader getClassLoader(String loaderName);

  /**
   * Get name for classloader. The loader must already have been registered.
   * @param loader Loader
   * @return the description, or null if the loader has not been registered.
   */
  LoaderDescription getLoaderDescriptionFor(ClassLoader loader);
  
  /**
   * Register named loader
   * @deprecated use {@link #registerNamedLoader(NamedClassLoader, String)} to support appGroup.
   * @param loader Loader
   */
  void registerNamedLoader(NamedClassLoader loader);

  /**
   * @param loader must implement both ClassLoader and NamedClassLoader
   * @param appGroup an appGroup to support sharing roots between apps, or null if
   * no sharing is desired. The empty string will be replaced with null.
   */
   void registerNamedLoader(NamedClassLoader loader, String appGroup);

}
