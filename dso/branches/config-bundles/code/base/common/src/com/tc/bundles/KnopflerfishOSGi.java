/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import org.knopflerfish.framework.Framework;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import com.tc.net.util.URLUtil;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Dictionary;

/**
 * Embedded KnopflerFish OSGi implementation, see the <a href="http://www.knopflerfish.org/">Knopflerfish documentation</a>
 * for more details.
 */
final class KnopflerfishOSGi implements EmbeddedOSGiRuntime {

  private static String       KF_BUNDLESTORAGE_PROP         = "org.knopflerfish.framework.bundlestorage";
  private static String       KF_BUNDLESTORAGE_PROP_DEFAULT = "memory";

  private static final String BUNDLE_PATH                   = "{0}-{1}.jar";

  private final URL[]         bundleRepositories;
  private final Framework     framework;

  static {
    System.setProperty(Constants.FRAMEWORK_BOOTDELEGATION, "*");
    System.setProperty(KF_BUNDLESTORAGE_PROP, KF_BUNDLESTORAGE_PROP_DEFAULT);
  }

  /**
   * Creates and starts an in-memory OSGi layer using Knopflerfish.
   */
  public KnopflerfishOSGi(final URL[] bundleRepositories) throws Exception {
    this.bundleRepositories = bundleRepositories;
    framework = new Framework(null);
    framework.launch(0);
  }

  public void installBundle(final String bundleName, final String bundleVersion) throws BundleException {
    final URL bundleLocation = getBundleURL(bundleName, bundleVersion);
    if (bundleLocation != null) {
      try {
        framework.installBundle(bundleLocation.toString(), bundleLocation.openStream());
      } catch (IOException ioe) {
        throw new BundleException("Unable to open URL[" + bundleLocation + "]", ioe);
      }
    } else {
      throw new BundleException("Unable to find bundle '" + bundleName + "', version '" + bundleVersion
          + "' in any repository");
    }
  }

  public void startBundle(final String bundleName, final String bundleVersion) throws BundleException {
    framework.startBundle(getBundleID(bundleName, bundleVersion));
  }

  public void installService(final Object serviceObject, final Dictionary serviceProps) throws BundleException {
    framework.getSystemBundleContext().registerService(serviceObject.getClass().getName(), serviceObject, serviceProps);
  }

  public void stopBundle(final String bundleName, final String bundleVersion) throws BundleException {
    framework.startBundle(getBundleID(bundleName, bundleVersion));
  }

  public void uninstallBundle(final String bundleName, final String bundleVersion) throws BundleException {
    framework.uninstallBundle(getBundleID(bundleName, bundleVersion));
  }

  private URL getBundleURL(final String bundleName, final String bundleVersion) {
    final String path = MessageFormat.format(BUNDLE_PATH, new String[] { bundleName, bundleVersion });
    try {
      return URLUtil.resolve(bundleRepositories, path);
    } catch (MalformedURLException murle) {
      throw new RuntimeException("Unable to resolve bundle " + path
          + ", please check that your repositories are correctly configured", murle);
    }
  }

  private long getBundleID(final String bundleName, final String bundleVersion) {
    final URL bundleURL = getBundleURL(bundleName, bundleVersion);
    return framework.getBundleId(bundleURL.toString());
  }

}
