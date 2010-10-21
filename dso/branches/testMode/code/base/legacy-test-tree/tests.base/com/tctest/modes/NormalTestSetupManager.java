/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest.modes;

import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.terracottatech.config.PersistenceMode;

public class NormalTestSetupManager extends TestSetupManager {
  private boolean isPersistent = false;

  public void setPersistent(boolean isPersistent) {
    this.isPersistent = isPersistent;
  }

  public void setInConfig(TestTVSConfigurationSetupManagerFactory factory) {
    if (isPersistent) {
      factory.setPersistenceMode(PersistenceMode.PERMANENT_STORE);
    } else {
      factory.setPersistenceMode(PersistenceMode.TEMPORARY_SWAP_ONLY);
    }
  }

  @Override
  public String toString() {
    return "NormalTestSetupManager [isPersistent=" + isPersistent + "]";
  }
}
