package com.tctest;

import com.tc.util.runtime.Vm;

/*
 * Unit test for measuring the overhead of the instrumented Field class. For correctness
 * tests for instrumented Field class, refer to the ReflectionFieldTest.
 */
public class IBatisSimpleTest extends TransparentTestBase {
  private final static int NODE_COUNT = 2;
  private final static int LOOP_COUNT = 1;
  
  public IBatisSimpleTest() {
    if (Vm.isJDK16()) {
      disableAllUntil("2010-01-01");
    }
  }

  public void setUp() throws Exception {
    super.setUp();
    
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(LOOP_COUNT);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return IBatisSimpleTestApp.class;
  }
}
