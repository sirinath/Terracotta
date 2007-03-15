/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tctest.runner.TransparentAppConfig;

public class MutateValidateArrayTest extends TransparentTestBase {

  public static final int MUTATOR_NODE_COUNT   = 2;
  public static final int VALIDATOR_NODE_COUNT = 1;

  public void doSetUp(TransparentTestIface t) throws Exception {
    TransparentAppConfig tac = t.getTransparentAppConfig();
    tac.setClientCount(MUTATOR_NODE_COUNT).setIntensity(1).setValidatorCount(VALIDATOR_NODE_COUNT).setApplicationInstancePerClientCount(2);
    t.initializeTestRunner(true);
  }

  protected Class getApplicationClass() {
    return MutateValidateArrayTestApp.class;
  }

}
