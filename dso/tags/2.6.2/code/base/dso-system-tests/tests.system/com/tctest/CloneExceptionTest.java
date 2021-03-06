/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

public class CloneExceptionTest extends TransparentTestBase {

  private static final int NODE_COUNT = 1;

  public CloneExceptionTest() {
    // DEV-865
    disableAllUntil("2008-11-10");
  }

  public void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return CloneExceptionTestApp.class;
  }
}
