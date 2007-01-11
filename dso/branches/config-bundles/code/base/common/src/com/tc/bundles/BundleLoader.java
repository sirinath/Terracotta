/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Poor man's bundle loader, this is used in place of Spring/OSGi/etc. due to time constraints, in time this should
 * DEFINITELY GO AWAY.
 */
public final class BundleLoader {

  private static final String BUNDLE_PATH_FORMAT = "/{0}/{1}/{0}-{1}.jar";

  private final Map           loadedBundles;
  private final URL[]         repositories;

  public BundleLoader(final URL[] repositories) {
    loadedBundles = new HashMap();
    this.repositories = repositories;
  }

  public synchronized ITerracottaBundle getBundle(final String name, final String version)
      throws MalformedURLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
    final String bundlePath = MessageFormat.format(BUNDLE_PATH_FORMAT, new Object[] { name, version });
    final ITerracottaBundle bundle;
    if (loadedBundles.containsKey(bundlePath)) {
      bundle = (ITerracottaBundle) loadedBundles.get(bundlePath);
    } else {
      final URL[] possiblePaths = new URL[repositories.length];
      for (int pos = 0; pos < possiblePaths.length; ++pos) {
        possiblePaths[pos] = new URL(repositories[pos].toString() + bundlePath);
      }
      URLClassLoader bundleClassLoader = new URLClassLoader(possiblePaths);
      bundle = (ITerracottaBundle) bundleClassLoader.loadClass("terracotta.bundle.Bundle").newInstance();
      if (bundle != null) {
        loadedBundles.put(bundlePath, bundle);
      }
    }
    return bundle;
  }

}
