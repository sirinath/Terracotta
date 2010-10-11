/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest.modes;

public class NormalTestMode implements TestMode {
  private final NormalTestSetupManager setupManager = new NormalTestSetupManager();

  public Mode getMode() {
    return Mode.NORMAL;
  }

  public TestSetupManager getSetupManager() {
    return this.setupManager;
  }
}
