/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;

public class DistributedWaitCrashTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;
  
  public DistributedWaitCrashTest() {
    //this.disableAllUntil("2007-05-01");
  }
  
  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return DistributedWaitCrashTestApp.class;
  }
  
  @Override
  protected void setupConfig(TestConfigurationSetupManagerFactory configFactory) {
    super.setupConfig(configFactory);
    configFactory.l2DSOConfig().getDso().setClientReconnectWindow(20);
  }

  @Override
  protected boolean canRunCrash() {
    return true;
  }

}
