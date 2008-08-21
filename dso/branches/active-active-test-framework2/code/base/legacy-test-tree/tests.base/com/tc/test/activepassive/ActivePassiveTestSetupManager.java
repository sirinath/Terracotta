/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.activepassive;

import com.tc.test.MultipleServersTestSetupManager;

public class ActivePassiveTestSetupManager extends MultipleServersTestSetupManager {

  public void setServerCrashMode(String mode) {
    this.crashMode = new ActivePassiveCrashMode(mode);
  }
}
