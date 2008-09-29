/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.object.loaders.ClassLoaderRegistry;

import java.io.Serializable;

public class ClassLoaderInstance implements Serializable {

  private final UTF8ByteDataHolder loaderDef;

  public ClassLoaderInstance(UTF8ByteDataHolder loaderDefinition) {
    loaderDef = loaderDefinition;
  }

  public boolean equals(Object obj) {
    if (obj instanceof ClassLoaderInstance) {
      ClassLoaderInstance other = (ClassLoaderInstance) obj;
      return this.loaderDef.equals(other.loaderDef);
    }
    return false;
  }

  /**
   * Note that this gets the classloader from the registry, not from a
   * ClassProvider. If the ClassProvider in use has a fallback strategy,
   * the ClassLoader that it actually uses to provide classes for any
   * particular class may or may not be the one specified by loaderDef.
   */
  public ClassLoader asClassLoader(ClassLoaderRegistry classLoaderRegistry) {
    String classLoaderdef = loaderDef.asString();
    return classLoaderRegistry.lookupLoader(classLoaderdef);
  }

  public int hashCode() {
    int hash = 17;
    hash = (37 * hash) + loaderDef.hashCode();
    return hash;
  }

  public UTF8ByteDataHolder getLoaderDef() {
    return loaderDef;
  }
}
