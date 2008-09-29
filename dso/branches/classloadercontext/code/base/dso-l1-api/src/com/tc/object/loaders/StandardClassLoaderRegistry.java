/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may
 * otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.loaders;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class StandardClassLoaderRegistry implements ClassLoaderRegistry {
  
  /*
   *  Names of standard classloaders. The bootstrap loader string is used to refer to the
   *  null loader; the extensions and system loader strings are registered in
   *  registerStandardLoaders().
   */
  private static final String BOOT    = Namespace.getStandardBootstrapLoaderName();
  private static final String EXT     = Namespace.getStandardExtensionsLoaderName();
  private static final String SYSTEM  = Namespace.getStandardSystemLoaderName();

  /**
   * Maps classloader description string to WeakReference&lt;ClassLoader&gt;
   */
  private final Map loaders = new HashMap();
  
  public StandardClassLoaderRegistry() {
    registerStandardLoaders();
  }
  
  public void registerNamedLoader(NamedClassLoader loader) {
    final String desc = getName(loader);
    synchronized (loaders) {
      loaders.put(desc, new WeakReference(loader));
    }
  }
  
  public ClassLoader lookupLoader(String desc) {
    final ClassLoader rv;
    synchronized (loaders) {
      WeakReference ref = (WeakReference) loaders.get(desc);
      if (ref != null) {
        rv = (ClassLoader) ref.get();
        if (rv == null) {
          loaders.remove(desc);
        }
      } else {
        rv = null;
      }
    }
    return rv;
  }

  public String getLoaderDescriptionFor(ClassLoader loader) {
    if (loader == null) { 
      return BOOT; 
    }
    if (loader instanceof NamedClassLoader) { 
      return getName((NamedClassLoader) loader); 
    }
    throw handleMissingLoader(loader);
  }

  public boolean isStandardLoader(String desc) {
    if (BOOT.equals(desc) || EXT.equals(desc) || SYSTEM.equals(desc)) { return true; }
    return false;
  }
  
  private RuntimeException handleMissingLoader(ClassLoader loader) {
    if ("org.apache.jasper.servlet.JasperLoader".equals(loader.getClass().getName())) {
      // try to give a better error message if you're trying to share a JSP
      return new RuntimeException("JSP instances (and inner classes there of) cannot be distributed, loader = "
                                  + loader);
    }
    return new RuntimeException("No loader description for " + loader);
  }

  private static String getName(NamedClassLoader loader) {
    String name = loader.__tc_getClassLoaderName();
    if (name == null || name.length() == 0) { throw new AssertionError("Invalid name [" + name + "] from loader "
                                                                       + loader); }
    return name;
  }
  
  private void registerStandardLoaders() {
    ClassLoader loader1 = ClassLoader.getSystemClassLoader();
    ClassLoader loader2 = loader1.getParent();
    ClassLoader loader3 = loader2.getParent();

    final ClassLoader sunSystemLoader;
    final ClassLoader extSystemLoader;

    if (loader3 != null) { // user is using alternate system loader
      sunSystemLoader = loader2;
      extSystemLoader = loader3;
    } else {
      sunSystemLoader = loader1;
      extSystemLoader = loader2;
    }

    registerNamedLoader((NamedClassLoader) sunSystemLoader);
    registerNamedLoader((NamedClassLoader) extSystemLoader);
  }




}
