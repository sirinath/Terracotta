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
 * The idea here is to specify exactly what functionality we need from a bundle layer, the implementation can then be an
 * OSGi implementation, our own, something else...
 */
public interface BundleManager {

  void installBundle(final String bundleName, final String bundleVersion) throws BundleException;

  void startBundle(final String bundleName, final String bundleVersion) throws BundleException;

  void installService(final Object serviceObject, final Dictionary serviceProps) throws BundleException;

  void stopBundle(final String bundleName, final String bundleVersion) throws BundleException;

  void uninstallBundle(final String bundleName, final String bundleVersion) throws BundleException;

  static class Factory {
    public static BundleManager createOSGiRuntime(final Plugins plugins) throws Exception {
      final File bundleRoot = new File(Directories.getInstallationRoot(), "plugins");
      final URL[] bundleRepositories = new URL[plugins.sizeOfRepositoryArray() + 1];
      bundleRepositories[0] = bundleRoot.toURL();
      for (int pos = 1; pos < bundleRepositories.length; ++pos) {
        bundleRepositories[pos] = new URL(plugins.getRepositoryArray(pos - 1));
      }
      return new KnopflerFishBundleManager(bundleRepositories);
    }
  }

}
