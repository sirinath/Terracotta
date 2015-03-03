/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.test.restart.RestartTestHelper;

public class TransparentVectorTest extends TransparentTestBase {
  private static final int NODE_COUNT           = 3;
  private static final int EXECUTION_COUNT      = 3;
  private static final int LOOP_ITERATION_COUNT = 2;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(EXECUTION_COUNT)
        .setIntensity(LOOP_ITERATION_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return TransparentVectorTestApp.class;
  }

  @Override
  protected boolean canRunCrash() {
    return true;
  }

  @Override
  protected void setupConfig(TestConfigurationSetupManagerFactory configFactory) {
    super.setupConfig(configFactory);
    configFactory.l2DSOConfig().getDso().setClientReconnectWindow(30);
  }

  @Override
  protected void customizeRestartTestHelper(RestartTestHelper helper) {
    super.customizeRestartTestHelper(helper);
    helper.getServerCrasherConfig().setRestartInterval(45 * 1000);
  }
}
