/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

public class SynchronizedTest extends TransparentTestBase
{
  private static final int NODE_COUNT = 1;

  public void doSetUp(TransparentTestIface t) throws Exception 
  {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }
  
  protected Class getApplicationClass()
  {
    return SynchronizedTestApp.class;
  }
}
