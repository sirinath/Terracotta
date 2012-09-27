/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.licenseserver;

import com.tc.l2.api.ReplicatedClusterStateManager;

public class LicenseUsageStateChangeListenerImpl implements LicenseUsageStateChangeListener {
  private final ReplicatedClusterStateManager replicatedClusterStateManager;

  public LicenseUsageStateChangeListenerImpl(ReplicatedClusterStateManager replicatedClusterStateManager) {
    this.replicatedClusterStateManager = replicatedClusterStateManager;
  }

  @Override
  public void licenseStateChanged(LicenseUsageState state) {
    replicatedClusterStateManager.publishLicenseUsageState(state);
  }
}
