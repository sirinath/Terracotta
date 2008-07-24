/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.test.activepassive.ActivePassiveCrashMode;
import com.tc.test.activepassive.ActivePassivePersistenceMode;
import com.tc.test.activepassive.ActivePassiveSharedDataMode;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;

public class LinkedBlockingQueueActiveActiveTest extends TransparentTestBase {

  private static final int NODE_COUNT   = 1;
  private final int        electionTime = 5;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return LinkedBlockingQueueCrashTestApp.class;
  }

  protected boolean canRunActiveActive() {
    return true;
  }

  public void setupActiveActiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(ActivePassiveCrashMode.NO_CRASH);
    setupManager.setServerShareDataMode(ActivePassiveSharedDataMode.DISK);
    setupManager.setServerPersistenceMode(ActivePassivePersistenceMode.PERMANENT_STORE);
    setupManager.addActiveServerGroup(1, ActivePassiveSharedDataMode.DISK, electionTime);
    setupManager.addActiveServerGroup(1, ActivePassiveSharedDataMode.DISK, electionTime);
  }

}
