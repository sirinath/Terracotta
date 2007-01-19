/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import java.net.URL;
import org.osgi.framework.BundleException;
import org.knopflerfish.framework.Framework;

/**
 * KnopflerFish OSGi implementation.
 */
class KnopflerFishBundleManager {
  public void start(final String bundleName, final String bundleVersion) 
    throws BundleException {
    try {
      URL bundleLocation = getBundleLocation(bundleName, bundleVersion);
      installBundle(bundleLocation.toString(), true);
    }
    catch (org.osgi.framework.BundleException bex) {
      throw new BundleException("bleh!", bex);
    }
  }

  public void stop(final String bundleName, final String bundleVersion) 
    throws BundleException {
    // ???
  }
  
  private static Framework framework = null;
  
  private static String OSGI_FWDIR_PROP                  = "org.osgi.framework.dir";
  private static String OSGI_FWDIR_PROP_DEFAULT          = "osgi/fwdir";
  
  private static String OSGI_BOOTDELEGATION_PROP         = "org.osgi.framework.bootdelegation";
  private static String OSGI_BOOTDELEGATION_PROP_DEFAULT = "*";
  
  private static String KF_BUNDLESTORAGE_PROP            = "org.knopflerfish.framework.bundlestorage";
  private static String KF_BUNDLESTORAGE_PROP_DEFAULT    = "memory";
  
  private void startup() {
    System.setProperty(OSGI_BOOTDELEGATION_PROP, OSGI_BOOTDELEGATION_PROP_DEFAULT);
    System.setProperty(KF_BUNDLESTORAGE_PROP, KF_BUNDLESTORAGE_PROP_DEFAULT);
    //System.setProperty(OSGI_FWDIR_PROP, OSGI_FWDIR_PROP_DEFAULT);
    try {
      synchronized(framework) {
        framework = new Framework(null);
        framework.launch(0);
      }
    }
    catch (org.osgi.framework.BundleException bex) {
    }
    catch (Exception ex) {
    }
  }
  
  private void shutdown() {
  }
  
  private void restart() {
  }

  /**
   * Calculates physical location of the bundle's jar file location based on 
   * the bundle's name and version. 
   * @param bundleName The name of the bundle
   * @param bundleVersion The vesion number of the bundle in x.x.x format
   * @returns An URL pointing to the location of the bundle hjar file
   */
  private URL getBundleLocation(final String bundleName, final String bundleVersion) {
    return null; 
  }
  
  private void installBundle(final String bundleLocation, final boolean startBundle) 
    throws org.osgi.framework.BundleException {
    if (framework == null) startup();
    
    long bundleId = framework.installBundle(bundleLocation, null);
    if (startBundle) framework.startBundle(bundleId);
  }
}
