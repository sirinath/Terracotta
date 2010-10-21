/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest.modes;

public class CrashTestSetupManager extends TestSetupManager {
  private int crashTimeInSeconds = 30;

  public int getCrashTimeInSeconds() {
    return crashTimeInSeconds;
  }

  public void setCrashTimeInSeconds(int crashTimeInSeconds) {
    this.crashTimeInSeconds = crashTimeInSeconds;
  }

  @Override
  public String toString() {
    return "CrashTestSetupManager [crashTimeInSeconds=" + crashTimeInSeconds + "]";
  }
}
