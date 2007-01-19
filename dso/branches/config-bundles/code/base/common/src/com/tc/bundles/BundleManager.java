/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import java.util.Hashtable;

/**
 * The idea here is to specify exactly what functionality we need from a bundle layer, the implementation can then be an
 * OSGi implementation, our own, something else...
 */
public interface BundleManager {
  
  void startBundle(final String bundleName, final String bundleVersion) 
    throws BundleException;

  void stopBundle(final String bundleName, final String bundleVersion) 
    throws BundleException;
  
  void installService(Object serviceObject, Hashtable serviceProps) 
    throws BundleException; // should probably throw a different exception type

}
