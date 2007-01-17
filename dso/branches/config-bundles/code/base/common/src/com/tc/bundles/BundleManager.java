/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

public interface BundleManager {

  void install(final Bundle bundle) throws BundleException;

  void start(final Bundle bundle) throws BundleException;

  void stop(final Bundle bundle) throws BundleException;

  void uninstall(final Bundle bundle) throws BundleException;

}
