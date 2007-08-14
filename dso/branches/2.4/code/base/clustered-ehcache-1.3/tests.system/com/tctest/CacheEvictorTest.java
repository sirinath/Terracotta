package com.tctest;

public class CacheEvictorTest extends TransparentTestBase {
  public static final int NODE_COUNT = 2;

  public CacheEvictorTest() {
    //
  }

  public void doSetUp(final TransparentTestIface tt) throws Exception {
    tt.getTransparentAppConfig().setClientCount(NODE_COUNT);
    tt.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return CacheEvictorTestApp.class;
  }
}
