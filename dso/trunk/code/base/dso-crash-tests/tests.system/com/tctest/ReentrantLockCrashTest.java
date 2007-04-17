/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.test.activepassive.ActivePassiveTestSetupManager;

public class ReentrantLockCrashTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return ReentrantLockTestApp.class;
  }
  
  protected boolean canRunCrash() {
    return true;
  }
  
  protected boolean canRunActivePassive() {
    return true;
  }

  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(ActivePassiveTestSetupManager.CONTINUOUS_ACTIVE_CRASH);
    setupManager.setServerCrashWaitInSec(30);
    setupManager.setServerShareDataMode(ActivePassiveTestSetupManager.DISK);
    setupManager.setServerPersistenceMode(ActivePassiveTestSetupManager.PERMANENT_STORE);
  }
  
}
