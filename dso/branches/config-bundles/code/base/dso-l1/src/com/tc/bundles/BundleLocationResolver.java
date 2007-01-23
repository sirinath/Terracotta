/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.bundles;

import com.tc.config.schema.NewCommonL1Config;
import com.tc.object.config.DSOClientConfigHelper;
import com.terracottatech.configV3.Plugins;

import java.io.File;
import java.net.URI;
import java.net.URL;

public class BundleLocationResolver {
	private DSOClientConfigHelper config;
	
  /**
   */
  public BundleLocationResolver(DSOClientConfigHelper config) {
	  this.config = config;
  }

  /**
   */
  private URL resolve(String relativePath) 
    throws java.net.URISyntaxException, java.net.MalformedURLException {

	  if (bundleLocations == null) {
      NewCommonL1Config ncl1cfg = config.getNewCommonL1Config();
      Plugins plugins           = ncl1cfg.plugins();
      String[] repositories     = (plugins == null) ? new String[0] : plugins.getRepositoryArray();
      bundleLocations           = new URL[repositories.length];
      for(int i=0; i<repositories.length; i++) {
        bundleLocations[i] = new URL(repositories[i]);
        System.out.println(bundleLocations[i+1].toString());
      }
    }
    
    for(int i=0; i<bundleLocations.length; i++) {
      URI uri   = new URI(bundleLocations[i].toString() + "/" + relativePath + ".jar");
      System.err.println(uri.toString());
      File file = new File(uri);
      if (file.exists()) 
        return uri.toURL();
    }
    
    return null;
  }

  /**
   */
  private URL[] bundleLocations;

  /**
   * Locate a bundle's jar file based on the bundle's name and version. 
   * @param bundleName The name of the bundle
   * @param bundleVersion The vesion number of the bundle in x.x.x format
   * @returns An URL pointing to the location of the bundle jar file
   */
  public URL locateBundle(String bundleName, String bundleVersion) {
    try {
      return resolve(bundleName + "-" + bundleVersion);
    }
    catch (java.net.MalformedURLException murlex) {
      System.err.println(murlex.getMessage());
      return null;
    }
    catch (java.net.URISyntaxException usex) {
      System.err.println(usex.getMessage());
      return null;
    }
  }
}
