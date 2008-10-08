/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersPersistenceMode;
import com.tc.test.MultipleServersSharedDataMode;
import com.tc.test.activeactive.ActiveActiveTestSetupManager;

public class MapOfMapsActiveActiveCrashTest extends ActiveActiveTransparentTestBase implements TestConfigurator {

  private static final int NODE_COUNT    = 2;
  private static final int THREADS_COUNT = 2;
  private final int electionTime = 5;

  public MapOfMapsActiveActiveCrashTest() {
    // disableAllUntil("2019-10-01");
  }

  protected Class<MapOfMapsTestApp> getApplicationClass() {
    return MapOfMapsTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(THREADS_COUNT);
    t.initializeTestRunner();
  }

  public void setupActiveActiveTest(ActiveActiveTestSetupManager setupManager) {
    setupManager.setServerCount(4);
    setupManager.setServerCrashWaitTimeInSec(50);
    setupManager.setServerCrashMode(MultipleServersCrashMode.RANDOM_SERVER_CRASH);
    setupManager.setServerShareDataMode(MultipleServersSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(MultipleServersPersistenceMode.PERMANENT_STORE);
    setupManager.addActiveServerGroup(2, MultipleServersSharedDataMode.NETWORK, electionTime);
    setupManager.addActiveServerGroup(2, MultipleServersSharedDataMode.NETWORK, electionTime);
  }
}
