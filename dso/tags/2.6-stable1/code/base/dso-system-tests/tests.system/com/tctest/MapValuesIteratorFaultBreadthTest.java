/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

public class MapValuesIteratorFaultBreadthTest extends TransparentTestBase implements TestConfigurator {

  private static final int NODE_COUNT    = 3;
  private static final int THREADS_COUNT = 1;

  protected Class getApplicationClass() {
    return MapValuesIteratorFaultBreadthTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(THREADS_COUNT);
    t.initializeTestRunner();
  }

}
