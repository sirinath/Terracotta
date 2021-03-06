/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

/**
 * Tests that distributed wait/notify works intra-VM. Most other tests validate the it work inter-VM
 */
public class SingleVMWaitNotifyTest extends TransparentTestBase implements TestConfigurator {

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(1).setIntensity(1);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return SingleVMWaitNotifyTestApp.class;
  }

  protected boolean canRunCrash() {
    return true;
  }

}
