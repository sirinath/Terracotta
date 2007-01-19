/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import java.net.URL;
import java.util.Hashtable;
import org.osgi.framework.BundleContext;
import org.knopflerfish.framework.Framework;
//import org.osgi.framework.BundleException;
//import com.tc.bundles.BundleManager;

/**
 * BundleManager that uses the KnopflerFish OSGi implementation.
 */
public class KnopflerFishBundleManager 
  implements com.tc.bundles.BundleManager {
  
  public void startBundle(final String bundleName, final String bundleVersion) 
    throws com.tc.bundles.BundleException {
    try {
      URL bundleLocation = getBundleLocation(bundleName, bundleVersion);
      installBundle(bundleLocation.toString(), true);
    }
    catch (org.osgi.framework.BundleException bex) {
      throw new com.tc.bundles.BundleException("bleh!", bex);
    }
    catch (Exception ex) {
      throw new com.tc.bundles.BundleException("bleh!", ex);
    }
  }

  public void stopBundle(final String bundleName, final String bundleVersion) 
    throws com.tc.bundles.BundleException {
    // do we even need to do this?
  }

  /**
   * Install an OSGi service.
   * @param serviceObject The service to install
   * @param serviceProps Properties for the service
   */
  public void installService(Object serviceObject, Hashtable serviceProps) 
    throws com.tc.bundles.BundleException {
    try {
      if (framework == null) startup();
      systemBC.registerService(serviceObject.getClass().getName(), serviceObject, serviceProps);
    }
    catch (IllegalStateException isex) {
      System.out.println(isex.getMessage());
      throw new com.tc.bundles.BundleException("bleh!", isex);
    }
    catch (Exception ex) {
      System.out.println(ex.getMessage());
      throw new com.tc.bundles.BundleException("bleh!", ex);
    }
  }

  private static Framework framework                     = null;
  private static BundleContext systemBC                  = null;
  
  private static String OSGI_FWDIR_PROP                  = "org.osgi.framework.dir";
  private static String OSGI_FWDIR_PROP_DEFAULT          = "osgi/fwdir";
  
  private static String OSGI_BOOTDELEGATION_PROP         = "org.osgi.framework.bootdelegation";
  private static String OSGI_BOOTDELEGATION_PROP_DEFAULT = "*";
  
  private static String KF_BUNDLESTORAGE_PROP            = "org.knopflerfish.framework.bundlestorage";
  private static String KF_BUNDLESTORAGE_PROP_DEFAULT    = "memory";

  /**
   * Creates an instance of the KnopflerFish OSGi framework
   * and launches it.
   */
  private void startup() 
    throws Exception {
    //System.setProperty(OSGI_FWDIR_PROP, OSGI_FWDIR_PROP_DEFAULT);
    System.setProperty(OSGI_BOOTDELEGATION_PROP, OSGI_BOOTDELEGATION_PROP_DEFAULT);
    System.setProperty(KF_BUNDLESTORAGE_PROP, KF_BUNDLESTORAGE_PROP_DEFAULT);
    try {
      framework = new Framework(null);
      framework.launch(0);
      systemBC = framework.getSystemBundleContext();
    }
    catch (Exception ex) {
      ex.printStackTrace();
      throw ex;
    }
  }

  /**
   * Calculates physical location of the bundle's jar file location based on 
   * the bundle's name and version. 
   * @param bundleName The name of the bundle
   * @param bundleVersion The vesion number of the bundle in x.x.x format
   * @returns An URL pointing to the location of the bundle hjar file
   */
  private URL getBundleLocation(String bundleName, String bundleVersion) {
    return null; 
  } 

  /**
   * Installs a bundle, and optionally start it.
   * This will also create (if needed) an instance of KF and launch it.
   * @param bundleLocation The bundle's jar file location (eg: file:/path/to/jar/file)
   * @param startBundle The flag to indicate if the newly installed bundle should also start
   */
  private void installBundle(String bundleLocation, boolean startBundle) 
    throws org.osgi.framework.BundleException, Exception {
    if (framework == null) startup();
    long bundleId = framework.installBundle(bundleLocation, null);
    if (startBundle) framework.startBundle(bundleId);
  }
}
