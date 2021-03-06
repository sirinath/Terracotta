/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

public class JavaUtilArraysTest extends TransparentTestBase implements TestConfigurator {

  private static final int NODE_COUNT = 2;

  public JavaUtilArraysTest() {
    //
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return JavaUtilArraysTestApp.class;
  }

  protected boolean canRunCrash() {
    return true;
  }

}
