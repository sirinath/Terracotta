/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest;

public class SimplePrimitiveArrayTest extends TransparentTestBase implements TestConfigurator {

  private static final int NODE_COUNT = 8;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return SimplePrimitiveArrayTestApp.class;
  }

}
