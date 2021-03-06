/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

public class LinkedBlockingQueueL1ReconnectCrashTest extends TransparentTestBase {

  private static final int NODE_COUNT = 8;
  
  public LinkedBlockingQueueL1ReconnectCrashTest() {
    //disableAllUntil("2007-06-30");
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return LinkedBlockingQueueCrashTestApp.class;
  }

  protected boolean canRunCrash() {
    return true;
  }
  
  protected boolean enableL1Reconnec() {
    return true;
  }

 
}
