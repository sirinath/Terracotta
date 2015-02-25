/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.util.runtime.Vm;
import com.tctest.ReentrantReadWriteLockTestApp;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

public class ReentrantReadWriteLockTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;

  public ReentrantReadWriteLockTest() {
    if (Vm.isIBM()) {
      // these currently don't have to work on the IBM JDK
      disableTest();
    }
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return ReentrantReadWriteLockTestApp.class;
  }

}
