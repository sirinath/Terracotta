/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import org.osgi.framework.BundleException;

import com.tc.config.Directories;
import com.terracottatech.configV3.Plugins;

import java.io.File;
import java.net.URL;
import java.util.Dictionary;

/**
 * The methods named here are pretty standard; if you don't know what they mean please refer to the documentation at the
 * <a href="http://www.osgi.org/">OSGi web page</a>
 */
public interface EmbeddedOSGiRuntime {

  void installBundle(final String bundleName, final String bundleVersion) throws BundleException;

  void startBundle(final String bundleName, final String bundleVersion) throws BundleException;

  void registerService(final Object serviceObject, final Dictionary serviceProps) throws BundleException;

  void stopBundle(final String bundleName, final String bundleVersion) throws BundleException;

  void uninstallBundle(final String bundleName, final String bundleVersion) throws BundleException;

  /**
   * This should shut down the OSGi framework itself and all running bundles.
   */
  void shutdown() throws BundleException;

  static class Factory {
    public static EmbeddedOSGiRuntime createOSGiRuntime(final Plugins plugins) throws Exception {
      final File bundleRoot = new File(Directories.getInstallationRoot(), "plugins");
      final URL[] bundleRepositories = new URL[plugins.sizeOfRepositoryArray() + 1];
      bundleRepositories[0] = bundleRoot.toURL();
      for (int pos = 1; pos < bundleRepositories.length; ++pos) {
        bundleRepositories[pos] = new URL(plugins.getRepositoryArray(pos - 1));
      }
      return new KnopflerfishOSGi(bundleRepositories);
    }
  }

}
