/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.terracottatech.config.PersistenceMode;

public class TransparentLinkedListTest extends TransparentTestBase implements TestConfigurator {
  private static final int NODE_COUNT           = 2;
  private static final int EXECUTION_COUNT      = 2;
  private static final int LOOP_ITERATION_COUNT = 3;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(EXECUTION_COUNT)
        .setIntensity(LOOP_ITERATION_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected void setupConfig(TestConfigurationSetupManagerFactory configFactory) {
    configFactory.setGCEnabled(true);
    configFactory.setPersistenceMode(PersistenceMode.TEMPORARY_SWAP_ONLY);
  }

  @Override
  protected Class getApplicationClass() {
    return TransparentLinkedListTestApp.class;
  }

  @Override
  protected boolean canRunCrash() {
    return true;
  }

}
