/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.longrunning;

import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tctest.TestConfigurator;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.terracottatech.config.PersistenceMode;

public class LongrunningGCTester extends TransparentTestBase implements TestConfigurator {

  int NODE_COUNT           = 3;
  int LOOP_ITERATION_COUNT = 1;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(LOOP_ITERATION_COUNT);
    t.initializeTestRunner();
  }

  protected void setupConfig(TestConfigurationSetupManagerFactory configFactory) {
    configFactory.setGCEnabled(true);
    configFactory.setGCVerbose(true);
    configFactory.setPersistenceMode(PersistenceMode.TEMPORARY_SWAP_ONLY);
  }

  protected Class getApplicationClass() {
    return LongrunningGCTestApp.class;
  }

}
