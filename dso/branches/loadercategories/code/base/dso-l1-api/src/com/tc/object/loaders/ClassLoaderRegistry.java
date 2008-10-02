/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may 
 * otherwise be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.object.loaders;

/**
 * Information used by the ClassProvider to choose classloaders.
 */
public interface ClassLoaderRegistry {

  /**
   * Register a named classloader
   * @param loader Loader
   */
  void registerNamedLoader(NamedClassLoader loader);

  /**
   * Look up a named classloader by name
   */
  ClassLoader lookupLoader(String desc);

  /**
   * @param loader may be null, in which case the name of the 
   *        bootclassloader will be returned.
   * @return the name of the classloader, if it has one.
   */
  String getLoaderDescriptionFor(ClassLoader loader);

  /**
   * @return true if the description is the name of a standard loader,
   * in the naming scheme used by this ClassLoaderRegistry.
   */
  boolean isStandardLoader(String desc);
  
}