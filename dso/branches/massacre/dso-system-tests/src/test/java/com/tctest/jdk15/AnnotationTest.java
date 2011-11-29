/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

public class AnnotationTest extends TransparentTestBase {

  public static final int NODE_COUNT = 3;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return AnnotationTestApp.class;
  }

  @Override
  protected boolean canRunCrash() {
    return false;
  }

}
