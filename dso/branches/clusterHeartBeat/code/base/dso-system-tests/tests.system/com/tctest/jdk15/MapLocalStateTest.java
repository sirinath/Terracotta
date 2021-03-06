/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.jdk15;

import com.tctest.MapLocalStateTestApp;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

public class MapLocalStateTest extends TransparentTestBase {

  public static final int NODE_COUNT = 1;

  public MapLocalStateTest() {
    //
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return MapLocalStateTestApp.class;
  }

}
