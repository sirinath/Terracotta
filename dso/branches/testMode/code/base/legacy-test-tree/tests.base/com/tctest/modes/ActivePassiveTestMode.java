/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest.modes;

import com.tc.test.activepassive.ActivePassiveTestSetupManager;

public class ActivePassiveTestMode implements TestMode {
  private final ActivePassiveTestSetupManager setupManager = new ActivePassiveTestSetupManager();

  public Mode getMode() {
    return Mode.ACTIVE_PASSIVE;
  }

  public TestSetupManager getSetupManager() {
    return setupManager;
  }
}
