/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.tc.config.Directories;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.terracottatech.config.Plugins;

import java.io.File;
import java.net.URL;
import java.util.Dictionary;

/**
 * The methods named here are pretty standard; if you don't know what they mean please refer to the documentation at the
 * <a href="http://www.osgi.org/">OSGi web page</a>
 */
public interface EmbeddedOSGiRuntime {

  public static final String PLUGINS_URL_PROPERTY_NAME = "tc.tests.configuration.plugins.url";

  void installBundle(final String bundleName, final String bundleVersion) throws BundleException;

  void startBundle(final String bundleName, final String bundleVersion) throws BundleException;

  Bundle getBundle(String bundleName, String bundleVersion);

  void registerService(final Object serviceObject, final Dictionary serviceProps) throws BundleException;

  void stopBundle(final String bundleName, final String bundleVersion) throws BundleException;

  void uninstallBundle(final String bundleName, final String bundleVersion) throws BundleException;
  
  ServiceReference[] getAllServiceReferences(java.lang.String clazz, java.lang.String filter) throws InvalidSyntaxException;
  
  Object getService(ServiceReference service);
  
  void ungetService(ServiceReference service);

  /**
   * This should shut down the OSGi framework itself and all running bundles.
   */
  void shutdown() throws BundleException;

  static class Factory {
    private static final TCLogger logger = TCLogging.getLogger(EmbeddedOSGiRuntime.Factory.class);
    public static EmbeddedOSGiRuntime createOSGiRuntime(final Plugins plugins) throws Exception {
      int extraRepoCount = 1;
      final String pluginsUrl = System.getProperty(PLUGINS_URL_PROPERTY_NAME);
      if (pluginsUrl != null) {
        extraRepoCount++;
      }

      final URL[] bundleRepositories = new URL[plugins.sizeOfRepositoryArray() + extraRepoCount];
      final File bundleRoot = new File(Directories.getInstallationRoot(), "plugins");
      
      
      bundleRepositories[0] = bundleRoot.toURL();
      if (pluginsUrl != null) {
        bundleRepositories[1] = new URL(pluginsUrl);
      }

      for (int i = extraRepoCount; i < bundleRepositories.length; i++) {
        bundleRepositories[i] = new URL(plugins.getRepositoryArray(i - extraRepoCount));
      }

      logger.info("OSGi Bundle Repositories:");
      for (int i = 0; i < bundleRepositories.length; i++) {
        logger.info(bundleRepositories[i]);
      }
      EmbeddedOSGiRuntime osgiRuntime = new KnopflerfishOSGi(bundleRepositories);
      osgiRuntime.installBundle("plugins-common", "1.0");
      return osgiRuntime;
    }
  }

}
