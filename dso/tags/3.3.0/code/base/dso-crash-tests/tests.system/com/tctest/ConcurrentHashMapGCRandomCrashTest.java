/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersPersistenceMode;
import com.tc.test.MultipleServersSharedDataMode;
import com.tc.test.activepassive.ActivePassiveServerManager;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;

public class ConcurrentHashMapGCRandomCrashTest extends GCAndActivePassiveTestBase {

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setAttribute(ConcurrentHashMapSwappingTestApp.GC_TEST_KEY, "true");
    super.doSetUp(t);
  }

  protected Class getApplicationClass() {
    return ConcurrentHashMapSwappingTestApp.class;
  }

  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(3);
    setupManager.setServerCrashMode(MultipleServersCrashMode.RANDOM_SERVER_CRASH);
    setupManager.setServerCrashWaitTimeInSec(30);
    setupManager.setServerShareDataMode(MultipleServersSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(MultipleServersPersistenceMode.TEMPORARY_SWAP_ONLY);
  }

  @Override
  protected void customizeActivePassiveTest(ActivePassiveServerManager manager) throws Exception {
    super.customizeActivePassiveTest(manager);
    if (isMultipleServerTest()) {
      addPostAction(new VerifyDGCPostAction(manager.connectAllDsoMBeans()));
    }
  }
}
