/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;


import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.activepassive.ActivePassiveCrashMode;
import com.tc.test.activepassive.ActivePassivePersistenceMode;
import com.tc.test.activepassive.ActivePassiveSharedDataMode;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tc.util.runtime.Os;

import java.util.ArrayList;
import java.util.Date;

public class CreateLotsOfGarbageYoungGenGCTest extends GCTestBase implements TestConfigurator {

  public CreateLotsOfGarbageYoungGenGCTest() {
    if (Os.isSolaris()) {
      disableAllUntil(new Date(Long.MAX_VALUE));
    }
  }

  protected Class getApplicationClass() {
    return CreateLotsOfGarbageGCTestApp.class;
  }

  protected boolean canRunActivePassive() {
    return true;
  }

  // Run Full Gen every 60 secs
  public int getGarbageCollectionInterval() {
    return 60;
  }
  
  // Run Young Gen every 10 seconds
  protected void  setExtraJvmArgs(final ArrayList jvmArgs) {
    TCProperties tcProps = TCPropertiesImpl.getProperties();
    tcProps.setProperty(TCPropertiesConsts.L2_OBJECTMANAGER_DGC_YOUNG_ENABLED, "true");
    tcProps.setProperty(TCPropertiesConsts.L2_OBJECTMANAGER_DGC_YOUNG_FREQUENCY, "10000");
    System.setProperty("com.tc." + TCPropertiesConsts.L2_OBJECTMANAGER_DGC_YOUNG_ENABLED, "true");
    System.setProperty("com.tc." + TCPropertiesConsts.L2_OBJECTMANAGER_DGC_YOUNG_FREQUENCY, "10000");

    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_OBJECTMANAGER_DGC_YOUNG_ENABLED + "=true");
    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_OBJECTMANAGER_DGC_YOUNG_FREQUENCY + "=10000");
  }

  // start only 1 L1
  protected int getNodeCount() {
    return 1;
  }

  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(ActivePassiveCrashMode.CONTINUOUS_ACTIVE_CRASH);
    setupManager.setServerCrashWaitTimeInSec(60);
    setupManager.setServerShareDataMode(ActivePassiveSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(ActivePassivePersistenceMode.TEMPORARY_SWAP_ONLY);
  }

}
