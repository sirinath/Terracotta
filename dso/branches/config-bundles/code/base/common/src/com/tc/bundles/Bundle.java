/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import com.tc.exception.ImplementMe;
import com.terracottatech.configV3.Plugins;

import java.net.MalformedURLException;
import java.net.URL;

public final class Bundle {

  private final String name;
  private final String version;
  private final URL    location;

  public static Bundle[] convertToBundles(final Plugins plugins) throws BundleException {
    final URL[] repositories = new URL[plugins.sizeOfRepositoryArray()];
    for (int pos = 0; pos < repositories.length; ++pos) {
      try {
        repositories[pos] = new URL(plugins.getRepositoryArray(pos));
      } catch (MalformedURLException murle) {
        throw new BundleException("Unable to parse repository[" + plugins.getRepositoryArray(pos) + "]", murle);
      }
    }
    final Bundle[] bundles = new Bundle[plugins.sizeOfPluginArray()];
    for (int pos = 0; pos < bundles.length; ++pos) {
      bundles[pos] = new Bundle(repositories, plugins.getPluginArray(pos).getName(), plugins.getPluginArray(pos)
          .getVersion());
    }
    return bundles;
  }

  private static final URL resolveLocation(final URL[] repositories, final String path) {
    throw new ImplementMe();
  }

  private Bundle(final URL[] repositories, final String name, final String version) {
    this.name = name;
    this.version = version;
    location = resolveLocation(repositories, name + "-" + version + ".jar");
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  public URL getLocation() {
    return location;
  }

}
