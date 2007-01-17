/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

public interface BundleManager {

  void start(final String bundleName, final String bundleVersion) throws BundleException;

  void stop(final String bundleName, final String bundleVersion) throws BundleException;

}
