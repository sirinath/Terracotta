/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest.modes;

public class CrashTestMode implements TestMode {
  private TestSetupManager setupManager = new CrashTestSetupManager();

  public Mode getMode() {
    return Mode.CRASH;
  }

  public TestSetupManager getSetupManager() {
    return setupManager;
  }
}
