/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest.modes;

public class NormalTestSetupManager implements TestSetupManager {
  private boolean isPersistent = false;

  public void setPersistent(boolean isPersistent) {
    this.isPersistent = isPersistent;
  }

  public boolean isPersistent() {
    return this.isPersistent;
  }

  @Override
  public String toString() {
    return "NormalTestSetupManager [isPersistent=" + isPersistent + "]";
  }
}
