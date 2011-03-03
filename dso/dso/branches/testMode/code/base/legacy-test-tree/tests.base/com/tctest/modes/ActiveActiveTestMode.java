/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest.modes;

import com.tc.test.activeactive.ActiveActiveTestSetupManager;

public class ActiveActiveTestMode implements TestMode {
  private final ActiveActiveTestSetupManager setupManager = new ActiveActiveTestSetupManager();

  public Mode getMode() {
    return Mode.ACTIVE_ACTIVE;
  }

  public TestSetupManager getSetupManager() {
    return setupManager;
  }

}
